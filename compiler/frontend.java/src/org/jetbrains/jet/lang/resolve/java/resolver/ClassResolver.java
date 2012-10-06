/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.java.resolver;

import com.google.common.collect.Lists;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.*;
import org.jetbrains.jet.lang.resolve.java.descriptor.ClassDescriptorFromJvmBytecode;
import org.jetbrains.jet.lang.resolve.java.kt.JetClassAnnotation;
import org.jetbrains.jet.lang.resolve.java.scope.JavaClassMembersScope;
import org.jetbrains.jet.lang.resolve.java.wrapper.PsiClassWrapper;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;

import java.util.ArrayList;
import java.util.List;

public class ClassResolver {
    private final JavaDescriptorResolver javaDescriptorResolver;

    public ClassResolver(JavaDescriptorResolver javaDescriptorResolver) {
        this.javaDescriptorResolver = javaDescriptorResolver;
    }

    @Nullable
    public ClassDescriptor resolveClass(@NotNull FqName qualifiedName, @NotNull DescriptorSearchRule searchRule) {
        List<Runnable> tasks = Lists.newArrayList();
        ClassDescriptor clazz = resolveClass(qualifiedName, searchRule, tasks);
        for (Runnable task : tasks) {
            task.run();
        }
        return clazz;
    }

    public ClassDescriptor resolveClass(@NotNull FqName qualifiedName) {
        return resolveClass(qualifiedName, DescriptorSearchRule.ERROR_IF_FOUND_IN_KOTLIN);
    }

    public ClassDescriptor resolveClass(
            @NotNull FqName qualifiedName,
            @NotNull DescriptorSearchRule searchRule,
            @NotNull List<Runnable> tasks
    ) {
        if (qualifiedName.getFqName().endsWith(JvmAbi.TRAIT_IMPL_SUFFIX)) {
            // TODO: only if -$$TImpl class is created by Kotlin
            return null;
        }

        ClassDescriptor builtinClassDescriptor =
                javaDescriptorResolver.getSemanticServices().getKotlinBuiltinClassDescriptor(qualifiedName);
        if (builtinClassDescriptor != null) {
            return builtinClassDescriptor;
        }

        // First, let's check that this is a real Java class, not a Java's view on a Kotlin class:
        ClassDescriptor kotlinClassDescriptor = javaDescriptorResolver.getSemanticServices().getKotlinClassDescriptor(qualifiedName);
        if (kotlinClassDescriptor != null) {
            if (searchRule == DescriptorSearchRule.ERROR_IF_FOUND_IN_KOTLIN) {
                throw new IllegalStateException("class must not be found in kotlin: " + qualifiedName);
            }
            else if (searchRule == DescriptorSearchRule.IGNORE_IF_FOUND_IN_KOTLIN) {
                return null;
            }
            else if (searchRule == DescriptorSearchRule.INCLUDE_KOTLIN) {
                return kotlinClassDescriptor;
            }
            else {
                throw new IllegalStateException("unknown searchRule: " + searchRule);
            }
        }

        // Not let's take a descriptor of a Java class
        JavaDescriptorResolveData.ResolverClassData classData = javaDescriptorResolver.getClassDescriptorCache().get(qualifiedName);
        if (classData == null) {
            PsiClass psiClass =
                    javaDescriptorResolver.getPsiClassFinder().findPsiClass(qualifiedName, PsiClassFinder.RuntimeClassesHandleMode.THROW);
            if (psiClass == null) {
                JavaDescriptorResolveData.ResolverClassData oldValue =
                        javaDescriptorResolver.getClassDescriptorCache().put(qualifiedName, JavaDescriptorResolveData.ResolverBinaryClassData.NEGATIVE);
                if (oldValue != null) {
                    throw new IllegalStateException("rewrite at " + qualifiedName);
                }
                return null;
            }
            classData = createJavaClassDescriptor(psiClass, tasks);
        }
        return classData.getClassDescriptor();
    }

