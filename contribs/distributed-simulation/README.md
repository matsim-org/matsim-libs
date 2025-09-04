
# Distributed MATSim

This contrib contains functionality to run distributed MATSim simulations. 
Most of the functionality is already in the core MATSim code, but this package is needed to run simulations across multiple JVMs.

The examples here are also useful to parallelize in one JVM across multiple threads.

## Prerequisites

The following must be installed on the system

- Java 21 (no other version has been tested)
- Maven
- METIS (optional, for partitioning the network)

If METIS can not be found on the system, the network will be partitioned using a simple bisection algorithm.

## How to run distributed simulations

> [!NOTE]
> This project uses preview features of the JVM. Additionally, most of the communication libraries require certain options.
> Always run simulations with the [options] given below.

>[!NOTE]
> --enable-preview --add-exports java.base/jdk.internal.ref=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.management/sun.management=ALL-UNNAMED --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED

The enable the functionality of a distributed simulation, the distributed mobsim needs to be enabled in the config, either via xml or in code like so:
```java
config.controller().setMobsim(ControllerConfigGroup.MobsimType.dsim.name());
```

Additionally, the controller needs to be created with a `DistributedContext`.
This can be done in the usual way of creating the controller, but with the `DistributedContext` as an additional argument.
The same syntax is also used when running a parallel simulation in one JVM.

To create a local simulation use the following code:
```java
Controler controler = new Controler(scenario, DistributedContext.createLocal(config));
```

To run in a real distributed environment, across multiple JVMs, a `Communicator` needs to be created. 
Please refer to the content of this contrib to see which implementations are available.

```java
import org.matsim.core.communication.Communicator;
import org.matsim.core.communication.HazelcastCommunicator;

Communicator comm =  new HazelcastCommunicator(0, 2, null);
Controler controler = new Controler(scenario, DistributedContext.create(config));
```
The distributed setup does not require any further modules, as the `DistributedSimulationModule`, which includes the required classes, is automatically
added.

At this point, the controller can be run as usual or other custom modules can be installed.

## Example MATSim run class

An example how to integrate different communicators within a MATSim run class can be found in the [RunDistributedSim](src/main/java/org/matsim/dsim/RunDistributedSim.java) example.

## Building executable jar

This contrib also contains some utility classes to test and benchmark the network communication. See `RunMessagingBenchmark.java` for more information.

In the contrib directory, run the following command:
```shell
mvn package -DskipShaded=false
```

This will build an executable jar file at `sim.jar`.