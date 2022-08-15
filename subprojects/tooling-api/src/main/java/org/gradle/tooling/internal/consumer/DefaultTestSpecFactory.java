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

package org.gradle.tooling.internal.consumer;

import org.gradle.tooling.TestSpec;
import org.gradle.tooling.TestSpecFactory;

import java.util.ArrayList;
import java.util.List;

public class DefaultTestSpecFactory implements TestSpecFactory {

    private final boolean isTestTask;
    private List<DefaultTestSpec> testSpecs = new ArrayList<DefaultTestSpec>();

    public List<DefaultTestSpec> getTestSpecs() {
        return testSpecs;
    }

    public DefaultTestSpecFactory(boolean isTestTask) {
        this.isTestTask = isTestTask;
    }

    @Override
    public TestSpec forTaskPath(String taskPath) {
        DefaultTestSpec spec = new DefaultTestSpec(taskPath);
        testSpecs.add(spec);
        return spec;
    }
}
