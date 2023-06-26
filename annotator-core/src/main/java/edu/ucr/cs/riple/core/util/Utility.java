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

package edu.ucr.cs.riple.core.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.Context;
import edu.ucr.cs.riple.core.Report;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.core.metadata.region.Region;
import edu.ucr.cs.riple.core.metadata.region.RegionRecord;
import edu.ucr.cs.riple.core.module.ModuleConfiguration;
import edu.ucr.cs.riple.core.module.ModuleInfo;
import edu.ucr.cs.riple.scanner.AnnotatorScanner;
import edu.ucr.cs.riple.scanner.ScannerConfigWriter;
import edu.ucr.cs.riple.scanner.generatedcode.SourceType;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarStyle;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/** Utility class. */
public class Utility {

  /**
   * Executes a shell command in a subprocess. If {@link Config#redirectBuildOutputToStdErr} is
   * activated, it will write the command's output in std error.
   *
   * @param config Annotator configuration.
   * @param command The shell command to run.
   */
  public static void executeCommand(Config config, String command) {
    try {
      Process p = Runtime.getRuntime().exec(new String[] {"/bin/sh", "-c", command});
      BufferedReader reader =
          new BufferedReader(new InputStreamReader(p.getErrorStream(), Charset.defaultCharset()));
      String line;
      while ((line = reader.readLine()) != null) {
        if (config.redirectBuildOutputToStdErr) {
          System.err.println(line);
        }
      }
      p.waitFor();
    } catch (Exception e) {
      throw new RuntimeException("Exception happened in executing command: " + command, e);
    }
  }

  /**
   * Writes reports content in json format in reports.json file in the output directory.
   *
   * @param context Annotator context.
   * @param reports Immutable set of reports.
   */
  @SuppressWarnings("unchecked")
  public static void writeReports(Context context, ImmutableSet<Report> reports) {
    Path reportsPath = context.config.globalDir.resolve("reports.json");
    JSONObject result = new JSONObject();
    JSONArray reportsJson = new JSONArray();
    for (Report report : reports) {
      JSONObject reportJson = report.root.getJson();
      reportJson.put("LOCAL EFFECT", report.localEffect);
      reportJson.put("OVERALL EFFECT", report.getOverallEffect(context.config));
      reportJson.put("Upper Bound EFFECT", report.getUpperBoundEffectOnDownstreamDependencies());
      reportJson.put("Lower Bound EFFECT", report.getLowerBoundEffectOnDownstreamDependencies());
      reportJson.put("FINISHED", !report.requiresFurtherProcess(context.config));
      JSONArray followUps = new JSONArray();
      if (context.config.chain && report.localEffect < 1) {
        followUps.addAll(report.tree.stream().map(Fix::getJson).collect(Collectors.toList()));
      }
      reportJson.put("TREE", followUps);
      reportsJson.add(reportJson);
    }
    // Sort by overall effect.
    reportsJson.sort(
        (o1, o2) -> {
          int first = (Integer) ((JSONObject) o1).get("OVERALL EFFECT");
          int second = (Integer) ((JSONObject) o2).get("OVERALL EFFECT");
          return Integer.compare(second, first);
        });
    result.put("REPORTS", reportsJson);
    try (BufferedWriter writer =
        Files.newBufferedWriter(reportsPath.toFile().toPath(), Charset.defaultCharset())) {
      writer.write(result.toJSONString().replace("\\/", "/").replace("\\\\\\", "\\"));
      writer.flush();
    } catch (IOException e) {
      throw new RuntimeException(
          "Could not create the Annotator report json file: " + reportsPath, e);
    }
  }

  /**
   * Reads serialized errors "errors.tsv" file in the output directory, and returns the collected
   * set of resolving fixes for read errors.
   *
   * @param context Context of the annotator.
   * @param moduleInfo ModuleInfo of the module which errors are created for.
   * @return Set of collected fixes.
   */
  public static Set<Fix> readFixesFromOutputDirectory(Context context, ModuleInfo moduleInfo) {
    return Error.getResolvingFixesOfErrors(context.checker.deserializeErrors(moduleInfo));
  }

  /**
   * Reads serialized errors of passed module in "errors.tsv" file in the output directory,
   *
   * @param context Annotation context. Required to fetch the deserializer.
   * @param moduleInfo ModuleInfo of the module which errors are created for.
   * @return Set of serialized errors.
   */
  public static <T extends Error> Set<T> readErrorsFromOutputDirectory(
      Context context, ModuleInfo moduleInfo, Class<T> klass) {
    return context.checker.deserializeErrors(moduleInfo).stream()
        .map(klass::cast)
        .collect(Collectors.toSet());
  }

  /**
   * Activates/Deactivates {@link AnnotatorScanner} features by updating the {@link
   * edu.ucr.cs.riple.scanner.Config} in {@code XML} format for the given module.
   *
   * @param config Annotator configuration.
   * @param info module that its configuration file need to be updated.
   * @param activation activation flag for all features of the scanner.
   */
  public static void setScannerCheckerActivation(
      Config config, ModuleConfiguration info, boolean activation) {
    ScannerConfigWriter writer = new ScannerConfigWriter();
    writer
        .setSerializationActivation(activation)
        .addGeneratedCodeDetectors(config.generatedCodeDetectors)
        .setOutput(info.dir)
        .setNonnullAnnotations(config.getNonnullAnnotations())
        .writeAsXML(info.scannerConfig);
  }

