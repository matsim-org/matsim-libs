package playground.sebhoerl.remote_exec.euler;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.MatsimEventsReader;
import playground.sebhoerl.remote_exec.RemoteSimulation;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class EulerInterface  {
    final EulerConfiguration config;
    final Session session;

    final String indexPath;

    static class EulerState {
        @JsonCreator
        public EulerState() {} // for json

        public Map<String, InternalEulerScenario> scenarios = new HashMap<>();
        public Map<String, InternalEulerSimulation> simulations = new HashMap<>();
        public Map<String, InternalEulerController> controllers = new HashMap<>();
    }

    EulerState state = new EulerState();

    final private ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, InternalEulerScenario> getScenarios() {
        return state.scenarios;
    }
    public Map<String, InternalEulerSimulation> getSimulations() {
        return state.simulations;
    }
    public Map<String, InternalEulerController> getControllers() {
        return state.controllers;
    }

    public EulerInterface(Session session, EulerConfiguration config) throws JSchException, IOException {
        if (!session.isConnected()) {
            throw new RuntimeException("A connected session needs to be provided.");
        }

        this.config = config;
        this.session = session;

        if (!checkDirectoryStructure()) {
            if (config.getCreateDirectoryStructure()) {
                createDirectoryStructure();
            } else {
                throw new IllegalStateException("Directory structure is not initialized.");
            }
        }

        InjectableValues injectableValues = new InjectableValues.Std()
                .addValue(EulerInterface.class, this);
        objectMapper.setInjectableValues(injectableValues);

        indexPath = config.getScenarioPath() + "/index.json";

        readIndices();
    }

    private boolean checkDirectoryStructure() throws IOException, JSchException {
        return checkPathExists(config.getScenarioPath()) && checkPathExists(config.getOutputPath());
    }

    private void createDirectoryStructure() throws IOException, JSchException {
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

    private void readIndices() throws JSchException, IOException {
        readingIndices = true;

        if (checkPathExists(indexPath)) {
            state = objectMapper.readValue(readFile(indexPath), EulerState.class);
        }

        readingIndices = false;
    }

    private void writeIndices() throws IOException, JSchException {
        if (readingIndices) {
            throw new IllegalStateException("Trying to write indices while reading them.");
        }

        byte[] data = objectMapper.writeValueAsBytes(state);
        writeFile(indexPath, new ByteArrayInputStream(data), data.length);
    }

    public RemoteSimulation.Status getSimulationStatus(String scenarioId, String simulationId) {
        try {
            RunResult result = runCommand("bjobs -J " + makeJobId(scenarioId, simulationId));
            String status = new String(result.getOutput());

            if (status.contains("RUN")) {
                return RemoteSimulation.Status.RUNNING;
            } else if (status.contains("PEND")) {
                return RemoteSimulation.Status.PENDING;
            }

            result = runCommand("grep \"Successfully completed.\" " + makeSimulationDirectory(scenarioId, simulationId) + "/o.log");
            if (result.getExitStatus() == 0) {
                return RemoteSimulation.Status.DONE;
            }

            result = runCommand("grep \"job killed by owner.\" " + makeSimulationDirectory(scenarioId, simulationId) + "/o.log");
            if (result.getExitStatus() == 0) {
                return RemoteSimulation.Status.STOPPED;
            }

            result = runCommand("grep \"Exited with exit code\" " + makeSimulationDirectory(scenarioId, simulationId) + "/o.log");
            if (result.getExitStatus() == 0) {
                return RemoteSimulation.Status.ERROR;
            }

            return RemoteSimulation.Status.IDLE;
        } catch (JSchException | IOException e) {
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
        } catch (JSchException | IOException e) {
            System.err.println(e.toString());
            return -1;
        }
    }

    public boolean getSimulationFile(String simulationId, String path, OutputStream stream) {
        try {
            readFileByStream(makeOutputDirectory(simulationId), stream);
        } catch (JSchException | IOException e) {
            return false;
        }

        return true;
    }

    public boolean getSimulationErrorLog(String scenarioId, String simulationId, OutputStream stream) {
        try {
            readFileByStream(makeSimulationDirectory(scenarioId, simulationId) + "/e.log", stream);
        } catch (JSchException | IOException e) {
            return false;
        }

        return true;
    }

    public boolean getSimulationOutputLog(String scenarioId, String simulationId, OutputStream stream) {
        try {
            readFileByStream(makeSimulationDirectory(scenarioId, simulationId) + "/o.log", stream);
        } catch (JSchException | IOException e) {
            return false;
        }

        return true;
    }

    public boolean getSimulationEvents(String simulationId, EventsManager events, Integer iteration) {
        String source;

        if (iteration == null) {
            source = makeOutputDirectory(simulationId) + "/output_events.xml.gz";
        } else {
            source = makeOutputDirectory(simulationId) + "/ITERS/it." + iteration + "/events.xml.gz";
        }

        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            readFileByStream(source, output);
            new MatsimEventsReader(events).readStream(new GZIPInputStream(new ByteArrayInputStream(output.toByteArray())));
        } catch (JSchException | IOException e) {
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
        InternalEulerScenario scenario = state.scenarios.get(scenarioId);
        InternalEulerController controller = state.controllers.get(controllerId);
        InternalEulerSimulation simulation = state.simulations.get(simulationId);

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
            String javaCommand = String.join(" ", command);

            String matsimScript = "cd " + makeSimulationDirectory(scenarioId, simulationId) + "\nmodule load java\n" + javaCommand + "\n";
            byte[] matsimScriptData = matsimScript.getBytes();

            writeFile(makeSimulationDirectory(scenarioId, simulationId) + "/matsim.sh",
                    new ByteArrayInputStream(matsimScriptData), matsimScriptData.length);

            List<String> bsub = new LinkedList<>();
            bsub.add("bsub");
            bsub.add("-J");
            bsub.add(makeJobId(scenarioId, simulationId));
            bsub.add("-o");
            bsub.add(makeSimulationDirectory(scenarioId, simulationId) + "/o.log");
            bsub.add("-e");
            bsub.add(makeSimulationDirectory(scenarioId, simulationId) + "/e.log");
            bsub.add("<");
            bsub.add(makeSimulationDirectory(scenarioId, simulationId) + "/matsim.sh");


            List<String> runner = new LinkedList<>();
            runner.add("module load java");
            runner.add(String.join(" ", bsub));
            runner.add("");

            byte[] runnerFile = String.join("\n", runner).getBytes();

            writeFile(makeSimulationDirectory(scenarioId, simulationId) + "/run.sh",
                    new ByteArrayInputStream(runnerFile), runnerFile.length);

            RunResult result = runCommand("sh " + makeSimulationDirectory(scenarioId, simulationId) + "/run.sh");

            if (result.exitStatus != 0) {
                System.err.println("Run script failed.");
                return false;
            }

            RemoteSimulation.Status status = getSimulationStatus(scenarioId, simulationId);

            if (status != RemoteSimulation.Status.PENDING && status != RemoteSimulation.Status.RUNNING) {
                System.err.println("Validation failed.");
                return false;
            }
        } catch (JSchException | IOException e) {
            System.out.println(e.toString());
            return false;
        }

        return true;
    }

    public boolean resetSimulation(String scenarioId, String simulationId) {
        try {
            runCommand("rm -r " + makeSimulationDirectory(scenarioId, simulationId) + "/o.log");
            runCommand("rm -r " + makeSimulationDirectory(scenarioId, simulationId) + "/e.log");
            runCommand("rm -r " + makeOutputDirectory(simulationId));
        } catch (JSchException | IOException e) {
            return false;
        }

        return true;
    }

    public boolean stopSimulation(String scenarioId, String simulationId) {
        try {
            runCommand("bkill -J " + makeJobId(scenarioId, simulationId));
        } catch (JSchException | IOException e) {
            return false;
        }

        return true;
    }

    public boolean updateSimulation(String scenarioId, String simulationId) {
        try {
            writeIndices();
            return true;
        } catch (JSchException | IOException e) {
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

    private byte[] readScenarioConfig(String scenarioId) throws IOException, JSchException {
        InternalEulerScenario scenario = state.scenarios.get(scenarioId);
        return readFile(makeScenarioDirectory(scenarioId) + "/" + scenario.getConfig());
    }

    private void writeSimulationConfig(String scenarioId, String simulationId, byte[] data) throws IOException, JSchException {
        String target = makeSimulationDirectory(scenarioId, simulationId) + "/config.xml";
        writeFile(target, new ByteArrayInputStream(data), data.length);
    }

    public boolean createSimulation(String scenarioId, String simulationId, String controllerId, Map<String, String> parameters) {
        try {
            RunResult result = runCommand("mkdir -p " + makeSimulationDirectory(scenarioId, simulationId));

            if (result.exitStatus != 0) {
                return false;
            }

            state.simulations.put(simulationId, new InternalEulerSimulation(this, simulationId, scenarioId, controllerId, parameters));
            state.scenarios.get(scenarioId).addSimulation(simulationId);
            state.controllers.get(controllerId).addSimulation(simulationId);
            writeIndices();
        } catch (JSchException | IOException e) {
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
        } catch (JSchException | IOException e) {
            return false;
        }

        return true;
    }

    public boolean createController(String controllerId, String localPath, String classPath, String className) {
        try {
            pushDirectory(localPath, makeControllerDirectory(controllerId));
            state.controllers.put(controllerId, new InternalEulerController(this, controllerId, classPath, className));
            writeIndices();
        } catch (IOException | JSchException e) {
            return false;
        }

        return true;
    }

    public boolean removeController(String controllerId) {
        try {
            runCommand("rm -r " + makeControllerDirectory(controllerId));
            state.controllers.remove(controllerId);
            writeIndices();
        } catch (JSchException | IOException e) {
            return false;
        }

        return true;
    }

    public boolean createScenario(String remoteId, String localPath) {
        try {
            pushDirectory(localPath, makeScenarioDirectory(remoteId));
            state.scenarios.put(remoteId, new InternalEulerScenario(this, remoteId));
            writeIndices();
        } catch (IOException | JSchException e) {
            return false;
        }

        return true;
    }

    public boolean removeScenario(String scenarioId) {
        try {
            runCommand("rm -r " + makeScenarioDirectory(scenarioId));
            state.scenarios.remove(scenarioId);
            writeIndices();
        } catch (JSchException | IOException e) {
            return false;
        }

        return true;
    }

    public boolean updateScenario(String scenarioId) {
        try {
            writeIndices();
            return true;
        } catch (JSchException | IOException e) {
            return false;
        }
    }



    // ---- Handle SSH commands and SCP transmissions

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

    private RunResult runCommand(String command) throws IOException, JSchException {
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(command);

        ByteArrayOutputStream errorOutput = new ByteArrayOutputStream();
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        byte[] buffer = new byte[BUFFER_SIZE];
        int length;

        InputStream errorStream = channel.getErrStream();
        InputStream outputStream = channel.getInputStream();

        channel.connect();

        while (!channel.isClosed() || errorStream.available() > 0 || outputStream.available() > 0) {
            while (errorStream.available() > 0) {
                length = errorStream.read(buffer);
                errorOutput.write(buffer, 0, length);
            }

            while (outputStream.available() > 0) {
                length = outputStream.read(buffer);
                output.write(buffer, 0, length);
            }
        }

        channel.disconnect();

        return new RunResult(channel.getExitStatus(), output.toByteArray(), errorOutput.toByteArray());
    }

    private boolean checkPathExists(String remotePath) throws IOException, JSchException {
        return runCommand("ls " + remotePath).exitStatus == 0;
    }

    private boolean checkTransmission(InputStream input) throws IOException {
        int next = input.read();
        if (next <= 0) return true;
        throw new IOException();
    }

    final static int BUFFER_SIZE = 1024;

    private void writeFile(String remotePath, InputStream contentStream, long filesize) throws JSchException, IOException {
        if (remotePath.endsWith("/")) {
            throw new IllegalArgumentException();
        }

        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand("scp -t " + remotePath);

        OutputStream outputStream = channel.getOutputStream();
        InputStream inputStream = channel.getInputStream();

        channel.connect();

        String header = "C0644 " + filesize + " localfile\n";
        outputStream.write(header.getBytes());
        outputStream.flush();

        checkTransmission(inputStream);

        byte[] buffer = new byte[BUFFER_SIZE];
        int length;

        while (contentStream.available() > 0) {
            length = contentStream.read(buffer);
            outputStream.write(buffer, 0, length);
            outputStream.flush();
        }

        outputStream.write(0);
        outputStream.flush();

        checkTransmission(inputStream);
        outputStream.close();

        channel.disconnect();
    }

    private byte[] readFile(String remotePath) throws IOException, JSchException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        readFileByStream(remotePath, buffer);
        return buffer.toByteArray();
    }

    private void readFileByStream(String remotePath, OutputStream output) throws JSchException, IOException {
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand("scp -f " + remotePath);

        OutputStream outputStream = channel.getOutputStream();
        InputStream inputStream = channel.getInputStream();

        byte[] buffer = new byte[BUFFER_SIZE];
        int length;

        channel.connect();
        outputStream.write(0);
        outputStream.flush();

        inputStream.read(buffer, 0, 1); // read C
        if (buffer[0] != 'C') {
            throw new IOException();
        }

        inputStream.read(buffer, 0, 5); // read file permissions

        buffer = new byte[BUFFER_SIZE];
        length = 0;

        do { // read file size terminated by space
            inputStream.read(buffer, length, 1);
        } while (buffer[length++] != ' ');

        int filesize = Integer.parseInt(new String(buffer, 0, length - 1));
        outputStream.write(0);
        outputStream.flush();

        length = 0;
        do { // read file name terminated by newline
            inputStream.read(buffer, length, 1);
        } while (buffer[length++] != '\n');

        //String filename = new String(buffer, 0, length - 1);

        outputStream.write(0);
        outputStream.flush();

        while (filesize > 0 && !channel.isClosed()) { // read file
            while (inputStream.available() > 0) {
                length = inputStream.read(buffer);
                output.write(buffer, 0, buffer[length - 1] == '\0' ? (length - 1) : length);
                filesize -= length;
            }
        }

        if (filesize > 0 || inputStream.available() > 0) { // there is something left ...
            throw new IOException(remotePath);
        }

        outputStream.write(0);
        outputStream.flush();

        channel.disconnect();
    }

    private void pushFile(String source, String target) throws IOException, JSchException {
        System.out.println(source + " --> " + target);
        File fh = FileUtils.getFile(source);
        writeFile(target, new FileInputStream(fh), fh.length());

        if (!checkPathExists(target)) {
            throw new RuntimeException();
        }
    }

    private void pushDirectory(String localSourceDirectory, String remoteTargetDirectory) throws IOException, JSchException {
        File root = FileUtils.getFile(localSourceDirectory);

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
        return config.getJobPrefix() + simulationId;
    }
}
