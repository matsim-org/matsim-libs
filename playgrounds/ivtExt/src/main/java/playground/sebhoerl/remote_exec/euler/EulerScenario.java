package playground.sebhoerl.remote_exec.euler;

import playground.sebhoerl.remote_exec.RemoteScenario;
import playground.sebhoerl.remote_exec.RemoteSimulation;

import java.util.Collection;

public class EulerScenario implements RemoteScenario {
    final EulerInterface euler;
    final String id;

    public EulerScenario(final EulerInterface euler, String id) {
        this.euler = euler;
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void remove() {
        getInternal().remove();
    }

    @Override
    public Collection<EulerSimulation> getSimulations() {
        return getInternal().getSimulationInstances();
    }

    @Override
    public void setConfig(String config) {
        getInternal().setConfig(config);
        getInternal().update();
    }

    @Override
    public void update() {
        getInternal().update();
    }

    @Override
    public String getPath(String suffix) {
        return getInternal().getPath(suffix);
    }

    private InternalEulerScenario getInternal() {
        InternalEulerScenario internal = euler.getScenarios().get(id);

        if (internal == null) {
            throw new RuntimeException("Scenario " + id + " does not exist anymore");
        }

        return internal;
    }
}
