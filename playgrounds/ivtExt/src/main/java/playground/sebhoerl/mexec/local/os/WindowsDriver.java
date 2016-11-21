package playground.sebhoerl.mexec.local.os;

import playground.sebhoerl.mexec.data.ControllerData;
import playground.sebhoerl.mexec.data.SimulationData;

import java.io.*;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class WindowsDriver implements OSDriver {

    // WINDOWS DRIVER IS UNTESTED!!!

    @Override
    public long startProcess(SimulationData simulation, File simulationPath, ControllerData controller, File controllerPath, File outputPath, File errorPath) {
        List<String> command = new LinkedList<>();
        command.add("java");
        if (simulation.memory != null) command.add("-Xmx" + simulation.memory);
        command.add("-cp");
        command.add(new File(controllerPath, controller.classPath).toString());
        command.add(controller.className);
        command.add(new File(simulationPath, "run_config.xml").toString());
        command.add(">");
        command.add(outputPath.toString());
        command.add("2>");
        command.add(errorPath.toString());
        command.add("&");
        String javaCommand = String.join(" ", command);

        File pidPath = new File(simulationPath, "matsim.pid");
        File runScriptPath = new File(simulationPath, "runscript.bat");

        String runScript = "cd " + simulationPath + "\nstart " + javaCommand + "\neexit";

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

        // PID can only be inferred by comparing before and after
        Set<Long> beforePIDList = getJavaPIDList();

        // Start.
        try {
            Runtime.getRuntime().exec("cmd /c " + runScriptPath);
        } catch (IOException e) {
            throw new RuntimeException("Error while running the run script.");
        }

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {}

        Set<Long> afterPIDList = getJavaPIDList();

        afterPIDList.removeAll(beforePIDList);

        if (afterPIDList.size() == 0) {
            throw new RuntimeException("Error while inferring PID (not process found).");
        } else if (afterPIDList.size() > 1) {
            throw new RuntimeException("Error while inferring PID (multiple PIDs found).");
        }

        return afterPIDList.iterator().next();
    }

    private Set<Long> getJavaPIDList() {
        // https://www.windows-commandline.com/tasklist-command/
        Set<Long> pids = new HashSet<>();

        try {
            Process process = Runtime.getRuntime().exec("tasklist java");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line = null;
            boolean content = false;

            while ((line = reader.readLine()) != null) {
                if (content) {
                    String[] parts = line.split("\\s+");

                    if (parts.length > 1) {
                        pids.add(Long.parseLong(parts[1]));
                    }
                }

                if (line.startsWith("=")) {
                    content = true;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error while running tasklist");
        }

        return pids;
    }

    @Override
    public boolean isProcessActive(long pid) {
        // http://stackoverflow.com/questions/2533984/java-checking-if-any-process-id-is-currently-running-on-windows

        try {
            Process process = Runtime.getRuntime().exec("tasklist /FI \"PID eq " + pid + "\"");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            boolean found = false;
            String line = null;

            while ((line = reader.readLine()) != null) {
                if (line.contains(String.valueOf(pid))) {
                    found = true;
                }
            }

            return found;
        } catch (IOException e) {}

        return false;
    }

    @Override
    public void stopProcess(long pid) {
        // http://stackoverflow.com/questions/4633678/how-to-kill-a-process-in-java-given-a-specific-pid

        try {
            Runtime.getRuntime().exec("taskkill /F /PID " + pid);
        } catch (IOException e) {}
    }
}
