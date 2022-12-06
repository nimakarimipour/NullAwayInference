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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.metadata.field.FieldDeclarationStore;
import edu.ucr.cs.riple.core.metadata.method.MethodDeclarationTree;
import edu.ucr.cs.riple.core.metadata.trackers.Region;
import edu.ucr.cs.riple.injector.location.Location;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/** Represents an error reported by NullAway. */
@SuppressWarnings("JavaLangClash")
public class Error extends Enclosed {

  /** Error Type. */
  public final String messageType;
  /** Error message. */
  public final String message;
  /** Set of fixes that can resolve this error if exists. */
  private final ImmutableSet<Fix> fixes;

  public Error(String messageType, String message, Region region, @Nullable Fix fix) {
    this(messageType, message, region, fix == null ? Set.of() : Set.of(fix));
  }

  public Error(String messageType, String message, Region region, Set<Fix> fixes) {
    super(region);
    this.messageType = messageType;
    this.message = message;
    this.fixes = ImmutableSet.copyOf(fixes);
  }

  /**
   * Returns a factory that can create instances of Error object based on the given values. These
   * values correspond to a row in a TSV file generated by NullAway.
   *
   * @param config Config instance.
   * @return Factory instance.
   */
  public static Factory<Error> factory(Config config, FieldDeclarationStore store) {
    return values -> config.getAdapter().deserializeError(values, store);
  }

  /**
   * Checks if error is resolvable with only one fix.
   *
   * @return true if error is resolvable with only one fix and false otherwise.
   */
  public boolean isSingleFix() {
    return this.fixes.size() == 1;
  }

  /**
   * Checks if error is resolvable and all required fixes corresponds to elements in target module.
   *
   * @param tree Method declaration tree instance.
   * @return true if error is resolvable and all required fixes corresponds to elements in target
   *     module and false otherwise.
   */
  public boolean isFixableOnTarget(MethodDeclarationTree tree) {
    return this.fixes.size() > 0
        && fixes.stream().allMatch(fix -> tree.declaredInModule(fix.toLocation()));
  }

  /**
   * Returns the location the single fix that resolves this error.
   *
   * @return Location of the fix resolving this error.
   */
  public Location toResolvingLocation() {
    Preconditions.checkArgument(fixes.size() == 1);
    // no get() method, have to use iterator.
    return fixes.iterator().next().toLocation();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Error)) {
      return false;
    }
    Error error = (Error) o;
    return messageType.equals(error.messageType)
        && message.equals(error.message)
        && fixes.equals(error.fixes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(messageType, message, fixes);
  }

  @Override
  public String toString() {
    return "Type='" + messageType + '\'' + ", message='" + message + '\'';
  }

  private Set<Fix> getResolvingFixes() {
    return fixes;
  }

  public static Set<Fix> getResolvingFixesOfErrors(Collection<Error> errors) {
    return errors.stream()
        .flatMap(error -> error.getResolvingFixes().stream())
        .collect(Collectors.toSet());
  }
}
