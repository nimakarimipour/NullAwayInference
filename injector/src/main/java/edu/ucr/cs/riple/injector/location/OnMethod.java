/*
 * MIT License
 *
 * Copyright (c) 2020 Nima Karimipour
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

package edu.ucr.cs.riple.injector.location;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import edu.ucr.cs.riple.injector.Helper;
import edu.ucr.cs.riple.injector.SignatureMatcher;
import edu.ucr.cs.riple.injector.changes.Change;
import edu.ucr.cs.riple.injector.modifications.Modification;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.json.simple.JSONObject;

/** Represents a location for method element. This location is used to apply changes to a method. */
public class OnMethod extends Location {

  /** Method signature of the target element. */
  public final String method;
  /**
   * Matcher for the method signature. Method signature is given as a string, this matcher is used
   * to match the target.
   */
  public final SignatureMatcher matcher;

  public OnMethod(Path path, String clazz, String method) {
    super(LocationKind.METHOD, path, clazz);
    this.method = method;
    this.matcher = new SignatureMatcher(method);
  }

  public OnMethod(String path, String clazz, String method) {
    this(Helper.deserializePath(path), clazz, method);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void fillJsonInformation(JSONObject res) {
    res.put(KEYS.METHOD, method);
  }

  @Override
  protected Modification applyToMember(NodeList<BodyDeclaration<?>> members, Change change) {
    final AtomicReference<Modification> ans = new AtomicReference<>();
    members.forEach(
        bodyDeclaration ->
            bodyDeclaration.ifCallableDeclaration(
                callableDeclaration -> {
                  if (ans.get() != null) {
                    // already found the member.
                    return;
                  }
                  if (this.matcher.matchesCallableDeclaration(callableDeclaration)) {
                    ans.set(change.visit(callableDeclaration));
                  }
                }));
    if (ans.get() == null) {
      members.forEach(
          bodyDeclaration ->
              bodyDeclaration.ifAnnotationMemberDeclaration(
                  annotationMemberDeclaration -> {
                    if (annotationMemberDeclaration
                        .getNameAsString()
                        .equals(Helper.extractCallableName(method))) {
                      ans.set(change.visit(annotationMemberDeclaration));
                    }
                  }));
    }
    return ans.get();
  }

  @Override
  public void ifMethod(Consumer<OnMethod> consumer) {
    consumer.accept(this);
  }

  @Override
  public boolean isOnMethod() {
    return true;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof OnMethod)) {
      return false;
    }
    OnMethod other = (OnMethod) o;
    return super.equals(other) && method.equals(other.method);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), method);
  }

  @Override
  public String toString() {
    return "OnMethod{" + "method='" + method + '\'' + ", clazz='" + clazz + '\'' + '}';
  }
}
