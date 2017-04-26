package playground.sebhoerl.mexec.local;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import playground.sebhoerl.mexec.Controller;
import playground.sebhoerl.mexec.Environment;
import playground.sebhoerl.mexec.Scenario;
import playground.sebhoerl.mexec.Simulation;
import playground.sebhoerl.mexec.data.ControllerData;
import playground.sebhoerl.mexec.data.EnvironmentData;
import playground.sebhoerl.mexec.data.ScenarioData;
import playground.sebhoerl.mexec.data.SimulationData;
import playground.sebhoerl.mexec.local.data.LocalEnvironmentData;
import playground.sebhoerl.mexec.local.data.LocalSimulationData;
import playground.sebhoerl.mexec.local.os.OSDriver;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class LocalEnvironment implements Environment {
    final private File environmentPath;
    final private File scenariosPath;
    final private File controllersPath;
    final private File simulationsPath;
    final private File registryPath;

    final private ObjectMapper mapper = new ObjectMapper();
    final private LocalEnvironmentData data;

    final private OSDriver driver;

    public LocalEnvironment(String environmentPath, OSDriver driver) {
        this.environmentPath = new File(environmentPath).getAbsoluteFile();
        this.driver = driver;

        if (this.environmentPath.exists()) {
            if (!this.environmentPath.isDirectory()) {
                throw new RuntimeException("Not a directory: " + environmentPath);
            }
        } else {
            if (!this.environmentPath.mkdirs()) {
                throw new RuntimeException("Could not set up directory: " + environmentPath);
            }
        }

        scenariosPath = new File(this.environmentPath, "scenarios");
        controllersPath = new File(this.environmentPath, "controllers");
        simulationsPath = new File(this.environmentPath, "simulations");
        registryPath = new File(this.environmentPath, "registry.json");

        if (registryPath.exists()) {
            try {
                data = mapper.readValue(registryPath, LocalEnvironmentData.class);
            } catch (IOException e) {
                throw new RuntimeException("Could not load local registry file from " + registryPath);
            }
        } else {
            data = new LocalEnvironmentData();
        }
    }

    public void save() {
        try {
            mapper.writeValue(registryPath, data);
        } catch (IOException e) {
            throw new RuntimeException("Could not save local registry file to " + registryPath);
        }
    }

    private File getControllerPath(String controllerId) {
        return new File(controllersPath, controllerId);
    }

    private File getScenarioPath(String scenarioId) {
        return new File(scenariosPath, scenarioId);
    }

    private File getSimulationPath(String simulationId) {
        return new File(simulationsPath, simulationId);
    }

    @Override
    public Controller createController(String controllerId, String localPath, String classPath, String className) {
        File localSource = new File(localPath);

        if (!localSource.exists() || !localSource.isDirectory()) {
            throw new RuntimeException("Not an existing directory: " + localPath);
        }

        if (hasController(controllerId)) {
            throw new RuntimeException("Controller '" + controllerId + "' already exists");
        }

        ControllerData data = new ControllerData();
        data.className = className;
        data.classPath = classPath;
        data.id = controllerId;

        File localTarget = getControllerPath(controllerId);
        localTarget.mkdir();

        try {
            FileUtils.copyDirectory(localSource, localTarget);
        } catch (IOException e) {
            throw new RuntimeException("Error while copying from " + localSource);
        }

        this.data.controllers.put(controllerId, data);
        save();

        return new LocalController(this, data);
    }

    @Override
    public Controller getController(String controllerId) {
        if (!hasController(controllerId)) {
            throw new RuntimeException("Controller '" + controllerId + "' does not exist");
        }

        return new LocalController(this, data.controllers.get(controllerId));
    }

    @Override
    public Collection<? extends Controller> getControllers() {
        List<Controller> controllers = new LinkedList<>();

        for (ControllerData controllerData : data.controllers.values()) {
            controllers.add(new LocalController(this, controllerData));
        }

        return controllers;
    }

    @Override
    public boolean hasController(String controllerId) {
        return data.controllers.containsKey(controllerId);
    }

    @Override
    public void removeController(Controller controller) {
        for (Simulation simulation : getSimulations(controller)) {
            removeSimulation(simulation);
        }

        data.controllers.remove(controller.getId());
        save();

        FileUtils.deleteQuietly(getControllerPath(controller.getId()));
    }

    @Override
    public Scenario createScenario(String scenarioId, String localPath) {
        File localSource = new File(localPath);

        if (!localSource.exists() || !localSource.isDirectory()) {
            throw new RuntimeException("Not an existing directory: " + localPath);
        }

        if (hasScenario(scenarioId)) {
            throw new RuntimeException("Scenario '" + scenarioId + "' already exists");
        }

        ScenarioData data = new ScenarioData();
        data.id = scenarioId;

        File localTarget = getScenarioPath(scenarioId);
        localTarget.mkdir();

        try {
            FileUtils.copyDirectory(localSource, localTarget);
        } catch (IOException e) {
            throw new RuntimeException("Error while copying from " + localSource);
        }

        this.data.scenarios.put(scenarioId, data);
        save();

        return new LocalScenario(this, data, getScenarioPath(scenarioId));
    }

    @Override
    public Scenario getScenario(String scenarioId) {
        if (!hasScenario(scenarioId)) {
            throw new RuntimeException("Scenario '" + scenarioId + "' does not exist");
        }

        return new LocalScenario(this, data.scenarios.get(scenarioId), getScenarioPath(scenarioId));
    }

    @Override
    public Collection<? extends Scenario> getScenarios() {
        List<Scenario> scenarios = new LinkedList<>();

        for (ScenarioData scenarioData : data.scenarios.values()) {
            scenarios.add(new LocalScenario(this, scenarioData, getScenarioPath(scenarioData.id)));
        }

        return scenarios;
    }

    @Override
    public boolean hasScenario(String scenarioId) {
        return data.scenarios.containsKey(scenarioId);
    }

    @Override
    public void removeScenario(Scenario scenario) {
        for (Simulation simulation : getSimulations(scenario)) {
            removeSimulation(simulation);
        }

        data.scenarios.remove(scenario.getId());
        save();

        FileUtils.deleteQuietly(getScenarioPath(scenario.getId()));
    }

    private LocalSimulation getSimulationHandle(String simulationId) {
        LocalSimulationData simulationData = data.simulations.get(simulationId);
        ControllerData controllerData = data.controllers.get(simulationData.controllerId);

        return new LocalSimulation(
                this,
                simulationData,
                getSimulationPath(simulationId),
                getScenarioPath(simulationData.scenarioId),
                getControllerPath(simulationData.controllerId),
                controllerData,
                driver
        );
    }

    @Override
    public Simulation createSimulation(String simulationId, Scenario scenario, Controller controller) {
        if (hasSimulation(simulationId)) {
            throw new RuntimeException("Simulation '" + simulationId + "' already exists");
        }

        LocalSimulationData data = new LocalSimulationData();
        data.id = simulationId;
        data.controllerId = controller.getId();
        data.scenarioId = scenario.getId();

        File localTarget = getSimulationPath(simulationId);
        localTarget.mkdir();

        File configSource = new File(getScenarioPath(scenario.getId()), scenario.getMainConfigPath());
        File configTarget = new File(localTarget, "config.xml");

        try {
            FileUtils.copyFile(configSource, configTarget);
        } catch (IOException e) {
            throw new RuntimeException("Could not copy config " + configSource);
        }

        this.data.simulations.put(simulationId, data);
        save();

        return getSimulationHandle(simulationId);
    }

    @Override
    public Simulation getSimulation(String simulationId) {
        if (!hasSimulation(simulationId)) {
            throw new RuntimeException("Simulation '" + simulationId + "' does not exist");
        }

        return getSimulationHandle(simulationId);
    }

    @Override
    public Collection<? extends Simulation> getSimulations() {
        List<Simulation> simulations = new LinkedList<>();

        for (LocalSimulationData simulationData : data.simulations.values()) {
            simulations.add(getSimulationHandle(simulationData.id));
        }

        return simulations;
    }

    @Override
    public boolean hasSimulation(String simulationId) {
        return data.simulations.containsKey(simulationId);
    }

    @Override
    public void removeSimulation(Simulation simulation) {
        if (simulation.isActive()) {
            throw new RuntimeException("Cannot remove active simulation " + simulation.getId());
        }

        data.simulations.remove(simulation.getId());
        save();

        FileUtils.deleteQuietly(getSimulationPath(simulation.getId()));
    }

    @Override
    public Collection<? extends Simulation> getSimulations(Scenario scenario) {
        List<Simulation> simulations = new LinkedList<>();

        for (Simulation simulation : getSimulations()) {
            SimulationData simulationData = data.simulations.get(simulation.getId());

            if (simulationData.scenarioId == scenario.getId()) {
                simulations.add(simulation);
            }
        }

        return simulations;
    }

    @Override
    public Collection<? extends Simulation> getSimulations(Controller controller) {
        List<Simulation> simulations = new LinkedList<>();

        for (Simulation simulation : getSimulations()) {
            SimulationData simulationData = data.simulations.get(simulation.getId());

            if (simulationData.controllerId == controller.getId()) {
                simulations.add(simulation);
            }
        }

        return simulations;
    }

    @Override
    public Controller getController(Simulation simulation) {
        SimulationData simulationData = data.simulations.get(simulation.getId());
        return getController(simulationData.controllerId);
    }

    @Override
    public Scenario getScenario(Simulation simulation) {
        SimulationData simulationData = data.simulations.get(simulation.getId());
        return getScenario(simulationData.scenarioId);
    }
}
