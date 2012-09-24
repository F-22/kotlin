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

package org.jetbrains.jet.lang.resolve.java;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassOrNamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author abreslav
 * @author Stepan Koltsov
 * @author alex.tkachman
*/
abstract class ResolverScopeData {
    @Nullable
    final PsiClass psiClass;
    @Nullable
    final PsiPackage psiPackage;
    @Nullable
    final FqName fqName;
    final boolean staticMembers;
    final boolean kotlin;
    final ClassOrNamespaceDescriptor classOrNamespaceDescriptor;

    ResolverScopeData(
            @Nullable PsiClass psiClass,
            @Nullable PsiPackage psiPackage,
            @Nullable FqName fqName,
            boolean staticMembers,
            @NotNull ClassOrNamespaceDescriptor descriptor
    ) {
        JavaDescriptorResolver.checkPsiClassIsNotJet(psiClass);

        this.psiClass = psiClass;
        this.psiPackage = psiPackage;
        this.fqName = fqName;

        if (psiClass == null && psiPackage == null) {
            throw new IllegalStateException("both psiClass and psiPackage cannot be null");
        }

        this.staticMembers = staticMembers;
        this.kotlin = psiClass != null && JavaDescriptorResolver.isKotlinClass(psiClass);
        classOrNamespaceDescriptor = descriptor;

        if (fqName != null && fqName.lastSegmentIs(Name.identifier(JvmAbi.PACKAGE_CLASS)) && psiClass != null && kotlin) {
            throw new IllegalStateException("Kotlin namespace cannot have last segment " + JvmAbi.PACKAGE_CLASS + ": " + fqName);
        }
    }

    ResolverScopeData(boolean negative) {
        if (!negative) {
            throw new IllegalStateException();
        }
        this.psiClass = null;
        this.psiPackage = null;
        this.fqName = null;
        this.staticMembers = false;
        this.kotlin = false;
        this.classOrNamespaceDescriptor = null;
    }

    @NotNull
    public PsiElement getPsiPackageOrPsiClass() {
        if (psiPackage != null) {
            return psiPackage;
        }
        else {
            assert psiClass != null;
            return psiClass;
        }
    }

    private Map<Name, NamedMembers> namedMembersMap;

    @NotNull
    public abstract List<TypeParameterDescriptor> getTypeParameters();

    @NotNull
    Map<Name, NamedMembers> getNamedMembers() {
        if (namedMembersMap == null) {
            if (psiClass != null) {
                @SuppressWarnings("ConstantConditions")
                NamedMemberCollector
                        builder = new NamedMemberCollector(new PsiClassWrapper(psiClass), staticMembers, kotlin);
                builder.run();
                namedMembersMap =
                        builder.namedMembersMap.isEmpty() ? Collections.<Name, NamedMembers>emptyMap() : builder.namedMembersMap;
            }
            else {
                namedMembersMap = Collections.emptyMap();
            }
        }
        return namedMembersMap;
    }
}

/** Class with instance members */
class ResolverBinaryClassData extends ResolverClassData {

    ResolverBinaryClassData(@NotNull PsiClass psiClass, @Nullable FqName fqName, @NotNull ClassDescriptorFromJvmBytecode classDescriptor) {
        super(psiClass, null, fqName, false, classDescriptor);
    }

    ResolverBinaryClassData(boolean negative) {
        super(negative);
    }

    static final ResolverClassData NEGATIVE = new ResolverBinaryClassData(true);

}

class ResolverClassData extends ResolverScopeData {

    final ClassDescriptorFromJvmBytecode classDescriptor;

    List<JavaDescriptorSignatureResolver.TypeParameterDescriptorInitialization> typeParameters;

    protected ResolverClassData(boolean negative) {
        super(negative);
        this.classDescriptor = null;
    }


    protected ResolverClassData(
            @Nullable PsiClass psiClass,
            @Nullable PsiPackage psiPackage,
            @Nullable FqName fqName,
            boolean staticMembers,
            @NotNull ClassDescriptorFromJvmBytecode descriptor
    ) {
        super(psiClass, psiPackage, fqName, staticMembers, descriptor);
        classDescriptor = descriptor;
    }

    @NotNull
    public ClassDescriptor getClassDescriptor() {
        return classDescriptor;
    }

    @NotNull
    @Override
    public List<TypeParameterDescriptor> getTypeParameters() {
        return getClassDescriptor().getTypeConstructor().getParameters();
    }

}


class ResolverSyntheticClassObjectClassData extends ResolverClassData {

    protected ResolverSyntheticClassObjectClassData(
            @Nullable PsiClass psiClass,
            @Nullable FqName fqName,
            @NotNull ClassDescriptorFromJvmBytecode descriptor
    ) {
        super(psiClass, null, fqName, true, descriptor);
    }
}

/** Either package or class with static members */
class ResolverNamespaceData extends ResolverScopeData {
    final NamespaceDescriptor namespaceDescriptor;

    ResolverNamespaceData(@Nullable PsiClass psiClass, @Nullable PsiPackage psiPackage, @NotNull FqName fqName, @NotNull NamespaceDescriptor namespaceDescriptor) {
        super(psiClass, psiPackage, fqName, true, namespaceDescriptor);
        this.namespaceDescriptor = namespaceDescriptor;
    }

    private ResolverNamespaceData(boolean negative) {
        super(negative);
        this.namespaceDescriptor = null;
    }

    static final ResolverNamespaceData NEGATIVE = new ResolverNamespaceData(true);

    JavaPackageScope memberScope;

    @NotNull
    @Override
    public List<TypeParameterDescriptor> getTypeParameters() {
        return new ArrayList<TypeParameterDescriptor>(0);
    }
}
