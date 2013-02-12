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

package org.jetbrains.jet.plugin.codeInsight.surroundWith;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.File;
import java.util.regex.Pattern;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.test.InnerTestClasses;
import org.jetbrains.jet.test.TestMetadata;

import org.jetbrains.jet.plugin.codeInsight.surroundWith.AbstractSurroundWithTest;

/** This class is generated by {@link org.jetbrains.jet.generators.tests.GenerateTests}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@TestMetadata("idea/testData/codeInsight/surroundWith/if")
public class SurroundWithIfTestGenerated extends AbstractSurroundWithTest {
    public void testAllFilesPresentInIf() throws Exception {
        JetTestUtils.assertAllTestsPresentByMetadata(this.getClass(), "org.jetbrains.jet.generators.tests.GenerateTests", new File("idea/testData/codeInsight/surroundWith/if"), Pattern.compile("^(.+)\\.kt$"), true);
    }
    
    @TestMetadata("block.kt")
    public void testBlock() throws Exception {
        doTestWithIfSurrounder("idea/testData/codeInsight/surroundWith/if/block.kt");
    }
    
    @TestMetadata("severalStatements.kt")
    public void testSeveralStatements() throws Exception {
        doTestWithIfSurrounder("idea/testData/codeInsight/surroundWith/if/severalStatements.kt");
    }
    
    @TestMetadata("singleStatement.kt")
    public void testSingleStatement() throws Exception {
        doTestWithIfSurrounder("idea/testData/codeInsight/surroundWith/if/singleStatement.kt");
    }
    
    @TestMetadata("singleStatementAtCaret.kt")
    public void testSingleStatementAtCaret() throws Exception {
        doTestWithIfSurrounder("idea/testData/codeInsight/surroundWith/if/singleStatementAtCaret.kt");
    }
    
    @TestMetadata("variable.kt")
    public void testVariable() throws Exception {
        doTestWithIfSurrounder("idea/testData/codeInsight/surroundWith/if/variable.kt");
    }
    
}