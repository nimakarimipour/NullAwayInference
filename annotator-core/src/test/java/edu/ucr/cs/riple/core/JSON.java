/*
 * MIT License
 *
 * Copyright (c) 2022 Nima Karimipour
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package edu.ucr.cs.riple.core;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class JSON extends AnnotatorBaseCoreTest {

  public JSON() {
    super("json-property");
  }

  @Test
  public void field() {
    coreTestHelper
        .onTarget()
        .withSourceLines(
            "Bar.java",
            "package test;",
            "public class Bar {",
            "   public String foo;",
            "   public String foo2;",
            "   public void setFoo(String foo) {",
            "     this.foo = foo;",
            "   }",
            "   public String getFoo() {",
            "     return foo;",
            "   }",
            "}")
        .withDependency("Dep")
        .withSourceLines(
            "Dep.java",
            "package test.dep;",
            "import test.Bar;",
            "public class Dep {",
            "   public Bar bar = new Bar();",
            "   public void exec() {",
            "     bar.foo2 = bar.getFoo();",
            "   }",
            "}")
        .withExpectedReports()
        .disableBailOut()
        .enableDownstreamDependencyAnalysis(AnalysisMode.STRICT)
        .toDepth(5)
        .start();
  }
}
