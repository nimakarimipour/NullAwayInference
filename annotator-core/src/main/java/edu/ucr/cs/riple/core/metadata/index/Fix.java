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

package edu.ucr.cs.riple.core.metadata.index;

import com.google.common.collect.Sets;
import edu.ucr.cs.riple.injector.Helper;
import edu.ucr.cs.riple.injector.changes.AddAnnotation;
import edu.ucr.cs.riple.injector.location.Location;
import edu.ucr.cs.riple.injector.location.OnField;
import edu.ucr.cs.riple.injector.location.OnMethod;
import edu.ucr.cs.riple.injector.location.OnParameter;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import org.json.simple.JSONObject;

/**
 * Stores information suggesting adding @Nullable on an element in source code. These suggestions
 * are coming form NullAway.
 */
public class Fix {

  /** Suggested change. */
  public final AddAnnotation change;
  /** Reasons this fix is suggested by NullAway in string. */
  public final Set<String> reasons;

  /**
   * If true, the fix is suggested due to an error in the target module, false if the fix is
   * suggested due to error in downstream dependencies.
   */
  public boolean fixSourceIsInTarget;

  public Fix(AddAnnotation change, String reason, boolean fixSourceIsInTarget) {
    this.change = change;
    this.reasons = reason != null ? Sets.newHashSet(reason) : new HashSet<>();
    this.fixSourceIsInTarget = fixSourceIsInTarget;
  }

  /**
   * Returns the targeted location information.
   *
   * @return Location information.
   */
  public Location toLocation() {
    return change.location;
  }

  /**
   * Checks if fix is targeting a method.
   *
   * @return true, if fix is targeting a method.
   */
  public boolean isOnMethod() {
    return change.location.isOnMethod();
  }

  /**
   * Returns the targeted method information.
   *
   * @return Target method information.
   */
  public OnMethod toMethod() {
    return change.location.toMethod();
  }

  /**
   * Visit this fix if it is targeting an element of kind {@link
   * edu.ucr.cs.riple.injector.location.LocationType#METHOD}
   *
   * @param consumer Consumer instance.
   */
  public void ifOnMethod(Consumer<OnMethod> consumer) {
    change.location.ifMethod(consumer);
  }

  /**
   * Checks if fix is targeting a parameter.
   *
   * @return true, if fix is targeting a parameter.
   */
  public boolean isOnParameter() {
    return change.location.isOnParameter();
  }

  /**
   * Returns the targeted parameter information.
   *
   * @return Target method parameter.
   */
  public OnParameter toParameter() {
    return change.location.toParameter();
  }

  /**
   * Visit this fix if it is targeting an element of kind {@link
   * edu.ucr.cs.riple.injector.location.LocationType#PARAMETER}
   *
   * @param consumer Consumer instance.
   */
  public void ifOnParameter(Consumer<OnParameter> consumer) {
    change.location.ifParameter(consumer);
  }

  /**
   * Checks if fix is targeting a field.
   *
   * @return true, if fix is targeting a field.
   */
  public boolean isOnField() {
    return change.location.isOnField();
  }

  /**
   * Returns the targeted field information.
   *
   * @return Target field information.
   */
  public OnField toField() {
    return change.location.toField();
  }

  /**
   * Visit this fix if it is targeting an element of kind {@link
   * edu.ucr.cs.riple.injector.location.LocationType#FIELD}
   *
   * @param consumer Consumer instance.
   */
  public void ifOnField(Consumer<OnField> consumer) {
    change.location.ifField(consumer);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Fix)) {
      return false;
    }
    Fix fix = (Fix) o;
    return change.equals(fix.change);
  }

  @Override
  public int hashCode() {
    return Objects.hash(change);
  }

  /**
   * Returns json representation of fix.
   *
   * @return Json instance.
   */
  public JSONObject getJson() {
    return change.getJson();
  }

  /**
   * Checks if fix is modifying constructor (parameter or method).
   *
   * @return true, if fix is modifying constructor.
   */
  public boolean isModifyingConstructor() {
    if (isOnField()) {
      return false;
    }
    String methodSignature = isOnMethod() ? toMethod().method : toParameter().method;
    return Helper.extractCallableName(methodSignature)
        .equals(Helper.simpleName(change.location.clazz));
  }

  @Override
  public String toString() {
    return change.toString();
  }
}
