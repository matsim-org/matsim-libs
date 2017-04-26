package playground.sebhoerl.remote_exec.euler;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.matsim.core.api.experimental.events.EventsManager;
import playground.sebhoerl.remote_exec.RemoteSimulation;
import playground.sebhoerl.remote_exec.RemoteUtils;

import java.io.OutputStream;
import java.rmi.Remote;
import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties({"status"})
public class InternalEulerSimulation {
    @JacksonInject final private EulerInterface euler;
    final private String id;

    //private RemoteSimulation.Status status = RemoteSimulation.Status.IDLE;
    private String scenarioId;
    private String controllerId;

    private Map<String, String> parameters = new HashMap<>();

    private String memory = null;

    public InternalEulerSimulation(EulerInterface euler, String id, String scenarioId, String controllerId, Map<String, String> parameters) {
        this.euler = euler;
        this.id = id;
        this.scenarioId = scenarioId;
        this.parameters = parameters;
        this.controllerId = controllerId;
    }

    @JsonCreator
    private InternalEulerSimulation() {
        this.id = "unassigned";
        this.euler = null;
    }

    public String getId() {
        return id;
    }

    public String getScenarioId() {
        return scenarioId;
    }

    public String getControllerId() {
        return controllerId;
    }

    public String getMemory() {
        return memory;
    }

    public void setMemory(String memory) {
        this.memory = memory;
        update();
    }

    @JsonIgnore
    public RemoteSimulation.Status getStatus() {
        RemoteSimulation.Status status = euler.getSimulationStatus(scenarioId, id);

        if (status == null) {
            throw new RuntimeException("Error while getting status for simulation " + id);
        }

        return status;
    }

    public void remove() {
        if (RemoteUtils.isActive(getStatus())) {
            throw new RuntimeException("Cannot remove ACTIVE simulation " + id);
        }

        if (!euler.removeSimulation(scenarioId, id, controllerId)) {
            throw new RuntimeException("Could not remove simulation " + scenarioId + ":" + id + " from Euler");
        }
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public void update() {
        if (RemoteUtils.isActive(getStatus())) {
            throw new RuntimeException("Cannot update ACTIVE simulation " + id);
        }

        if (!euler.updateSimulation(scenarioId, id)) {
            throw new RuntimeException("Error while updating simulation " + id);
        }
    }

    public void reset() {
        if (RemoteUtils.isActive(getStatus())) {
            throw new RuntimeException("Cannot reset ACTIVE simulation " + id);
        }

        if (!euler.resetSimulation(scenarioId, id)) {
            throw new RuntimeException("Could not reset simulation " + id);
        }
    }

    public void start() {
        if (RemoteUtils.isActive(getStatus())) {
            throw new RuntimeException("Cannot start ACTIVE simulation " + id);
        }

        if (!euler.startSimulation(scenarioId, id, controllerId)) {
            throw new RuntimeException("Could not start simulation " + id);
        }
    }

    public void stop() {
        if (!RemoteUtils.isActive(getStatus())) {
            throw new RuntimeException("Cannot stop INACTIVE simulation " + id);
        }

        if (!euler.stopSimulation(scenarioId, id)) {
            throw new RuntimeException("Could not stop simulation " + id);
        }
    }

    @JsonIgnore
    public long getIteration() {
        long iteration = euler.getSimulationIteration(scenarioId, id);

        if (iteration == -1) {
            throw new RuntimeException("Error while getting iteration number for simulation " + id);
        }

        return iteration;
    }

    public void getOutputLog(OutputStream stream) {
        RemoteSimulation.Status status = getStatus();

        if (status == RemoteSimulation.Status.IDLE || status == RemoteSimulation.Status.PENDING) {
            throw new RuntimeException("Cannot get the log from pending simulation " + id);
        }

        if (!euler.getSimulationOutputLog(scenarioId, id, stream)) {
            throw new RuntimeException("Error while getting output log for " + id);
        }
    }

    public void getErrorLog(OutputStream stream) {
        RemoteSimulation.Status status = getStatus();

        if (status == RemoteSimulation.Status.IDLE || status == RemoteSimulation.Status.PENDING) {
            throw new RuntimeException("Cannot get the log from pending simulation " + id);
        }

        if (!euler.getSimulationErrorLog(scenarioId, id, stream)) {
            throw new RuntimeException("Error while getting error log for " + id);
        }
    }

    public void getEvents(EventsManager events, Integer iteration) {
        RemoteSimulation.Status status = getStatus();

        if (status == RemoteSimulation.Status.IDLE || status == RemoteSimulation.Status.PENDING) {
            throw new RuntimeException("Cannot get the events from pending simulation " + id);
        }

        if (iteration == null && status != RemoteSimulation.Status.DONE) {
            throw new RuntimeException("Cannot read final events from simulation " + id + " because it is not finsihed.");
        }

        if (iteration != null && iteration > getIteration()) {
            throw new RuntimeException("Cannot read final events from simulation " + id + " because iteration " + iteration + " is not reached yet.");
        }

        if (!euler.getSimulationEvents(id, events, iteration)) {
            throw new RuntimeException("Error while getting events for " + id);
        }
    }

    public void getFile(String path, OutputStream stream) {
        if (!euler.getSimulationFile(id, path, stream)) {
            throw new RuntimeException("Error while getting file for " + id);
        }
    }

    public String getPath(String suffix) {
        return euler.getSimulationPath(scenarioId, id, suffix);
    }
}
