package playground.sebhoerl.euler_opdyts;

import floetteroed.opdyts.trajectorysampling.TrajectorySampler;
import playground.sebhoerl.remote_exec.RemoteController;
import playground.sebhoerl.remote_exec.RemoteEnvironment;
import playground.sebhoerl.remote_exec.RemoteScenario;
import playground.sebhoerl.remote_exec.RemoteSimulation;

public class RemoteSimulationFactory {
    final private RemoteEnvironment environment;
    final private RemoteScenario scenario;
    final private RemoteController controller;
    final private String prefix;

    private int simulationIndex;
    private int totalNumberOfIterations;
    private int numberOfIterationsPerTransition;

    public RemoteSimulationFactory(RemoteEnvironment environment, RemoteScenario scenario, RemoteController controller, int totalNumberOfIterations, int numberOfIterationsPerTransition, String prefix) {
        this.environment = environment;
        this.scenario = scenario;
        this.controller = controller;
        this.prefix = prefix;
        this.simulationIndex = 0;
        this.totalNumberOfIterations = totalNumberOfIterations;
        this.numberOfIterationsPerTransition = numberOfIterationsPerTransition;
    }

    public int getTotalNumberOfIterations() {
        return totalNumberOfIterations;
    }

    public int getNumberOfIterationsPerTransition() {
        return numberOfIterationsPerTransition;
    }

    public RemoteSimulationFactory(RemoteEnvironment environment, RemoteScenario scenario, RemoteController controller, int totalNumberOfIterations, int numberOfIterationsPerTransition) {
        this(environment, scenario, controller, totalNumberOfIterations, numberOfIterationsPerTransition, "");

        if (totalNumberOfIterations % numberOfIterationsPerTransition != 0) {
            throw new IllegalArgumentException("Total number of iterations must be dividable by the number of iterations per transition");
        }
    }

    private String createNewId() {
        String simulationId = "";

        do {
            simulationIndex++;
            simulationId = prefix + "it_" + simulationIndex;
        } while (environment.hasSimulation(simulationId));

        return simulationId;
    }

    RemoteSimulation createSimulation(RemoteSimulation previous, RemoteDecisionVariable decisionVariable) {
        RemoteSimulation simulation = environment.createSimulation(createNewId(), scenario, controller);
        String population = previous == null ? scenario.getPath("population.xml.gz") : previous.getPath("output_plans.xml.gz");

        // Custom parameters
        simulation.getParameters().putAll(decisionVariable.getParameters());

        // Fixed parameters
        simulation.getParameters().put("opdyts_population", population);
        simulation.getParameters().put("opdyts_iterations", String.valueOf(totalNumberOfIterations));
        simulation.getParameters().put("opdyts_interval", String.valueOf(numberOfIterationsPerTransition));

        simulation.update();

        return simulation;
    }
}
