package playground.sebhoerl.remote_exec;

import java.util.Collection;

public interface RemoteScenario {
    String getId();

    void remove();

    Collection<? extends RemoteSimulation> getSimulations();

    void setConfig(String path);
    void update();

    String getPath(String suffix);
}
