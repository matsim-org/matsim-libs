package playground.sebhoerl.remote_exec.euler;

import playground.sebhoerl.remote_exec.RemoteController;

import java.util.Collection;

public class EulerController implements RemoteController {
    final EulerInterface euler;
    final String id;

    public EulerController(final EulerInterface euler, String id) {
        this.euler = euler;
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getClassName() {
        return getInternal().getClassName();
    }

    @Override
    public String getClassPath() {
        return getInternal().getClassPath();
    }

    @Override
    public void remove() {
        getInternal().remove();
    }

    @Override
    public Collection<EulerSimulation> getSimulations() {
        return getInternal().getSimulationInstances();
    }

    private InternalEulerController getInternal() {
        InternalEulerController internal = euler.getControllers().get(id);

        if (internal == null) {
            throw new RuntimeException("Controller " + id + " does not exist anymore");
        }

        return internal;
    }
}
