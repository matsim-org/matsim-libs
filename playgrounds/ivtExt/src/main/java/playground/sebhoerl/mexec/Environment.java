package playground.sebhoerl.mexec;

import java.util.Collection;

public interface Environment {
    // Controllers
    Controller createController(String controllerId, String localPath, String classPath, String className);
    Controller getController(String controllerId);
    Collection<? extends Controller> getControllers();
    boolean hasController(String controllerId);
    void removeController(Controller controller);

    // Scenarios
    Scenario createScenario(String scenarioId, String localPath);
    Scenario getScenario(String scenarioId);
    Collection<? extends Scenario> getScenarios();
    boolean hasScenario(String scenarioId);
    void removeScenario(Scenario scenario);

    // Simulations
    Simulation createSimulation(String simulationId, Scenario scenario, Controller controller);
    Simulation getSimulation(String simulationId);
    Collection<? extends Simulation> getSimulations();
    boolean hasSimulation(String simulationId);
    void removeSimulation(Simulation simulation);

    // Connections
    Collection<? extends Simulation> getSimulations(Scenario scenario);
    Collection<? extends Simulation> getSimulations(Controller controller);

    Controller getController(Simulation simulation);
    Scenario getScenario(Simulation simulation);
}
