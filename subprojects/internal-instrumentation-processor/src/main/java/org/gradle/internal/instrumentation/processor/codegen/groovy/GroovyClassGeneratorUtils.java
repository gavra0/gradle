/*
 * Copyright 2023 the original author or authors.
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

package org.gradle.internal.instrumentation.processor.codegen.groovy;

import org.gradle.internal.instrumentation.model.CallInterceptionRequest;
import org.gradle.internal.instrumentation.model.CallableInfo;
import org.gradle.internal.instrumentation.model.CallableKindInfo;
import org.gradle.internal.instrumentation.model.RequestExtra;
import org.gradle.internal.instrumentation.processor.codegen.groovy.CallInterceptorSpecs.CallInterceptorSpec.ConstructorInterceptorSpec;
import org.gradle.internal.instrumentation.processor.codegen.groovy.CallInterceptorSpecs.CallInterceptorSpec.NamedCallableInterceptorSpec;
import org.gradle.internal.instrumentation.util.NameUtil;
import org.objectweb.asm.Type;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

class GroovyClassGeneratorUtils {

    public static CallInterceptorSpecs groupRequests(Collection<CallInterceptionRequest> interceptionRequests) {
        Map<String, NamedCallableInterceptorSpec> namedRequests = new LinkedHashMap<>();
        Map<Type, ConstructorInterceptorSpec> constructorRequests = new LinkedHashMap<>();
        interceptionRequests.forEach(request -> {
            if (request.getRequestExtras().getByType(RequestExtra.InterceptGroovyCalls.class).isPresent()) {
                CallableInfo callable = request.getInterceptedCallable();
                CallableKindInfo kind = callable.getKind();
                if (kind == CallableKindInfo.AFTER_CONSTRUCTOR) {
                    constructorRequests.computeIfAbsent(request.getInterceptedCallable().getOwner().getType(), ConstructorInterceptorSpec::of).getRequests().add(request);
                } else {
                    String nameKey = NameUtil.interceptedJvmMethodName(callable);
                    namedRequests.computeIfAbsent(nameKey, NamedCallableInterceptorSpec::of).getRequests().add(request);
                }
            }
        });

        return new CallInterceptorSpecs(namedRequests.values(), constructorRequests.values());
    }
}
