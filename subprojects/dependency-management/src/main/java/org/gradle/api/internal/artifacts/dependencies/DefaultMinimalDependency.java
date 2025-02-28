/*
 * Copyright 2020 the original author or authors.
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
package org.gradle.api.internal.artifacts.dependencies;

import org.gradle.api.artifacts.MinimalExternalModuleDependency;
import org.gradle.api.artifacts.ModuleIdentifier;
import org.gradle.api.artifacts.MutableVersionConstraint;

import java.io.Serializable;

public class DefaultMinimalDependency extends DefaultExternalModuleDependency implements MinimalExternalModuleDependency, Serializable {
    public DefaultMinimalDependency(ModuleIdentifier module, MutableVersionConstraint versionConstraint) {
        super(module, versionConstraint);
    }

    @Override
    public void because(String reason) {
        validateMutation();
    }

    @Override
    protected void validateMutation() {
        throw new UnsupportedOperationException("Minimal dependencies are immutable.");
    }

    @Override
    protected void validateMutation(Object currentValue, Object newValue) {
        validateMutation();
    }

    // copy() intentionally not overridden because we use it to go to a mutable version

    public String toString() {
        String versionConstraintAsString = getVersionConstraint().toString();
        return versionConstraintAsString.isEmpty()
            ? getModule().toString()
            : getModule() + ":" + versionConstraintAsString;
    }
}
