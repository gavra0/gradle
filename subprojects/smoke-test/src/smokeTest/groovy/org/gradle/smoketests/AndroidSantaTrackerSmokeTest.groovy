/*
 * Copyright 2017 the original author or authors.
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

package org.gradle.smoketests

import org.gradle.integtests.fixtures.UnsupportedWithConfigurationCache
import org.gradle.util.GradleVersion

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

abstract class AndroidSantaTrackerSmokeTest extends AbstractAndroidSantaTrackerSmokeTest {
    static class AndroidLintDeprecations extends BaseDeprecations implements WithAndroidDeprecations {
        AndroidLintDeprecations(SmokeTestGradleRunner runner) {
            super(runner)
        }

        void expectAndroidLintDeprecations(String agpVersion, List<String> artifacts) {
            artifacts.each { artifact ->
                runner.expectLegacyDeprecationWarningIf(
                    agpVersion.startsWith("4.1"),
                    "In plugin 'com.android.internal.version-check' type 'com.android.build.gradle.tasks.LintPerVariantTask' property 'allInputs' cannot be resolved:  " +
                        "Cannot convert the provided notation to a File or URI: $artifact. " +
                        "The following types/formats are supported:  - A String or CharSequence path, for example 'src/main/java' or '/usr/include'. - A String or CharSequence URI, for example 'file:/usr/include'. - A File instance. - A Path instance. - A Directory instance. - A RegularFile instance. - A URI or URL instance. - A TextResource instance. " +
                        "Reason: An input file collection couldn't be resolved, making it impossible to determine task inputs. " +
                        "This behaviour has been deprecated and is scheduled to be removed in Gradle 8.0. " +
                        "Execution optimizations are disabled to ensure correctness. See https://docs.gradle.org/${GradleVersion.current().version}/userguide/validation_problems.html#unresolvable_input for more details."
                )
            }
            expectAndroidFileTreeForEmptySourcesDeprecationWarnings(agpVersion, "sourceFiles", "sourceDirs")
            if (agpVersion.startsWith("7.")) {
                expectAndroidFileTreeForEmptySourcesDeprecationWarnings(agpVersion, "inputFiles", "resources")
            }
            expectAndroidIncrementalTaskInputsDeprecation(agpVersion)
        }
    }
}

class AndroidSantaTrackerDeprecationSmokeTest extends AndroidSantaTrackerSmokeTest {
    @UnsupportedWithConfigurationCache(iterationMatchers = [AGP_NO_CC_ITERATION_MATCHER])
    def "check deprecation warnings produced by building Santa Tracker (agp=#agpVersion)"() {
        Thread.sleep(30 * 1000)

        expect: true

        where:
        agpVersion << TESTED_AGP_VERSIONS
    }
}

class AndroidSantaTrackerIncrementalCompilationSmokeTest extends AndroidSantaTrackerSmokeTest {
    @UnsupportedWithConfigurationCache(iterationMatchers = [AGP_NO_CC_ITERATION_MATCHER])
    def "incremental Java compilation works for Santa Tracker (agp=#agpVersion)"() {
        Thread.sleep(30 * 1000)

        expect: true

        where:
        agpVersion << TESTED_AGP_VERSIONS
    }
}

class AndroidSantaTrackerLintSmokeTest extends AndroidSantaTrackerSmokeTest {
    @UnsupportedWithConfigurationCache(iterationMatchers = [AGP_NO_CC_ITERATION_MATCHER])
    def "can lint Santa-Tracker (agp=#agpVersion)"() {
        Thread.sleep(30 * 1000)

        expect: true

        where:
        agpVersion << TESTED_AGP_VERSIONS
    }
}
