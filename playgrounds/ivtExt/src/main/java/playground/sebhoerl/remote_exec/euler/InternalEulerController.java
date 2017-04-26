package playground.sebhoerl.remote_exec.euler;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.*;

@JsonIgnoreProperties({"simulationInstances"})
public class InternalEulerController {
    @JacksonInject
    final private EulerInterface euler;
    final private String id;
    final private List<String> simulations = new LinkedList<>();
    final private String className;
    final private String classPath;

    public InternalEulerController(final EulerInterface euler, final String id, final String classPath, final String className) {
        this.id = id;
        this.euler = euler;
        this.className = className;
        this.classPath = classPath;
    }

    @JsonCreator
    private InternalEulerController() {
        this.id = "unassigned";
        this.euler = null;
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

        if (!euler.removeController(id)) {
            throw new RuntimeException("Error while removing controller " + id + " from Euler");
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
}
