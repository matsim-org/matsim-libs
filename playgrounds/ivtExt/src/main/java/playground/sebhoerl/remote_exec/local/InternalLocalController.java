package playground.sebhoerl.remote_exec.local;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.*;

@JsonIgnoreProperties({"simulationInstances"})
public class InternalLocalController {
    @JacksonInject
    final private LocalInterface local;
    final private String id;
    final private List<String> simulations = new LinkedList<>();
    final private String className;
    final private String classPath;

    public InternalLocalController(final LocalInterface local, final String id, final String classPath, final String className) {
        this.id = id;
        this.local = local;
        this.className = className;
        this.classPath = classPath;
    }

    @JsonCreator
    private InternalLocalController() {
        this.id = "unassigned";
        this.local = null;
        this.className = "unassigned";
        this.classPath = "unassigned";
    }

    public String getId() {
        return id;
    }

    void addSimulation(String id) {
        simulations.add(id);
    }
    void removeSimulation(String id) {
        simulations.remove(id);
    }

    public String getClassName() {
        return className;
    }
    public String getClassPath() { return classPath; }

    public void remove() {
        if (simulations.size() > 0) {
            throw new RuntimeException("Cannot remove controller " + id + " while simulations are attached");
        }

        if (!local.removeController(id)) {
            throw new RuntimeException("Error while removing controller " + id + " from Euler");
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
}
