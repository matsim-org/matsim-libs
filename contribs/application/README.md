
# Application Scenarios

This contrib allows building user-friendly MATSim scenarios, as well as preprocessing and analysis pipelines.


## Defining an Application

An application is defined by extending the `MATSimApplication` class and defining a `main` function as shown in this example:


    @CommandLine.Command(header = ":: Open City Scenario ::", version = "1.0")
    @MATSimApplication.Prepare({
        CreateNetworkFromSumo.class, CreateTransitScheduleFromGtfs.class, TrajectoryToPlans.class, GenerateShortDistanceTrips.class
    })
    @MATSimApplication.Analysis({
        AnalysisSummary.class, TravelTimeAnalysis.class
    })
    public class RunCityScenario extends MATSimApplication {
     
        public RunCityScenario() {
            // The default config to use when no other config is given by the user
            super("scenarios/input/city-v1.0-25pct.config.xml");
        }
    
        public static void main(String[] args) {
            MATSimApplication.run(RunCityScenario.class, args);
        }
    }

This class should also be set as the main class in the `maven` config in case an executable jar file is build.
It allows to run the scenario, as well as execute subcommand registered via
`@MATSimApplication.Prepare` and `@MATSimApplication.Analysis`.

This contrib is heavily streamline towards commandline usage, which works well together with other scripting languages,
tools like `make` and usage on hpc clusters.

Running the class with the `--help` option gives a nicely formatted overview of all available command and options.

### Sub commands

This contrib already contains often needed functionality as subcommands e.g. for population preprocessing, network creation, gtfs integration etc.
Own subcommands can be defined by implementing the `MATSimAppCommand` interface and its `call` method.

Commands are based on the [picocli](https://picocli.info/) library. The existing command are good examples to get started.
Please use the official documentation as reference on how to define options, or for some of the more advances techniques.