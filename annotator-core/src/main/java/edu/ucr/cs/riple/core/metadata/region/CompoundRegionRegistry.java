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

package edu.ucr.cs.riple.core.metadata.region;

import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.metadata.region.generatedcode.AnnotationProcessorHandler;
import edu.ucr.cs.riple.core.metadata.region.generatedcode.LombokHandler;
import edu.ucr.cs.riple.core.module.ModuleInfo;
import edu.ucr.cs.riple.injector.location.Location;
import edu.ucr.cs.riple.scanner.generatedcode.SourceType;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Container class for all region registries. This region registry can identify impacted regions for
 * all fix types.
 */
public class CompoundRegionRegistry implements RegionRegistry {

  /** List of all region registries. */
  private final ImmutableSet<RegionRegistry> registries;
  /**
   * List of all generated region registries. Generated region registries can extend the impacted
   * regions created by generated code which are not present in source code.
   */
  private final ImmutableSet<AnnotationProcessorHandler> generatedRegionsRegistries;

  private final ModuleInfo moduleInfo;

  public CompoundRegionRegistry(Config config, ModuleInfo moduleInfo) {
    this.moduleInfo = moduleInfo;
    MethodRegionRegistry methodRegionRegistry = new MethodRegionRegistry(moduleInfo);
    this.registries =
        ImmutableSet.of(
            new FieldRegionRegistry(moduleInfo),
            methodRegionRegistry,
            new ParameterRegionRegistry(moduleInfo, methodRegionRegistry));
    ImmutableSet.Builder<AnnotationProcessorHandler> generatedRegionRegistryBuilder =
        new ImmutableSet.Builder<>();
    if (config.generatedCodeDetectors.contains(SourceType.LOMBOK)) {
      generatedRegionRegistryBuilder.add(new LombokHandler(methodRegionRegistry));
    }
    this.generatedRegionsRegistries = generatedRegionRegistryBuilder.build();
  }

  @Override
  public Optional<Set<Region>> getImpactedRegions(Location location) {
    Set<Region> regions = new HashSet<>();
    this.registries.forEach(
        registry -> registry.getImpactedRegions(location).ifPresent(regions::addAll));
    this.generatedRegionsRegistries.forEach(
        registry -> regions.addAll(registry.extendForGeneratedRegions(moduleInfo, regions)));
    return Optional.of(regions);
  }
}
