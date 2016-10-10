package playground.sebhoerl.remote_exec;

import com.jcraft.jsch.JSchException;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

public interface RemoteEnvironment {
    RemoteScenario createScenario(String remoteId, String localPath);
    RemoteScenario getScenario(String scenarioId);

    Collection<? extends RemoteScenario> getScenarios();

    RemoteSimulation createSimulation(String simulationId, RemoteScenario scenario, RemoteController controller, Map<String, String> parameters);
    RemoteSimulation createSimulation(String simulationId, RemoteScenario scenario, RemoteController controller);
    RemoteSimulation getSimulation(String simulationId);

    Collection<? extends RemoteSimulation> getSimulations();

    RemoteController createController(String controllerId, String localPath, String classPath, String className);
    RemoteController getController(String controllerId);

    Collection<? extends RemoteController> getControllers();

    boolean hasScenario(String scenarioId);
    boolean hasSimulation(String simulationId);
    boolean hasController(String controllerId);
}