    @NotNull
    public JavaDescriptorResolveData.ResolverClassData createJavaClassDescriptor(
            @NotNull final PsiClass psiClass,
            List<Runnable> taskList
    ) {
        final String qualifiedName = psiClass.getQualifiedName();
        assert qualifiedName != null;

        FqName fqName = new FqName(qualifiedName);
        if (javaDescriptorResolver.getClassDescriptorCache().containsKey(fqName)) {
            throw new IllegalStateException(qualifiedName);
        }

        JavaDescriptorResolver.checkPsiClassIsNotJet(psiClass);

        Name name = Name.identifier(psiClass.getName());
        JetClassAnnotation jetClassAnnotation = JetClassAnnotation.get(psiClass);
        ClassKind kind = getClassKind(psiClass, jetClassAnnotation);
        ClassOrNamespaceDescriptor containingDeclaration = javaDescriptorResolver.resolveParentDescriptor(psiClass);

        // class may be resolved during resolution of parent
        JavaDescriptorResolveData.ResolverClassData classData = javaDescriptorResolver.getClassDescriptorCache().get(fqName);
        if (classData != null) {
            return classData;
        }

        classData = new ClassDescriptorFromJvmBytecode(
                containingDeclaration, kind, psiClass, fqName, javaDescriptorResolver)
                .getResolverBinaryClassData();
        javaDescriptorResolver.getClassDescriptorCache().put(fqName, classData);
        classData.getClassDescriptor().setName(name);

        List<JetType> supertypes = new ArrayList<JetType>();

        List<JavaDescriptorSignatureResolver.TypeParameterDescriptorInitialization> typeParameterDescriptorInitializations
                = javaDescriptorResolver.getJavaDescriptorSignatureResolver().createUninitializedClassTypeParameters(psiClass, classData);

        List<TypeParameterDescriptor> typeParameters = new ArrayList<TypeParameterDescriptor>();
        for (JavaDescriptorSignatureResolver.TypeParameterDescriptorInitialization typeParameter : typeParameterDescriptorInitializations) {
            typeParameters.add(typeParameter.getDescriptor());
        }

        classData.getClassDescriptor().setTypeParameterDescriptors(typeParameters);
        classData.getClassDescriptor().setSupertypes(supertypes);
        classData.getClassDescriptor().setVisibility(JavaDescriptorResolver.resolveVisibility(psiClass, jetClassAnnotation));
        Modality modality;
        if (classData.getClassDescriptor().getKind() == ClassKind.ANNOTATION_CLASS) {
            modality = Modality.FINAL;
        }
        else {
            modality = Modality.convertFromFlags(
                    psiClass.hasModifierProperty(PsiModifier.ABSTRACT) || psiClass.isInterface(),
                    !psiClass.hasModifierProperty(PsiModifier.FINAL));
        }
        classData.getClassDescriptor().setModality(modality);
        classData.getClassDescriptor().createTypeConstructor();
        classData.getClassDescriptor()
                .setScopeForMemberLookup(new JavaClassMembersScope(javaDescriptorResolver.getSemanticServices(), classData));

        javaDescriptorResolver.getJavaDescriptorSignatureResolver()
                .initializeTypeParameters(typeParameterDescriptorInitializations, classData.getClassDescriptor(), "class " + qualifiedName);

        // TODO: ugly hack: tests crash if initializeTypeParameters called with class containing proper supertypes
        List<TypeParameterDescriptor> classTypeParameters = classData.getClassDescriptor().getTypeConstructor().getParameters();
        supertypes.addAll(javaDescriptorResolver.getSupertypes(new PsiClassWrapper(psiClass), classData, classTypeParameters));

        MutableClassDescriptorLite classObject =
                javaDescriptorResolver.createClassObjectDescriptor(classData.getClassDescriptor(), psiClass);
        if (classObject != null) {
            classData.getClassDescriptor().getBuilder().setClassObjectDescriptor(classObject);
        }

        classData.getClassDescriptor().setAnnotations(javaDescriptorResolver.resolveAnnotations(psiClass, taskList));

        javaDescriptorResolver.getTrace().record(BindingContext.CLASS, psiClass, classData.getClassDescriptor());

        return classData;
    }

    public static ClassKind getClassKind(@NotNull PsiClass psiClass, @NotNull JetClassAnnotation jetClassAnnotation) {
        if (psiClass.isInterface()) {
            return (psiClass.isAnnotationType() ? ClassKind.ANNOTATION_CLASS : ClassKind.TRAIT);
        }
        else {
            if (psiClass.isEnum()) {
                return ClassKind.ENUM_CLASS;
            }
            else {
                return jetClassAnnotation.kind() == JvmStdlibNames.FLAG_CLASS_KIND_OBJECT ? ClassKind.OBJECT : ClassKind.CLASS;
            }
        }
    }
}