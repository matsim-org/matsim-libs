package playground.sebhoerl.mexec.local.os;

import playground.sebhoerl.mexec.data.ControllerData;
import playground.sebhoerl.mexec.data.SimulationData;

import java.io.*;
import java.util.LinkedList;
import java.util.List;

public class LinuxDriver implements OSDriver {
    public long startProcess(SimulationData simulation, File simulationPath, ControllerData controller, File controllerPath, File outputPath, File errorPath) {
        List<String> command = new LinkedList<>();
        command.add("java");
        if (simulation.memory != null) command.add("-Xmx" + simulation.memory);
        command.add("-cp");
        command.add(new File(controllerPath, controller.classPath).toString());
        command.add(controller.className);
        command.add(new File(simulationPath, "run_config.xml").toString());
        command.add("1>");
        command.add(outputPath.toString());
        command.add("2>");
        command.add(errorPath.toString());
        command.add("&");
        String javaCommand = String.join(" ", command);

        File pidPath = new File(simulationPath, "matsim.pid");
        File runScriptPath = new File(simulationPath, "runscript.sh");

        String runScript = "cd " + simulationPath + "\n" + javaCommand + "\necho $! > " + pidPath;

        try {
            OutputStream outputStream = new FileOutputStream(runScriptPath);
            OutputStreamWriter streamWriter = new OutputStreamWriter(outputStream);
            streamWriter.write(runScript);
            streamWriter.flush();
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Error while creating the run file.");
        } catch (IOException e) {
            throw new RuntimeException("Error while writing the run file.");
        }

        // Start.
        try {
            Runtime.getRuntime().exec("sh " + runScriptPath);
        } catch (IOException e) {
            throw new RuntimeException("Error while running the run script.");
        }

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {}

        long pid = 0;

        try {
            InputStream inputStream = new FileInputStream(pidPath);
            BufferedReader streamReader = new BufferedReader(new InputStreamReader(inputStream));
            pid = Long.parseLong(streamReader.readLine());
            streamReader.close();
        } catch (FileNotFoundException e) {
            throw new RuntimeException("No PID file found.");
        } catch (IOException e) {
            throw new RuntimeException("Error while reading PID.");
        }

        return pid;
    }

    public boolean isProcessActive(long pid) {
        try {
            Process process = Runtime.getRuntime().exec("ps " + pid);
            process.waitFor();

            if (process.exitValue() == 0) {
                return true;
            }
        } catch (InterruptedException e) {
        } catch (IOException e) {}

        return false;
    }

    public void stopProcess(long pid) {
        try {
            Runtime.getRuntime().exec("kill " + pid);
        } catch (IOException e) {
            throw new RuntimeException("Error while killing process.");
        }
    }
}
