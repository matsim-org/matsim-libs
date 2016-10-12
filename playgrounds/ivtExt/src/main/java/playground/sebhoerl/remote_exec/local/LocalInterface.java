package playground.sebhoerl.remote_exec.local;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.JSchException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.lang3.NotImplementedException;
import org.geotools.filter.NotImpl;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.MatsimEventsReader;
import playground.sebhoerl.remote_exec.RemoteSimulation;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class LocalInterface {
    final private ObjectMapper objectMapper = new ObjectMapper();

    final LocalConfiguration config;
    final String indexPath;

    static class LocalState {
        @JsonCreator
        public LocalState() {} // for json

        public Map<String, InternalLocalScenario> scenarios = new HashMap<>();
        public Map<String, InternalLocalSimulation> simulations = new HashMap<>();
        public Map<String, InternalLocalController> controllers = new HashMap<>();
    }

    LocalState state = new LocalState();

    public Map<String, InternalLocalScenario> getScenarios() {
        return state.scenarios;
    }
    public Map<String, InternalLocalSimulation> getSimulations() {
        return state.simulations;
    }
    public Map<String, InternalLocalController> getControllers() {
        return state.controllers;
    }

    public LocalInterface(LocalConfiguration config) throws IOException {
        this.config = config;

        if (!checkDirectoryStructure()) {
            if (config.getCreateDirectoryStructure()) {
                createDirectoryStructure();
            } else {
                throw new IllegalStateException("Directory structure is not initialized.");
            }
        }

        InjectableValues injectableValues = new InjectableValues.Std()
                .addValue(LocalInterface.class, this);
        objectMapper.setInjectableValues(injectableValues);

        indexPath = config.getScenarioPath() + "/index.json";

        readIndices();
    }

    private boolean checkDirectoryStructure() throws IOException {
        return checkPathExists(config.getScenarioPath()) && checkPathExists(config.getOutputPath());
    }

    private void createDirectoryStructure() throws IOException {
        RunResult scenarioResult = runCommand("mkdir -p " + config.getScenarioPath());
        RunResult outputResult = runCommand("mkdir -p " + config.getOutputPath());

        if (scenarioResult.exitStatus != 0) {
            throw new RuntimeException("Could not create " + config.getScenarioPath());
        }

        if (outputResult.exitStatus != 0) {
            throw new RuntimeException("Could not create " + config.getOutputPath());
        }
    }

    private boolean readingIndices = false;

    private void readIndices() throws IOException {
        readingIndices = true;

        if (checkPathExists(indexPath)) {
            state = objectMapper.readValue(readFile(indexPath), LocalInterface.LocalState.class);
        }

        readingIndices = false;
    }

    private void writeIndices() throws IOException {
        if (readingIndices) {
            throw new IllegalStateException("Trying to write indices while reading them.");
        }

        byte[] data = objectMapper.writeValueAsBytes(state);
        writeFile(indexPath, new ByteArrayInputStream(data), data.length);
    }

    public RemoteSimulation.Status getSimulationStatus(String scenarioId, String simulationId) {
        try {
            RunResult result = runCommand("ps " + state.simulations.get(simulationId).getPid());

            if (result.exitStatus == 0) {
                return RemoteSimulation.Status.RUNNING;
            }

            boolean logExists = checkPathExists(makeSimulationDirectory(scenarioId, simulationId) + "/o.log");
            boolean outputDirectoryExists = checkPathExists(makeOutputDirectory(simulationId));
            boolean outputEventsExists = checkPathExists(makeOutputDirectory(simulationId) + "/output_events.xml.gz");

            if (!logExists) {
                return RemoteSimulation.Status.IDLE;
            }

            if (outputEventsExists) {
                return RemoteSimulation.Status.DONE;
            }

            return RemoteSimulation.Status.ERROR;
        } catch (IOException e) {
            return null;
        }
    }

    public long getSimulationIteration(String scenarioId, String simulationId) {
        String path = makeOutputDirectory(simulationId) + "/scorestats.txt";

        try {
            if (checkPathExists(path)) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(readFile(path))));

                String line;
                long iteration = 0;

                reader.readLine();

                while ((line = reader.readLine()) != null) {
                    try {
                        iteration = Math.max(iteration, Long.parseLong(line.substring(0, line.indexOf('\t'))));
                    } catch (NumberFormatException | StringIndexOutOfBoundsException e) {}
                }

                return iteration;
            } else {
                return 0;
            }
        } catch (IOException e) {
            System.err.println(e.toString());
            return -1;
        }
    }

    public boolean getSimulationFile(String simulationId, String path, OutputStream stream) {
        try {
            readFileByStream(makeOutputDirectory(simulationId), stream);
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    public boolean getSimulationErrorLog(String scenarioId, String simulationId, OutputStream stream) {
        try {
            readFileByStream(makeSimulationDirectory(scenarioId, simulationId) + "/e.log", stream);
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    public boolean getSimulationOutputLog(String scenarioId, String simulationId, OutputStream stream) {
        try {
            readFileByStream(makeSimulationDirectory(scenarioId, simulationId) + "/o.log", stream);
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    public boolean getSimulationEvents(String simulationId, EventsManager events, Integer iteration) {
        String source;

        if (iteration == null) {
            source = makeOutputDirectory(simulationId) + "/output_events.xml.gz";
        } else {
            source = makeOutputDirectory(simulationId) + "/ITERS/it." + iteration + "/" + iteration + ".events.xml.gz";
        }

        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            readFileByStream(source, output);
            new MatsimEventsReader(events).readStream(new GZIPInputStream(new ByteArrayInputStream(output.toByteArray())));
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    public String getSimulationPath(String scenarioId, String simulationId, String suffix) {
        return makeOutputDirectory(simulationId) + (suffix == null ? "" : "/" + suffix);
    }

    public String getScenarioPath(String scenarioId, String suffix) {
        return makeScenarioDirectory(scenarioId) + (suffix == null ? "" : "/" + suffix);
    }

    public boolean startSimulation(String scenarioId, String simulationId, String controllerId) {
        InternalLocalScenario scenario = state.scenarios.get(scenarioId);
        InternalLocalController controller = state.controllers.get(controllerId);
        InternalLocalSimulation simulation = state.simulations.get(simulationId);

        resetSimulation(scenarioId, simulationId);

        try {
            writeSimulationConfig(
                    scenarioId,
                    simulationId,
                    transformConfigFile(
                            readScenarioConfig(scenarioId),
                            scenarioId,
                            simulationId,
                            state.simulations.get(simulationId).getParameters()
                    )
            );

            List<String> command = new LinkedList<>();
            command.add("java");
            if (simulation.getMemory() != null) command.add("-Xmx" + simulation.getMemory());
            command.add("-cp");
            command.add(makeControllerDirectory(controllerId) + "/" + controller.getClassPath());
            command.add(controller.getClassName());
            command.add(makeSimulationDirectory(scenarioId, simulationId) + "/config.xml");
            command.add("1>");
            command.add(makeSimulationDirectory(scenarioId, simulationId) + "/o.log");
            command.add("2>");
            command.add(makeSimulationDirectory(scenarioId, simulationId) + "/e.log");
            command.add("&");
            String javaCommand = String.join(" ", command);

            String pidPath = makeSimulationDirectory(scenarioId, simulationId) + "/matsim.pid";

            String matsimScript = "cd " + makeSimulationDirectory(scenarioId, simulationId) + "\n" + javaCommand + "\necho $! > " + pidPath;
            byte[] matsimScriptData = matsimScript.getBytes();

            writeFile(makeSimulationDirectory(scenarioId, simulationId) + "/run.sh",
                    new ByteArrayInputStream(matsimScriptData), matsimScriptData.length);

            RunResult result = runCommand("sh " + makeSimulationDirectory(scenarioId, simulationId) + "/run.sh");

            if (!checkPathExists(pidPath)) {
                System.err.println("No PID found.");
                return false;
            }

            long pid = Long.parseLong(new String(readFile(pidPath)).trim());
            simulation.setPid(pid);

            RemoteSimulation.Status status = getSimulationStatus(scenarioId, simulationId);

            if (status != RemoteSimulation.Status.PENDING && status != RemoteSimulation.Status.RUNNING) {
                System.err.println("Validation failed.");
                return false;
            }

            writeIndices();
        } catch (IOException e) {
            System.out.println(e.toString());
            return false;
        }

        return true;
    }

    public boolean stopSimulation(String scenarioId, String simulationId) {
        try {
            runCommand("kill " + state.simulations.get(simulationId).getPid());
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    public boolean resetSimulation(String scenarioId, String simulationId) {
        try {
            runCommand("rm -r " + makeSimulationDirectory(scenarioId, simulationId) + "/o.log");
            runCommand("rm -r " + makeSimulationDirectory(scenarioId, simulationId) + "/e.log");
            runCommand("rm -r " + makeOutputDirectory(simulationId));
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    public boolean updateSimulation(String scenarioId, String simulationId) {
        try {
            writeIndices();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    byte[] transformConfigFile(byte[] inputData, String scenarioId, String simulationId, Map<String, String> parameters) {
        Map<String, String> envParameters = new HashMap<>();

        envParameters.put("scenario", makeScenarioDirectory(scenarioId));
        envParameters.put("output", makeOutputDirectory(simulationId));
        envParameters.put("simulation", makeSimulationDirectory(scenarioId, simulationId));

        String inputString = new String(inputData);

        for (Map.Entry<String, String> entry : envParameters.entrySet()) {
            inputString = inputString.replaceAll("%" + entry.getKey(), entry.getValue());
        }

        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            inputString = inputString.replaceAll("%" + entry.getKey(), entry.getValue());
        }

        return inputString.getBytes();
    }

    private byte[] readScenarioConfig(String scenarioId) throws IOException {
        InternalLocalScenario scenario = state.scenarios.get(scenarioId);
        return readFile(makeScenarioDirectory(scenarioId) + "/" + scenario.getConfig());
    }

    private void writeSimulationConfig(String scenarioId, String simulationId, byte[] data) throws IOException {
        String target = makeSimulationDirectory(scenarioId, simulationId) + "/config.xml";
        writeFile(target, new ByteArrayInputStream(data), data.length);
    }

    public boolean createSimulation(String scenarioId, String simulationId, String controllerId, Map<String, String> parameters) {
        try {
            RunResult result = runCommand("mkdir -p " + makeSimulationDirectory(scenarioId, simulationId));

            if (result.exitStatus != 0) {
                return false;
            }

            state.simulations.put(simulationId, new InternalLocalSimulation(this, simulationId, scenarioId, controllerId, parameters));
            state.scenarios.get(scenarioId).addSimulation(simulationId);
            state.controllers.get(controllerId).addSimulation(simulationId);
            writeIndices();
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    public boolean removeSimulation(String scenarioId, String simulationId, String controllerId) {
        try {
            RunResult result = runCommand("rm -r " + makeSimulationDirectory(scenarioId, simulationId));

            if (result.exitStatus != 0) {
                return false;
            }

            state.simulations.remove(simulationId);
            state.scenarios.get(scenarioId).removeSimulation(simulationId);
            state.controllers.get(controllerId).removeSimulation(simulationId);
            writeIndices();
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    public boolean createController(String controllerId, String localPath, String classPath, String className) {
        try {
            pushDirectory(localPath, makeControllerDirectory(controllerId));
            state.controllers.put(controllerId, new InternalLocalController(this, controllerId, classPath, className));
            writeIndices();
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    public boolean removeController(String controllerId) {
        try {
            runCommand("rm -r " + makeControllerDirectory(controllerId));
            state.controllers.remove(controllerId);
            writeIndices();
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    public boolean createScenario(String remoteId, String localPath) {
        try {
            pushDirectory(localPath, makeScenarioDirectory(remoteId));
            state.scenarios.put(remoteId, new InternalLocalScenario(this, remoteId));
            writeIndices();
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    public boolean removeScenario(String scenarioId) {
        try {
            runCommand("rm -r " + makeScenarioDirectory(scenarioId));
            state.scenarios.remove(scenarioId);
            writeIndices();
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    public boolean updateScenario(String scenarioId) {
        try {
            writeIndices();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private class RunResult {
        final private int exitStatus;
        final private byte[] output;
        final private byte[]  errorOutput;

        public RunResult(int exitStatus, byte[] output, byte[] errorOutput) {
            this.exitStatus = exitStatus;
            this.output = output;
            this.errorOutput = errorOutput;
        }

        public int getExitStatus() {
            return exitStatus;
        }

        public byte[] getOutput() {
            return output;
        }

        public byte[] getErrorOutput() {
            return errorOutput;
        }
    }

    private RunResult runCommand(String command) throws IOException {
        ByteArrayOutputStream commandOutputStream = new ByteArrayOutputStream();
        ByteArrayOutputStream commandErrorStream = new ByteArrayOutputStream();

        ProcessBuilder builder = new ProcessBuilder();
        builder.command(command.split(" "));

        Process process = builder.start();

        while (process.isAlive()) {
            if (process.getErrorStream().available() > 0) {
                IOUtils.copy(process.getErrorStream(), commandErrorStream);
            }

            if (process.getInputStream().available() > 0) {
                IOUtils.copy(process.getInputStream(), commandOutputStream);
            }
        }

        return new RunResult(process.exitValue(), commandOutputStream.toByteArray(), commandErrorStream.toByteArray());
    }

    private boolean checkPathExists(String remotePath) throws IOException {
        return runCommand("ls " + remotePath).exitStatus == 0;
    }

    final static int BUFFER_SIZE = 1024;

    private void writeFile(String remotePath, InputStream contentStream, long filesize) throws IOException {
        if (remotePath.endsWith("/")) {
            throw new IllegalArgumentException();
        }

        IOUtils.copy(contentStream, new FileOutputStream(new File(remotePath)));
    }

    private byte[] readFile(String remotePath) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        readFileByStream(remotePath, buffer);
        return buffer.toByteArray();
    }

    private void readFileByStream(String remotePath, OutputStream output) throws IOException {
        IOUtils.copy(new FileInputStream(new File(remotePath)), output);
    }

    private void pushFile(String source, String target) throws IOException {
        System.out.println(source + " --> " + target);
        File fh = FileUtils.getFile(source);
        writeFile(target, new FileInputStream(fh), fh.length());

        if (!checkPathExists(target)) {
            throw new RuntimeException();
        }
    }

    private void pushDirectory(String localSourceDirectory, String remoteTargetDirectory) throws IOException {
        File root = FileUtils.getFile(localSourceDirectory);

        System.out.println(localSourceDirectory);
        Collection<File> hits = FileUtils.listFilesAndDirs(root, new RegexFileFilter("^(.*?)"), DirectoryFileFilter.DIRECTORY);

        List<String> directories = new LinkedList<>();
        List<String> files = new LinkedList<>();

        for (File file : hits) {
            if (file.getAbsolutePath().contains(".git")) {
                continue;
            }

            if (file.isDirectory()) {
                directories.add(file.getAbsolutePath());
            } else if (file.isFile()) {
                files.add(file.getAbsolutePath());
            }
        }

        List<String> validation = new LinkedList<>();

        for (String directory : directories) {
            System.out.println("Creating [target]" + directory.replace(root.getAbsolutePath(), ""));
            String remote = directory.replace(root.getAbsolutePath(), remoteTargetDirectory);
            runCommand("mkdir -p " + remote);
            validation.add(remote);
        }

        for (String file : files) {
            System.out.println("Sending [target]" + file.replace(root.getAbsolutePath(), ""));
            File fh = FileUtils.getFile(file);
            String remote = file.replace(root.getAbsolutePath(), remoteTargetDirectory);
            writeFile(remote, new FileInputStream(fh), fh.length());
            validation.add(remote);
        }

        for (String path : validation) {
            System.out.println("Validating [target]" + path.replace(remoteTargetDirectory, ""));

            if (!checkPathExists(path)) {
                throw new RuntimeException();
            }
        }

        System.out.println("Transmission successful.");
    }

    private String makeControllerDirectory(String controllerId) {
        return config.getScenarioPath() + "/controllers/" + controllerId;
    }

    private String makeScenarioDirectory(String scenarioId) {
        return config.getScenarioPath() + "/scenarios/" + scenarioId;
    }

    private String makeSimulationDirectory(String scenarioId, String simulationId) {
        return config.getOutputPath() + "/" + simulationId;
    }

    private String makeOutputDirectory(String simulationId) {
        return config.getOutputPath() + "/" + simulationId + "/output";
    }

    private String makeJobId(String scenarioId, String simulationId) {
        return simulationId;
    }
}
