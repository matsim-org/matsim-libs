# SimWrapper

Contrib to provide [SimWrapper](https://simwrapper.github.io/) integration.
SimWrapper offers a huge amount of visualization possibilities in the webbrowser and enables users to analyze,
evaluate and  disseminate MATSim runs visually.

This extension allows you to automatically create SimWrapper dashboards after runs have finished.

To use it, simply install the module:

```java
controler.addOverridingModule(new SimWrapperModule())
```

## Custom Dashboards

This module not only provides a set of default dashboards, but is designed to be extended with scenario specific
dashboards as needed.
The existing dashboards in `org.matsim.simwrapper.dashboard` serve as good examples to get started.

Internally SimWrapper works mostly with YAML files, but in the Java API all YAML files are generated automatically for
you.
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

## Adding Dashboards

The philosophy of this module is to provide as much functionality as possible without any need for configuration.

To define which dashboard are available you need to implement
a [DashboardProvider](src%2Fmain%2Fjava%2Forg%2Fmatsim%2Fsimwrapper%2FDashboardProvider.java), which simply returns a
list of  dashboards.

There are two preferred ways to add these providers by default:

### 1. Java Service Provider Interface

A Java Service Provider Interface (SPI) allows to pickup implementation of classes, as soon as they are on the
classpath.
Thus, dashboards added this way will be available as soon the maven dependency is imported. No other configuration is
required from the users side.

To use this method you need a file `META-INF/services/org.matsim.simwrapper.DashboardProvider`, which lists all your provider implementations.

### 2. Package Scanning

If dashboards should not be added automatically without any configuration, then you still need to implement the provider, but not add it to the services file.

The developer then needs to configure [SimWrapperConfigGroup](src%2Fmain%2Fjava%2Forg%2Fmatsim%2Fsimwrapper%2FSimWrapperConfigGroup.java) and add
the corresponding package name to `packages` in the config.
The contrib will then pick it up from there only if configured correctly.