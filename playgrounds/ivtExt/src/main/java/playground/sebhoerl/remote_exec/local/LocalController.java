package playground.sebhoerl.remote_exec.local;

import playground.sebhoerl.remote_exec.RemoteController;
import playground.sebhoerl.remote_exec.euler.EulerInterface;
import playground.sebhoerl.remote_exec.euler.EulerSimulation;
import playground.sebhoerl.remote_exec.euler.InternalEulerController;

import java.util.Collection;

/**
 * Created by sebastian on 11/10/16.
 */
public class LocalController implements RemoteController {
    final LocalInterface local;
    final String id;

    public LocalController(final LocalInterface local, String id) {
        this.local = local;
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
    public Collection<LocalSimulation> getSimulations() {
        return getInternal().getSimulationInstances();
    }

    private InternalLocalController getInternal() {
        InternalLocalController internal = local.getControllers().get(id);

        if (internal == null) {
            throw new RuntimeException("Controller " + id + " does not exist anymore");
        }

        return internal;
    }
}
