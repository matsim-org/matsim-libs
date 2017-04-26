package playground.sebhoerl.remote_exec.euler;

import com.fasterxml.jackson.annotation.*;
import playground.sebhoerl.remote_exec.RemoteScenario;
import playground.sebhoerl.remote_exec.RemoteSimulation;

import java.util.*;

@JsonIgnoreProperties({"simulationInstances"})
public class InternalEulerScenario {
    @JacksonInject  final private EulerInterface euler;
    final private String id;
    final private LinkedList<String> simulations = new LinkedList<>();
    private String config = "config.xml";

    public InternalEulerScenario(final EulerInterface euler, final String id) {
        this.id = id;
        this.euler = euler;
    }

    @JsonCreator
    private InternalEulerScenario() {
        this.id = "unassigned";
        this.euler = null;
    }

    void addSimulation(String id) {
        simulations.add(id);
    }
    void removeSimulation(String id) {
        simulations.remove(id);
    }

    public String getId() {
        return id;
    }

    public String getConfig() { return config; }
    public void setConfig(String config) {
        this.config = config;
    }

    public void update() {
        if (!euler.updateScenario(id)) {
            throw new RuntimeException("Failed to update scenario " + id);
        }
    }

    public void remove() {
        if (simulations.size() > 0) {
            throw new RuntimeException("Cannot remove scenario " + id + " while simulations are attached");
        }

        if (!euler.removeScenario(id)) {
            throw new RuntimeException("Error while removing scenario " + id + " from Euler");
        }
    }

    public Collection<String> getSimulations() {
        return simulations;
    }

    public Collection<EulerSimulation> getSimulationInstances() {
        Set<EulerSimulation> simulations = new HashSet<>();

        for (String simulationId : this.simulations) {
            simulations.add(new EulerSimulation(euler, simulationId));
        }

        return simulations;
    }

    public String getPath(String suffix) {
        return euler.getScenarioPath(id, suffix);
    }
}
