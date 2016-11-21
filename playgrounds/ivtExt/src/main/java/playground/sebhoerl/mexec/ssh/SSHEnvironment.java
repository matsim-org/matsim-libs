package playground.sebhoerl.mexec.ssh;

import com.fasterxml.jackson.databind.ObjectMapper;
import playground.sebhoerl.mexec.Controller;
import playground.sebhoerl.mexec.Environment;
import playground.sebhoerl.mexec.Scenario;
import playground.sebhoerl.mexec.Simulation;
import playground.sebhoerl.mexec.data.ControllerData;
import playground.sebhoerl.mexec.data.ScenarioData;
import playground.sebhoerl.mexec.ssh.data.SSHEnvironmentData;
import playground.sebhoerl.mexec.ssh.data.SSHSimulationData;
import playground.sebhoerl.mexec.ssh.utils.SSHFile;
import playground.sebhoerl.mexec.ssh.utils.SSHUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class SSHEnvironment implements Environment {
    private SSHFile environmentPath;
    final private SSHFile scenariosPath;
    final private SSHFile controllersPath;
    final private SSHFile simulationsPath;
    final private SSHFile registryPath;

    final private SSHUtils ssh;

    final private ObjectMapper mapper = new ObjectMapper();
    final private SSHEnvironmentData data;

    public SSHEnvironment(String environmentPath, SSHUtils ssh) {
        this.ssh = ssh;
        this.environmentPath = new SSHFile(environmentPath);

        if (ssh.exists(this.environmentPath)) {
            if (!ssh.isDirectory(this.environmentPath)) {
                throw new RuntimeException("Not a directory: " + environmentPath);
            }
        } else {
            if (!(ssh.mkdirs(this.environmentPath))) {
                throw new RuntimeException("Could not set up directory: " + environmentPath);
            }
        }

        this.environmentPath = ssh.getAbsoluteFile(this.environmentPath);

        scenariosPath = new SSHFile(this.environmentPath, "scenarios");
        controllersPath = new SSHFile(this.environmentPath, "controllers");
        simulationsPath = new SSHFile(this.environmentPath, "simulations");
        registryPath = new SSHFile(this.environmentPath, "registry.json");

        if (ssh.exists(registryPath)) {
            try {
                data = mapper.readValue(ssh.read(registryPath), SSHEnvironmentData.class);
            } catch (IOException e) {
                throw new RuntimeException("Could not load sftp registry file from " + registryPath);
            }
        } else {
            data = new SSHEnvironmentData();
        }
    }

    public void save() {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            mapper.writeValue(stream, data);
            ssh.write(registryPath, new ByteArrayInputStream(stream.toByteArray()));
        } catch (IOException e) {
            throw new RuntimeException("Could not save local registry file to " + registryPath);
        }
    }

    private SSHFile getControllerPath(String controllerId) {
        return new SSHFile(controllersPath, controllerId);
    }

    private SSHFile getScenarioPath(String scenarioId) {
        return new SSHFile(scenariosPath, scenarioId);
    }

    private SSHFile getSimulationPath(String simulationId) {
        return new SSHFile(simulationsPath, simulationId);
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

        SSHFile remoteTarget = getControllerPath(controllerId);
        ssh.mkdirs(remoteTarget);

        try {
            ssh.copyDirectory(localSource, remoteTarget);
        } catch (IOException e) {
            throw new RuntimeException("Error while copying from " + localSource);
        }

        this.data.controllers.put(controllerId, data);
        save();

        return new SSHController(this, data);
    }

    @Override
    public Controller getController(String controllerId) {
        if (!hasController(controllerId)) {
            throw new RuntimeException("Controller '" + controllerId + "' does not exist");
        }

        return new SSHController(this, data.controllers.get(controllerId));
    }

    @Override
    public Collection<? extends Controller> getControllers() {
        List<Controller> controllers = new LinkedList<>();

        for (ControllerData controllerData : data.controllers.values()) {
            controllers.add(new SSHController(this, controllerData));
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

        ssh.deleteQuietly(getControllerPath(controller.getId()));
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

        SSHFile remoteTarget = getScenarioPath(scenarioId);
        ssh.mkdirs(remoteTarget);

        try {
            ssh.copyDirectory(localSource, remoteTarget);
        } catch (IOException e) {
            throw new RuntimeException("Error while copying from " + localSource);
        }

        this.data.scenarios.put(scenarioId, data);
        save();

        return new SSHScenario(this, data, getScenarioPath(scenarioId), ssh);
    }

    @Override
    public Scenario getScenario(String scenarioId) {
        if (!hasScenario(scenarioId)) {
            throw new RuntimeException("Scenario '" + scenarioId + "' does not exist");
        }

        return new SSHScenario(this, data.scenarios.get(scenarioId), getScenarioPath(scenarioId), ssh);
    }

    @Override
    public Collection<? extends Scenario> getScenarios() {
        List<Scenario> scenarios = new LinkedList<>();

        for (ScenarioData scenarioData : data.scenarios.values()) {
            scenarios.add(new SSHScenario(this, scenarioData, getScenarioPath(scenarioData.id), ssh));
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

        ssh.deleteQuietly(getScenarioPath(scenario.getId()));
    }

    private SSHSimulation getSimulationHandle(String simulationId) {
        SSHSimulationData simulationData = data.simulations.get(simulationId);
        ControllerData controllerData = data.controllers.get(simulationData.controllerId);

        return new SSHSimulation(
                this,
                simulationData,
                getSimulationPath(simulationId),
                getScenarioPath(simulationData.scenarioId),
                getControllerPath(simulationData.controllerId),
                controllerData,
                ssh
        );
    }

    @Override
    public Simulation createSimulation(String simulationId, Scenario scenario, Controller controller) {
        if (hasSimulation(simulationId)) {
            throw new RuntimeException("Simulation '" + simulationId + "' already exists");
        }

        SSHSimulationData data = new SSHSimulationData();
        data.id = simulationId;
        data.controllerId = controller.getId();
        data.scenarioId = scenario.getId();

        SSHFile remoteTarget = getSimulationPath(simulationId);
        ssh.mkdirs(remoteTarget);

        SSHFile configSource = new SSHFile(getScenarioPath(scenario.getId()), scenario.getMainConfigPath());
        SSHFile configTarget = new SSHFile(remoteTarget, "config.xml");

        try {
            ssh.copyRemote(configSource, configTarget);
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

        for (SSHSimulationData simulationData : data.simulations.values()) {
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

        ssh.deleteQuietly(getSimulationPath(simulation.getId()));
    }

    @Override
    public Collection<? extends Simulation> getSimulations(Scenario scenario) {
        List<Simulation> simulations = new LinkedList<>();

        for (Simulation simulation : getSimulations()) {
            SSHSimulationData simulationData = data.simulations.get(simulation.getId());

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
            SSHSimulationData simulationData = data.simulations.get(simulation.getId());

            if (simulationData.controllerId == controller.getId()) {
                simulations.add(simulation);
            }
        }

        return simulations;
    }

    @Override
    public Controller getController(Simulation simulation) {
        SSHSimulationData simulationData = data.simulations.get(simulation.getId());
        return getController(simulationData.controllerId);
    }

    @Override
    public Scenario getScenario(Simulation simulation) {
        SSHSimulationData simulationData = data.simulations.get(simulation.getId());
        return getScenario(simulationData.scenarioId);
    }
}
