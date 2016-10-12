package playground.sebhoerl.remote_exec.local;

import playground.sebhoerl.remote_exec.RemoteScenario;

import java.util.Collection;

/**
 * Created by sebastian on 11/10/16.
 */
public class LocalScenario implements RemoteScenario {
    final LocalInterface local;
    final String id;

    public LocalScenario(final LocalInterface local, String id) {
        this.local = local;
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
    public Collection<LocalSimulation> getSimulations() {
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

    private InternalLocalScenario getInternal() {
        InternalLocalScenario internal = local.getScenarios().get(id);

        if (internal == null) {
            throw new RuntimeException("Scenario " + id + " does not exist anymore");
        }

        return internal;
    }
}
