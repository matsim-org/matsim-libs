package playground.sebhoerl.mexec.local.os;

import java.io.File;

import playground.sebhoerl.mexec.data.ControllerData;
import playground.sebhoerl.mexec.data.SimulationData;

public interface OSDriver {
    long startProcess(SimulationData simulation, File simulationPath, ControllerData controller, File controllerPath, File outputPath, File errorPath);
    boolean isProcessActive(long pid);
    void stopProcess(long pid);
}
