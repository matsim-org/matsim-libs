# SimWrapper Contrib

Contrib to provide [SimWrapper](https://simwrapper.github.io/) integration.
SimWrapper provides a wide range of visualization options within the web browser, allowing users to analyze,
evaluate and disseminate MATSim runs visually.

This extension allows you to automatically create SimWrapper dashboards after simulation runs have finished.
Additionally, to generating dashboard YAML files, it will also take care of executing all necessary post-process steps to
generate required data files.

To use it, include the maven dependency, and install the module:

```java
controler.addOverridingModule(new SimWrapperModule())
```

## Custom Dashboards

This module not only provides a set of default dashboards, but is designed to be extended with scenario specific
dashboards as needed.
The existing dashboards in `org.matsim.simwrapper.dashboard` serve as good examples to get started.

Internally SimWrapper works mostly with YAML files, but in the Java API all YAML files are generated automatically.
During development, you only interact with type-safe Java API and set the necessary attributes:

```java
public class CustomDashboard implements Dashboard {

    @Override
    public void configure(Header header, Layout layout) {

        header.title = "My Title";
        layout.row("overview")
                .el(PieChart.class, (viz, data) -> {
                    viz.title = "Stuck Agents per Transport Mode";
                    viz.dataset = data.compute(StuckAgentAnalysis.class, "stuckAgentsPerModePieChart.csv");
                });
    }
}
```

The package `org.matsim.simwrapper.viz` contains all the viz elements available in SimWrapper as Java API.

## Adding Dashboards

The philosophy of this module is to provide as much functionality as possible without any need for configuration.


### 1. Guice Binding

If SimWrapper is used within MATSim, the easiest way to add dashboards is to use Guice modules add them as a binding like so:

```java

import org.matsim.simwrapper.SimWrapper;

public class MyModule extends AbstractModule {

    @Override
    protected void configure() {
        // Use utility method, which just uses multi-binder internally
        SimWrapper.addDashboardBinding(binder()).toInstance(new CustomDashboard());
    }
}

```

This method works the best for you own scenarios, as long as you ensure that the needed modules are installed.
The next method provides a way to add dashboards even without Guice modules and does not require the developer to add modules to its scenario.

### 2. Java Service Provider Interface

To define which dashboard are available you can implement a [DashboardProvider](src%2Fmain%2Fjava%2Forg%2Fmatsim%2Fsimwrapper%2FDashboardProvider.java), which simply returns a list of dashboards.

A Java Service Provider Interface (SPI) allows to pickup implementation of classes, as soon as they are on the
classpath.
Thus, dashboards added this way will be available as soon the maven dependency is imported. No other configuration is
required from the users side.

To use this method you need a file `META-INF/services/org.matsim.simwrapper.DashboardProvider`, which lists all your
provider implementations.

### 3. Package Scanning

If dashboards should not be added automatically without any configuration, then you still need to implement the
provider, but not add it to the services file.

The developer then needs to
configure [SimWrapperConfigGroup](src%2Fmain%2Fjava%2Forg%2Fmatsim%2Fsimwrapper%2FSimWrapperConfigGroup.java) and add
the corresponding package name to `packages` in the config.

## Post-processing

This module relies on the MATSim application contrib to integrate classes needed for post-processing.

A class that depends on the output of a run and is supposed to produce files to be shown on the dashboard needs to be
written  as `MATSimAppCommand` class.
Additionally, it needs to declare the files it depends on and produces via the`@CommandSpec` annotation.
Here is an example that shows the structure of such a class:

```java

@CommandLine.Command(name = "trips", description = "Calculates various trip related metrics.")
@CommandSpec(
        requires = {"trips.csv", "persons.csv"},
        produces = {"mode_share.csv", "mode_share_per_dist.csv", "mode_users.csv", "trip_stats.csv", "population_trip_stats.csv", "trip_purposes_by_hour.csv"}
)
public class TripAnalysis implements MATSimAppCommand {

    @CommandLine.Mixin
    private InputOptions input = InputOptions.ofCommand(TripAnalysis.class);
    @CommandLine.Mixin
    private OutputOptions output = OutputOptions.ofCommand(TripAnalysis.class);

    @Override
    public Integer call() throws Exception {
        Path p = input.getPath("trips.csv");
        // .. Analysis codes that generates the required csv ...
        Path out = output.getPath("trip_purposes_by_hour.csv");
    }
}
```

In a dashboard, the output of this analysis can be referenced using `data.compute(TripAnalysis.class, "trip_purposes_by_hour.csv")`.
The contrib will collect all required analysis classes, no further configuration for the post-processing is necessary.

For detailed examples, please refer to existing dashboards in `org.matsim.simwrapper.dashboard`.
