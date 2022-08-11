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

package org.gradle.api.attributes;

import org.gradle.api.Incubating;
import org.gradle.api.Named;

/**
 * Represents the depth at which module dependencies are exposes. Differentiates between
 * dependencies required to compile a module and dependencies required to interface with it.
 *
 * @since 7.6
 */
@Incubating
public interface View extends Named {
    Attribute<View> VIEW_ATTRIBUTE = Attribute.of("org.gradle.view", View.class);

    /**
     * The Java API classpath of a project, not including any implementation or compileOnly dependencies.
     *
     * @since 7.6
     */
    String JAVA_API = "java-api";

    /**
     * The Java compile classpath of a project, including implementation and compileOnly dependencies.
     *
     * @since 7.6
     */
    String JAVA_COMPILE = "java-compile";
}
