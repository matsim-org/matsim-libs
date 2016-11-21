package playground.sebhoerl.mexec.local.os;

import playground.sebhoerl.mexec.data.ControllerData;
import playground.sebhoerl.mexec.data.SimulationData;

import java.io.*;
import java.util.LinkedList;
import java.util.List;

public interface OSDriver {
    long startProcess(SimulationData simulation, File simulationPath, ControllerData controller, File controllerPath, File outputPath, File errorPath);
    boolean isProcessActive(long pid);
    void stopProcess(long pid);
}
