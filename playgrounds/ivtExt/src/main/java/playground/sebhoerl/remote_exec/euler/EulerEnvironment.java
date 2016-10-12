package playground.sebhoerl.remote_exec.euler;

import playground.sebhoerl.remote_exec.RemoteController;
import playground.sebhoerl.remote_exec.RemoteEnvironment;
import playground.sebhoerl.remote_exec.RemoteScenario;
import playground.sebhoerl.remote_exec.RemoteSimulation;

import java.util.*;

public class EulerEnvironment implements RemoteEnvironment {
    final private EulerInterface euler;

    public EulerEnvironment(EulerInterface euler) {
        this.euler = euler;
    }

    @Override
    public EulerScenario createScenario(String remoteId, String localPath) {
        if (euler.getScenarios().containsKey(remoteId)) {
            throw new IllegalArgumentException("Scenario " + remoteId + " already exists on Euler.");
        }

        if(euler.createScenario(remoteId, localPath)) {
            return new EulerScenario(euler, remoteId);
        }

        throw new RuntimeException("Scenario " + remoteId + " could not be created on Euler.");
    }

    @Override
    public EulerScenario getScenario(String scenarioId) {
        Map<String, InternalEulerScenario> scenarios = euler.getScenarios();

        if (scenarios.containsKey(scenarioId)) {
            return new EulerScenario(euler, scenarioId);
        }

        throw new IllegalArgumentException("Scenario " + scenarioId + " does not exist on Euler.");
    }

    @Override
    public Collection<EulerScenario> getScenarios() {
        Set<EulerScenario> scenarios = new HashSet<>();

        for (InternalEulerScenario internal : euler.getScenarios().values()) {
            scenarios.add(new EulerScenario(euler, internal.getId()));
        }

        return scenarios;
    }

    @Override
    public EulerSimulation createSimulation(String simulationId, RemoteScenario scenario, RemoteController controller) {
        return createSimulation(simulationId, scenario, controller, new HashMap<String,String>());
    }

    @Override
    public EulerSimulation createSimulation(String simulationId, RemoteScenario scenario, RemoteController controller, Map<String, String> parameters) {
        if (euler.getSimulations().containsKey(simulationId)) {
            throw new IllegalArgumentException("Simulation " + simulationId + " already exists on Euler.");
        }

        if (!euler.getScenarios().containsKey(scenario.getId())) {
            throw new IllegalArgumentException("Unknown scenario " + scenario.getId());
        }

        if (!euler.getControllers().containsKey(controller.getId())) {
            throw new IllegalArgumentException("Unknown controller " + scenario.getId());
        }

        if (!euler.createSimulation(scenario.getId(), simulationId, controller.getId(), parameters)) {
            throw new RuntimeException("Error while creating simulation " + simulationId);
        }

        return new EulerSimulation(euler, simulationId);
    }

    @Override
    public EulerSimulation getSimulation(String simulationId) {
        Map<String, InternalEulerSimulation> simulations = euler.getSimulations();

        if (simulations.containsKey(simulationId)) {
            return new EulerSimulation(euler, simulationId);
        }

        throw new IllegalArgumentException("Scenario " + simulationId + " does not exist on Euler.");
    }

    @Override
    public Collection<EulerSimulation> getSimulations() {
        Set<EulerSimulation> simulations = new HashSet<>();

        for (InternalEulerSimulation internal : euler.getSimulations().values()) {
            simulations.add(new EulerSimulation(euler, internal.getId()));
        }

        return simulations;
    }

    @Override
    public EulerController createController(String controllerId, String localPath, String classPath, String className) {
        if (euler.getControllers().containsKey(controllerId)) {
            throw new IllegalArgumentException("Controller " + controllerId + " already exists on Euler.");
        }

        if(euler.createController(controllerId, localPath, classPath, className)) {
            return new EulerController(euler, controllerId);
        }

        throw new RuntimeException("Controller " + controllerId + " could not be created on Euler.");
    }

    @Override
    public EulerController getController(String controllerId) {
        Map<String, InternalEulerController> controllers = euler.getControllers();

        if (controllers.containsKey(controllerId)) {
            return new EulerController(euler, controllerId);
        }

        throw new IllegalArgumentException("Controller " + controllerId + " does not exist on Euler.");
    }

    @Override
    public Collection<EulerController> getControllers() {
        Set<EulerController> controllers = new HashSet<>();

        for (InternalEulerController internal : euler.getControllers().values()) {
            controllers.add(new EulerController(euler, internal.getId()));
        }

        return controllers;
    }

    @Override
    public boolean hasScenario(String scenarioId) {
        return euler.getScenarios().containsKey(scenarioId);
    }

    @Override
    public boolean hasSimulation(String simulationId) {
        return euler.getSimulations().containsKey(simulationId);
    }

    @Override
    public boolean hasController(String controllerId) {
        return euler.getControllers().containsKey(controllerId);
    }
}
