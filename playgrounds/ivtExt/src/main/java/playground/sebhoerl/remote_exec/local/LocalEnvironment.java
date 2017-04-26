package playground.sebhoerl.remote_exec.local;

import playground.sebhoerl.remote_exec.RemoteController;
import playground.sebhoerl.remote_exec.RemoteEnvironment;
import playground.sebhoerl.remote_exec.RemoteScenario;

import java.util.*;

public class LocalEnvironment implements RemoteEnvironment {
    final private LocalInterface local;

    public LocalEnvironment(LocalInterface local) {
        this.local = local;
    }

    @Override
    public LocalScenario createScenario(String remoteId, String localPath) {
        if (local.getScenarios().containsKey(remoteId)) {
            throw new IllegalArgumentException("Scenario " + remoteId + " already exists on Euler.");
        }

        if(local.createScenario(remoteId, localPath)) {
            return new LocalScenario(local, remoteId);
        }

        throw new RuntimeException("Scenario " + remoteId + " could not be created on Euler.");
    }

    @Override
    public LocalScenario getScenario(String scenarioId) {
        Map<String, InternalLocalScenario> scenarios = local.getScenarios();

        if (scenarios.containsKey(scenarioId)) {
            return new LocalScenario(local, scenarioId);
        }

        throw new IllegalArgumentException("Scenario " + scenarioId + " does not exist on Euler.");
    }

    @Override
    public Collection<LocalScenario> getScenarios() {
        Set<LocalScenario> scenarios = new HashSet<>();

        for (InternalLocalScenario internal : local.getScenarios().values()) {
            scenarios.add(new LocalScenario(local, internal.getId()));
        }

        return scenarios;
    }

    @Override
    public LocalSimulation createSimulation(String simulationId, RemoteScenario scenario, RemoteController controller) {
        return createSimulation(simulationId, scenario, controller, new HashMap<String,String>());
    }

    @Override
    public LocalSimulation createSimulation(String simulationId, RemoteScenario scenario, RemoteController controller, Map<String, String> parameters) {
        if (local.getSimulations().containsKey(simulationId)) {
            throw new IllegalArgumentException("Simulation " + simulationId + " already exists on Euler.");
        }

        if (!local.getScenarios().containsKey(scenario.getId())) {
            throw new IllegalArgumentException("Unknown scenario " + scenario.getId());
        }

        if (!local.getControllers().containsKey(controller.getId())) {
            throw new IllegalArgumentException("Unknown controller " + scenario.getId());
        }

        if (!local.createSimulation(scenario.getId(), simulationId, controller.getId(), parameters)) {
            throw new RuntimeException("Error while creating simulation " + simulationId);
        }

        return new LocalSimulation(local, simulationId);
    }

    @Override
    public LocalSimulation getSimulation(String simulationId) {
        Map<String, InternalLocalSimulation> simulations = local.getSimulations();

        if (simulations.containsKey(simulationId)) {
            return new LocalSimulation(local, simulationId);
        }

        throw new IllegalArgumentException("Scenario " + simulationId + " does not exist on Euler.");
    }

    @Override
    public Collection<LocalSimulation> getSimulations() {
        Set<LocalSimulation> simulations = new HashSet<>();

        for (InternalLocalSimulation internal : local.getSimulations().values()) {
            simulations.add(new LocalSimulation(local, internal.getId()));
        }

        return simulations;
    }

    @Override
    public LocalController createController(String controllerId, String localPath, String classPath, String className) {
        if (local.getControllers().containsKey(controllerId)) {
            throw new IllegalArgumentException("Controller " + controllerId + " already exists on Euler.");
        }

        if(local.createController(controllerId, localPath, classPath, className)) {
            return new LocalController(local, controllerId);
        }

        throw new RuntimeException("Controller " + controllerId + " could not be created on Euler.");
    }

    @Override
    public LocalController getController(String controllerId) {
        Map<String, InternalLocalController> controllers = local.getControllers();

        if (controllers.containsKey(controllerId)) {
            return new LocalController(local, controllerId);
        }

        throw new IllegalArgumentException("Controller " + controllerId + " does not exist on Euler.");
    }

    @Override
    public Collection<LocalController> getControllers() {
        Set<LocalController> controllers = new HashSet<>();

        for (InternalLocalController internal : local.getControllers().values()) {
            controllers.add(new LocalController(local, internal.getId()));
        }

        return controllers;
    }

    @Override
    public boolean hasScenario(String scenarioId) {
        return local.getScenarios().containsKey(scenarioId);
    }

    @Override
    public boolean hasSimulation(String simulationId) {
        return local.getSimulations().containsKey(simulationId);
    }

    @Override
    public boolean hasController(String controllerId) {
        return local.getControllers().containsKey(controllerId);
    }
}
