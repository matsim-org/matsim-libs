
# Distributed MATSim

This contrib contains functionality to run distributed MATSim simulations. 
Most of the functionality is already in the core MATSim code, but this package is needed to run simulations across multiple JVMs.

The examples here are also useful to parallelize in one JVM across multiple threads.

## Prerequisites

The following must be installed on the system

- Java 25 (no other version has been tested)
- Maven
- METIS (optional, for partitioning the network)

If METIS can not be found on the system, the network will be partitioned using a simple bisection algorithm.

## How to run distributed simulations

The enable the functionality of a distributed simulation, the distributed mobsim needs to be enabled in the `controller` config, either via xml or in code like so:
```java
config.controller().setMobsim(ControllerConfigGroup.MobsimType.dsim.name());
```

Furthermore, `DSim` has a separate config section, which provides similar config options as the QSim:

```
<module name="dsim" >
    <!-- End time of the simulation. Default: 24:00:00 -->
    <param name="endTime" value="24:00:00" />
    
    <!-- Link dynamics determine how vehicles can overtake. Options: [FIFO, PassingQ, Seepage(not implemented)], Default: PassingQ -->
    <param name="linkDynamics" value="PassingQ" />

    <!-- Modes which are simulated on the network. All other modes, expect pt will be teleported. Default: car -->
    <param name="networkModes" value="car" />
    
    <!-- Partitioning strategy for the network. Options: [none, bisect, metis], default: 'bisect' -->
    <param name="partitioning" value="bisect" />
    
    <!-- Start time of the simulation. Default: 00:00:00 -->
    <param name="startTime" value="00:00:00" />
    
    <!-- Stuck time [s] after which a vehicle is pushed onto the next link regardless of available capacity. Default: 30 -->
    <param name="stuckTime" value="30.0" />
    
    <!-- Number of threads to use for execution. If <= 0, the number of available processors is used. -->
    <param name="threads" value="0" />
    
    <!-- Traffic dynamics determine how storage capacities and inflow capacities are freed. Options: [queue, kinematicWaves, withHoles (not implemented), Default: kinematicWaves -->
    <param name="trafficDynamics" value="kinematicWaves" />
    
    <!-- Determines how agents can access vehicles when starting a trip. Options=[teleport, wait (not implemented), exception], Default: teleport -->
    <param name="vehicleBehavior" value="teleport" />
</module>
```

To run in a real distributed environment, across multiple JVMs, a `Communicator` needs to be created.
Please refer to the content of this contrib to see which implementations are available.

Additionally, the controller needs to be created with a `DistributedContext`.
This can be done in the usual way of creating the controller, but with the `DistributedContext` as an additional argument.

```java
import org.matsim.core.communication.Communicator;
import org.matsim.core.communication.HazelcastCommunicator;

Communicator comm =  new HazelcastCommunicator(0, 2, null);
Controler controler = new Controler(scenario, DistributedContext.create(config));
```
The distributed setup does not require any further modules, as the `DistributedSimulationModule`, which includes the required classes, is automatically
added.

At this point, the controller can be run as usual or other custom modules can be installed.

> [!NOTE]
>  Most of the communication libraries require certain JVM options. If you want to run a real distributed setup always add the following arguments.

>[!NOTE]
> --add-exports java.base/jdk.internal.ref=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.management/sun.management=ALL-UNNAMED --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED

## Example MATSim run class

An example how to integrate different communicators within a MATSim run class can be found in the [RunDistributedSim](src/main/java/org/matsim/dsim/RunDistributedSim.java) example.

## Building executable jar

This contrib also contains some utility classes to test and benchmark the network communication. See `RunMessagingBenchmark.java` for more information.

In the contrib directory, run the following command:
```shell
mvn package -DskipShaded=false
```

This will build an executable jar file at `sim.jar`.