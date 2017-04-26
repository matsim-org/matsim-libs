package playground.sebhoerl.remote_exec.local;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

@JsonIgnoreProperties({"simulationInstances"})
public class InternalLocalScenario {
    @JacksonInject
    final private LocalInterface local;
    final private String id;
    final private LinkedList<String> simulations = new LinkedList<>();
    private String config = "config.xml";

    public InternalLocalScenario(final LocalInterface local, final String id) {
        this.id = id;
        this.local = local;
    }

    @JsonCreator
    private InternalLocalScenario() {
        this.id = "unassigned";
        this.local = null;
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
        if (!local.updateScenario(id)) {
            throw new RuntimeException("Failed to update scenario " + id);
        }
    }

    public void remove() {
        if (simulations.size() > 0) {
            throw new RuntimeException("Cannot remove scenario " + id + " while simulations are attached");
        }

        if (!local.removeScenario(id)) {
            throw new RuntimeException("Error while removing scenario " + id + " from Euler");
        }
    }

    public Collection<String> getSimulations() {
        return simulations;
    }

    public Collection<LocalSimulation> getSimulationInstances() {
        Set<LocalSimulation> simulations = new HashSet<>();

        for (String simulationId : this.simulations) {
            simulations.add(new LocalSimulation(local, simulationId));
        }

        return simulations;
    }

    public String getPath(String suffix) {
        return local.getScenarioPath(id, suffix);
    }
}
