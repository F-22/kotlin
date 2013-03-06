/*
 * Copyright 2010-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.lang.resolve.java;

import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.types.DependencyClassByQualifiedNameResolver;

public class JavaDependencyByQualifiedNameResolver implements DependencyClassByQualifiedNameResolver {

    private final PsiClassFinder psiClassFinder;
    private final JavaClassClassResolutionFacade classResolutionFacade;

    public JavaDependencyByQualifiedNameResolver(
            @NotNull PsiClassFinder psiClassFinder,
            @NotNull JavaClassClassResolutionFacade classResolutionFacade
    ) {
        this.psiClassFinder = psiClassFinder;
        this.classResolutionFacade = classResolutionFacade;
    }

    @Nullable
    @Override
    public ClassDescriptor resolveClass(@NotNull FqName fqName) {
        PsiClass psiClass = psiClassFinder.findPsiClass(fqName, PsiClassFinder.RuntimeClassesHandleMode.THROW);
        if (psiClass == null) {
            return null;
        }
        return classResolutionFacade.getClassDescriptor(psiClass);
    }
}