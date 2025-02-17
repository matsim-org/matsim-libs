# Profiling Contrib

Modules and usage examples for integrating and enhancing profiling.

- How to instrument profiling
- Create MATSim-specific JFR events during profiling
- Create and fire custom events via aspect-oriented programming

## Add dependency

Add as dependency to the `pom.xml`:

```xml
<dependency>
	<groupId>org.matsim.contrib</groupId>
	<artifactId>profiling-utils</artifactId>
	<version>${matsim.version}</version>
	<scope>compile</scope>
</dependency>
```

## Using Profiling

There are two ways to use profiling:
- continuous for the full program execution
- instrumenting start and stop of the profiler during execution

<ul>
  <li>
<details><summary>continuous for the full program execution</summary>

This variant results in big recording files and possibly incurs the most overhead.  
It solely requires additional options to the java execution command:

```sh
-XX:StartFlightRecording=name="myRecording",dumponexit=true,maxsize=0,filename="myRecording.jfr" -XX:FlightRecorderOptions=stackdepth=2048,repository="/tmp"
```

Name and filename can be set to your liking. `repository` should be set to a fast, temporary directory with enough available space.

</details>
  </li>
  <li>
<details><summary>instrumenting profiler start/stop during execution</summary>

The `ProfilerInstrumentationModule` can be used to create a profiling recording only for a chosen number of iterations.

```java
controller.addOverridingModule(new ProfilerInstrumentationModule(
        defaultConfiguration()
            .startIteration(10)
            .endIteration.(35)
            .outputPath(Path.of(ConfigUtils.addOrGetModule(getConfig(), ControllerConfigGroup.class).getOutputDirectory(), "profile.jfr"))
));
```
The output path given in the example is the default. You only need to specify it, if you want to use a different path or filename.

</details>
  </li>
</ul>

## JFR Events

Custom events can be created and recorded into the profiling recording.
This allows to define additional info to be recorded with a timestamp or duration.

### Use pre-defined Events from ProfilingEventsModule

The `ProfilingEventsModule` defines some MATSim specific events and registers
Listeners to trigger them during the execution.

- JFRIterationEvent - For the duration of each iteration with the current iteration count
- JFRMobsimEvent - For the duration of each mobsim execution
- JFRMatsimEvent - Marker events for startup/shutdown, scoring, and replanning

To use, add the module via `controller.addOverridingModule(new ProfilingEventsModule());`.

### Define custom Events

