[# Profiling Contrib

Modules and usage examples for integrating and enhancing profiling.

- How to instrument profiling
- Create MATSim-specific JFR events during profiling
- Create and fire custom events via aspect-oriented programming

## Add dependency

Add as dependency to the `pom.xml`:

```xml
<dependency>
	<groupId>org.matsim.contrib</groupId>
	<artifactId>profiling</artifactId>
	<version>${matsim.version}</version>
	<scope>compile</scope>
</dependency>
```

## Using Profiling

There are two ways to use profiling:

- using the `org.matsim.contrib.profiling.instrument.EnableProfilingModule` to profile selected iterations

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
<details><summary>using the `EnableProfilingModule` to profile selected iterations</summary>

The `org.matsim.contrib.profiling.instrument.EnableProfilingModule` can be used to create a profiling recording only for a chosen number of iterations.

```java
controller.addOverridingModule(new ProfilerInstrumentationModule(10, 20, "profile");
```

This will create a recording `profile.jfr` in the configured controller output directory,
starts to record in iteration 10, and stop after iteration 20.  
Omitting the name will default to including the iterations, i.e. `profile-10-20.jfr`.  
The endIteration can be omitted to only record the single given iteration.

The following Java options should still be used additionally:

```
-XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints"
```

For more detailed stacktraces (and potentially more overhead): `-XX:FlightRecorderOptions=stackdepth=2048`. 


</details>
  </li>
</ul>

## JFR Events

Custom events can be created and recorded into the profiling recording.
This allows to define additional info to be recorded with a timestamp or duration.

### Use pre-defined Events from FireDefaultProfilingEventsModule

The `FireDefaultProfilingEventsModule` defines some MATSim-specific events and registers
Listeners to trigger them during the execution.

- IterationJfrEvent - For the duration of each iteration (including starts/ends listeners) with the current iteration count
- IterationStartsListenersJfrEvent, IterationEndsListenersJfrEvent - for the duration of iteration starts/ends listeners respectively
- BeforeMobsimListenersJfrEvent - Duration of before mobsim listeners per iteration
- AfterMobsimListenersJfrEvent - Duration of after mobsim listeners per iteration
- MobsimJfrEvent - For the duration of each mobsim execution
- ReplanningListenersJfrEvent - Duration of replanning listeners
- ScoringListenersJfrEvent - Duration of scoring listeners
- MatsimStartupListenersJfrEvent, MatsimShutdownListenersJfrEvent - Duration of all startup/shutdown listeners
- MatsimJfrEvent - unused, generic event for convenience to provide a type per string instead of defining your own event class

To use, add the module via `controller.addOverridingModule(new ProfilingEventsModule());`.

Using the listeners provided by MATSim is not the most accurate approach,
as the ControlerListeners **won't measure the core listeners**
and the mobsim initialized/cleanup listeners don't have a priority to control to include other listeners.

### Define custom Events

Custom events can be defined easily:

```java
import jdk.jfr.*;

/**
 * Record Mobsim execution duration as a JFR profiling {@link Event}.
 */
@Name("matsim.Mobsim")
@Label("Mobsim")
@Description("Duration of a mobsim iteration")
@Category({"MATSim", "MATSim Operation Listeners"})
public class MobsimJfrEvent extends Event {}
```

The annotations provide metadata that will be included in the recording.

To use the event:

```java
var event = new MobsimJfrEvent();
event.begin(); // optional, only required to record a duration
// do something
event.end(); // optional, as commit will end the duration automatically
event.commit(); // commit the event to the recording
```

If MATSim listeners are not enough and you don't want to or can't insert these instructions into existing library code,
try aspect-oriented programming via aspectj.

## Aspect-Oriented Programming via aspectj

[AspectJ](https://eclipse.dev/aspectj/doc/latest/index.html) requires a bit more setup.
The prototype at https://git.tu-berlin.de/stendler/matsim-berlin contains an example on how to include aspectj during build via a maven module.
But you can also try the aspectj java runtime agent instead.

This library provides aspects to create accurate events equivalent to those created by the FireDefaultProfilingEventsModule.  
You can include those or refer to those as inspiration to create your own.
