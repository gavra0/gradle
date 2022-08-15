/*
 * Copyright 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gradlebuild.cleanup;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * NOTICE: this class is invoked via java command line, so we must NOT DEPEND ON ANY 3RD-PARTY LIBRARIES except JDK 11.
 *
 * Usage: java build-logic/cleanup/src/main/java/gradlebuild/cleanup/KillLeakingJavaProcesses.java
 *
 * Kill all Java processes matching:
 * 1. Main class is `GradleWorkerMain`/`KotlinCompileDaemon`.
 * 2. Main class is `GradleDaemon` and classpath is inside the current working directory.
 */
public class KillLeakingJavaProcesses {
    private static final Pattern UNIX_JAVA_PROCESS_PATTERN = Pattern.compile("^(\\d+)\\s+(.*/bin/java)\\s+(.*)$");
    private static final Pattern WINDOWS_JAVA_PROCESS_PATTERN = Pattern.compile("^\"?(.*[/\\\\]bin[/\\\\]java(\\.exe)?)\"\\s+(.*)\\s+(\\d+)$");
    private static final Pattern MAIN_CLASS_PATTERN = Pattern.compile("([a-z]+\\.)+[A-Z]\\w+");

    private static final String WORKING_DIRECTORY_PATH = new File("").getAbsolutePath();

    public static void main(String[] args) {
        System.out.println("Working dir is: " + WORKING_DIRECTORY_PATH);
        ps().stream()
            .peek(javaProcessInfo -> System.out.println("Found java process " + javaProcessInfo))
            .filter(JavaProcessInfo::isLeakingJavaProcess)
            .forEach(JavaProcessInfo::kill);

        System.out.println("After cleanup:");
        System.out.println(run(determinePsCommand()));
    }

    private static List<JavaProcessInfo> ps() {
        ExecResult result = run(determinePsCommand()).assertZeroExit();
        return JavaProcessInfo.parsePsOutput(result.stdout, isWindows());
    }

    private static String[] determinePsCommand() {
        if (isWindows()) {
            return new String[]{"wmic", "process", "get", "processid,commandline"};
        } else if (isMacOS()) {
            return new String[]{"ps", "x", "-o", "pid,command"};
        } else {
            return new String[]{"ps", "x", "-o", "pid,cmd"};
        }
    }

    static class JavaProcessInfo {
        /**
         * The pid of the process.
         */
        final String pid;
        /**
         * The command line arguments of the process. The first argument is java executable.
         */
        final List<String> commands = new ArrayList<>();
        /**
         * The main class of the process. May be null.
         */
        String mainClass;
        /**
         * The classpath passed to the process via `-cp` or `-classpath`. May be null.
         */
        String classpath;

        private JavaProcessInfo(String pid, String javaCommand, String otherCommands) {
            this.pid = pid;
            commands.add(javaCommand);
            Stream.of(otherCommands.split("\\s"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(commands::add);
            for (int i = 0; i < commands.size(); ++i) {
                String command = commands.get(i);
                if (MAIN_CLASS_PATTERN.matcher(command).matches()) {
                    mainClass = command;
                }
                if (i != commands.size() - 1 && "-classpath".equals(command) || "-cp".equals(command)) {
                    classpath = commands.get(i + 1);
                    if (classpath.startsWith("\"")) {
                        classpath = classpath.substring(1, classpath.length());
                    }
                    if (classpath.endsWith("\"")) {
                        classpath = classpath.substring(0, classpath.length() - 1);
                    }
                }
            }
        }

        static List<JavaProcessInfo> parsePsOutput(String psOutput, boolean isWindows) {
            return psOutput.lines()
                .map(line -> JavaProcessInfo.parseLine(line, isWindows))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        }

        /**
         * Parse a jps output line and get the Java process info.
         *
         * @return null if the line doesn't match the pattern.
         */
        private static JavaProcessInfo parseLine(String line, boolean isWindows) {
            Pattern pattern = isWindows ? WINDOWS_JAVA_PROCESS_PATTERN : UNIX_JAVA_PROCESS_PATTERN;
            Matcher matcher = pattern.matcher(line.trim());
            if (matcher.find()) {
                if (isWindows) {
                    return new JavaProcessInfo(matcher.group(4), matcher.group(1), matcher.group(3));
                } else {
                    return new JavaProcessInfo(matcher.group(1), matcher.group(2), matcher.group(3));
                }
            } else {
                return null;
            }
        }

        private boolean isLeakingJavaProcess() {
            if (mainClass == null) {
                return false;
            }
            if (mainClass.endsWith(".GradleWorkerMain") || mainClass.endsWith(".KotlinCompileDaemon")) {
                return true;
            } else if (mainClass.endsWith(".GradleDaemon")) {
                return classpath != null && classpath.startsWith(WORKING_DIRECTORY_PATH);
            } else {
                return false;
            }
        }

        private void kill() {
            System.out.println("Killing process " + pid);
            ExecResult execResult = isWindows() ? run("taskkill.exe", "/F", "/T", "/PID", pid) : run("kill", "-9", pid);
            System.out.println(execResult);
            if (execResult.code != 0) {
                System.out.println("Failed to kill " + pid + ". Maybe already killed?");
            }
        }

        @Override
        public String toString() {
            return "JavaProcessInfo{" +
                "pid='" + pid + '\'' +
                ", commands=" + commands +
                '}';
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    private static boolean isMacOS() {
        return System.getProperty("os.name").toLowerCase().contains("mac");
    }

    private static class ExecResult {
        private final String[] args;
        private final int code;
        private final String stdout;
        private final String stderr;

        public ExecResult(String[] args, int code, String stdout, String stderr) {
            this.args = args;
            this.code = code;
            this.stdout = stdout;
            this.stderr = stderr;
        }

        @Override
        public String toString() {
            return "ExecResult{" +
                "code=" + code +
                "\n stdout='" + stdout + '\'' +
                "\n stderr='" + stderr + '\'' +
                '}';
        }

        ExecResult assertZeroExit() {
            if (code != 0) {
                throw new AssertionError(String.format("%s return:\n%s\n%s\n", Arrays.toString(args), stdout, stderr));
            }
            return this;
        }
    }

    private static ExecResult run(String... args) {
        try {
            Process process = new ProcessBuilder().command(args).start();
            CountDownLatch latch = new CountDownLatch(2);
            ByteArrayOutputStream stdout = connectStream(process.getInputStream(), latch);
            ByteArrayOutputStream stderr = connectStream(process.getErrorStream(), latch);

            process.waitFor(1, TimeUnit.MINUTES);
            latch.await(1, TimeUnit.MINUTES);
            return new ExecResult(args, process.exitValue(), stdout.toString(), stderr.toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static ByteArrayOutputStream connectStream(InputStream forkedProcessOutput, CountDownLatch latch) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(os, true);
        new Thread(() -> {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(forkedProcessOutput));
                String line;
                while ((line = reader.readLine()) != null) {
                    ps.println(line);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        }).start();
        return os;
    }
}