  /**
   * Activates/Deactivates {@link AnnotatorScanner} features by updating the {@link
   * edu.ucr.cs.riple.scanner.Config} in {@code XML} format for the given modules.
   *
   * @param config Annotator configuration.
   * @param modules Immutable set of modules that their configuration files need to be updated.
   * @param activation activation flag for all features of the scanner.
   */
  public static void setScannerCheckerActivation(
      Config config, ImmutableSet<ModuleConfiguration> modules, boolean activation) {
    modules.forEach(info -> setScannerCheckerActivation(config, info, activation));
  }

  /**
   * Runs the scanner checker on the given modules.
   *
   * @param context Annotator context.
   * @param configurations Immutable set of modules that their configuration files need to be
   *     updated.
   * @param buildCommand build command to run the Scanner checker.
   */
  public static void runScannerChecker(
      Context context, ImmutableSet<ModuleConfiguration> configurations, String buildCommand) {
    Utility.setScannerCheckerActivation(context.config, configurations, true);
    Utility.build(context, buildCommand);
    Utility.setScannerCheckerActivation(context.config, configurations, false);
  }

  /**
   * Deserializes a {@link RegionRecord} corresponding to values stored in a string array.
   *
   * @param values String array of values.
   * @return Deserialized {@link RegionRecord} instance corresponding to the given values.
   */
  public static RegionRecord deserializeImpactedRegionRecord(String[] values) {
    Preconditions.checkArgument(
        values.length == 5,
        "Expected 5 values to create Impacted Region Record instance in this version of Annotator but found: "
            + values.length);
    return new RegionRecord(
        new Region(values[0], values[1], SourceType.valueOf(values[4])), values[2], values[3]);
  }

  /**
   * Builds all downstream dependencies.
   *
   * @param context Annotator context.
   */
  public static void buildDownstreamDependencies(Context context) {
    context.checker.prepareConfigFilesForBuild(context.downstreamConfigurations);
    build(context, context.config.downstreamDependenciesBuildCommand);
  }

  /**
   * Builds target with control on field initialization serialization.
   *
   * @param context Annotator context.
   */
  public static void buildTarget(Context context) {
    context.checker.prepareConfigFilesForBuild(context.targetModuleInfo.getModuleConfigurations());
    build(context, context.config.buildCommand);
  }

  /**
   * Builds module(s).
   *
   * @param context Annotator context.
   * @param command Command to run to build module(s).
   */
  public static void build(Context context, String command) {
    try {
      long timer = context.log.startTimer();
      Utility.executeCommand(context.config, command);
      context.log.stopTimerAndCaptureBuildTime(timer);
      context.log.incrementBuildRequest();
    } catch (Exception e) {
      throw new RuntimeException("Could not run command: " + command, e);
    }
  }

  /**
   * Returns a progress bar with the given task name.
   *
   * @param taskName Task name.
   * @param steps Number of total steps to show in the progress bar.
   * @return Progress bar instance.
   */
  public static ProgressBar createProgressBar(String taskName, int steps) {
    return new ProgressBar(
        taskName,
        steps,
        1000,
        System.out,
        ProgressBarStyle.ASCII,
        "",
        1,
        false,
        null,
        ChronoUnit.SECONDS,
        0L,
        Duration.ZERO);
  }

  /**
   * Writes log in the `log.txt` file at the output directory.
   *
   * @param context Annotator context.
   */
  public static void writeLog(Context context) {
    Path path = context.config.globalDir.resolve("log.txt");
    try {
      Files.write(path, Collections.singleton(context.log.toString()), Charset.defaultCharset());
    } catch (IOException exception) {
      System.err.println("Could not write log to: " + path);
      System.err.println("Writing in STD Error:\n" + context.log);
    }
  }

  /**
   * Read all lines from a file and returns as a List.
   *
   * @param path The path to the file.
   * @return The lines from the file as a Stream.
   */
  public static List<String> readFileLines(Path path) {
    try (Stream<String> stream = Files.lines(path, Charset.defaultCharset())) {
      return stream.collect(Collectors.toList());
    } catch (IOException e) {
      throw new RuntimeException("Exception while reading file: " + path, e);
    }
  }

  public static void timeStamp(Config config) {
    Path path = config.globalDir.resolve("time-stamp.txt");
    if (!path.toFile().exists()) {
      // make the file if it doesn't exist
      try {
        Files.createFile(path);
      } catch (IOException e) {
        throw new RuntimeException("Exception while creating file: " + path, e);
      }
    }
    // append the current time to the file
    try {
      Files.write(
          path,
          Collections.singleton(LocalDateTime.now(ZoneId.of("America/Los_Angeles")) + "\n"),
          Charset.defaultCharset(),
          StandardOpenOption.APPEND);
    } catch (IOException e) {
      throw new RuntimeException("Exception while writing in file: " + path, e);
    }
  }
}
