package playground.sebhoerl.remote_exec;

import java.util.Collection;

public interface RemoteController {
    String getId();

    void remove();

    Collection<? extends RemoteSimulation> getSimulations();

    String getClassName();
    String getClassPath();
}
