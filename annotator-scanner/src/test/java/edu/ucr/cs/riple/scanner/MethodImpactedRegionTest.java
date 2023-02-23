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

package edu.ucr.cs.riple.scanner;

import com.google.common.base.Preconditions;
import edu.ucr.cs.riple.scanner.tools.DisplayFactory;
import edu.ucr.cs.riple.scanner.tools.TrackerNodeDisplay;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class MethodImpactedRegionTest extends AnnotatorScannerBaseTest<TrackerNodeDisplay> {

  private static final DisplayFactory<TrackerNodeDisplay> METHOD_TRACKER_DISPLAY_FACTORY =
      values -> {
        Preconditions.checkArgument(values.length == 5, "Expected to find 5 values on each line");
        return new TrackerNodeDisplay(values[0], values[1], values[3], values[2], values[4]);
      };
  private static final String HEADER =
      "REGION_CLASS"
          + '\t'
          + "REGION_MEMBER"
          + '\t'
          + "USED_MEMBER"
          + '\t'
          + "USED_CLASS"
          + '\t'
          + "SOURCE_TYPE";
  private static final String FILE_NAME = "method_impacted_region_map.tsv";

  public MethodImpactedRegionTest() {
    super(METHOD_TRACKER_DISPLAY_FACTORY, HEADER, FILE_NAME);
  }

  @Test
  public void BasicTest() {
    tester
        .addSourceLines(
            "edu/ucr/A.java",
            "package edu.ucr;",
            "public class A {",
            "   public Object bar(){",
            "      Other o = new Other();",
            "      return o.foo();",
            "   }",
            "}",
            "class Other {",
            "   Object foo() { return null; };",
            "}")
        .setExpectedOutputs(
            new TrackerNodeDisplay("edu.ucr.A", "bar()", "edu.ucr.Other", "foo()"),
            new TrackerNodeDisplay("edu.ucr.A", "bar()", "edu.ucr.Other", "Other()"))
        .doTest();
  }

  @Test
  public void constructorCallTest() {
    tester
        .addSourceLines(
            "edu/ucr/A.java",
            "package edu.ucr;",
            "public class A {",
            "   A a = new A();",
            "   A b = new A(0);",
            "   A() { }",
            "   A(int i) { }",
            "   void bar() {",
            "       A a = new A();",
            "   }",
            "}")
        .addSourceLines(
            "edu/ucr/B.java",
            "package edu.ucr;",
            "public class B extends A {",
            "   B b = new B();",
            "   B() { }",
            "   void run() {",
            "       A a = new A();",
            "   }",
            "}")
        .setExpectedOutputs(
            new TrackerNodeDisplay("edu.ucr.A", "a", "edu.ucr.A", "A()"),
            new TrackerNodeDisplay("edu.ucr.A", "b", "edu.ucr.A", "A(int)"),
            new TrackerNodeDisplay("edu.ucr.A", "bar()", "edu.ucr.A", "A()"),
            new TrackerNodeDisplay("edu.ucr.B", "run()", "edu.ucr.A", "A()"),
            new TrackerNodeDisplay("edu.ucr.B", "b", "edu.ucr.B", "B()"))
        .doTest();
  }

  @Test
  public void methodReference() {
    tester
        .addSourceLines(
            "edu/ucr/A.java",
            "package edu.ucr;",
            "public interface A {",
            "  void foo(Object field);",
            "}")
        .addSourceLines(
            "edu/ucr/B.java",
            "package edu.ucr;",
            "public class B {",
            "  void useA(A a) { }",
            "  void bar() {",
            "      useA(System.out::println);",
            "  }",
            "}")
        .setExpectedOutputs(
            new TrackerNodeDisplay("edu.ucr.B", "bar()", "edu.ucr.A", "foo(java.lang.Object)"),
            new TrackerNodeDisplay("edu.ucr.B", "bar()", "edu.ucr.B", "useA(edu.ucr.A)"),
            new TrackerNodeDisplay(
                "edu.ucr.B", "bar()", "java.io.PrintStream", "println(java.lang.Object)"))
        .doTest();
  }

  @Test
  public void lambda() {
    tester
        .addSourceLines(
            "edu/ucr/A.java",
            "package edu.ucr;",
            "public interface A {",
            "  void foo(Object field);",
            "}")
        .addSourceLines(
            "edu/ucr/B.java",
            "package edu.ucr;",
            "public class B {",
            "  void useA(A a) { }",
            "  void memberReference() {",
            "      useA(System.out::println);",
            "  }",
            "  void lambda() {",
            "      useA(e -> System.out.println(e));",
            "  }",
            "}")
        .setExpectedOutputs(
            new TrackerNodeDisplay(
                "edu.ucr.B", "memberReference()", "edu.ucr.A", "foo(java.lang.Object)"),
            new TrackerNodeDisplay(
                "edu.ucr.B", "memberReference()", "edu.ucr.B", "useA(edu.ucr.A)"),
            new TrackerNodeDisplay(
                "edu.ucr.B",
                "memberReference()",
                "java.io.PrintStream",
                "println(java.lang.Object)"),
            new TrackerNodeDisplay("edu.ucr.B", "lambda()", "edu.ucr.A", "foo(java.lang.Object)"),
            new TrackerNodeDisplay("edu.ucr.B", "lambda()", "edu.ucr.B", "useA(edu.ucr.A)"),
            new TrackerNodeDisplay(
                "edu.ucr.B", "lambda()", "java.io.PrintStream", "println(java.lang.Object)"))
        .doTest();
  }
}
