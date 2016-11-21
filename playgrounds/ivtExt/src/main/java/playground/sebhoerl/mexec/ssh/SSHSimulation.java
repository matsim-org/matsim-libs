package playground.sebhoerl.mexec.ssh;

import playground.sebhoerl.mexec.Config;
import playground.sebhoerl.mexec.ConfigUtils;
import playground.sebhoerl.mexec.Simulation;
import playground.sebhoerl.mexec.data.ControllerData;
import playground.sebhoerl.mexec.generic.AbstractSimulation;
import playground.sebhoerl.mexec.placeholders.PlaceholderUtils;
import playground.sebhoerl.mexec.ssh.data.SSHSimulationData;
import playground.sebhoerl.mexec.ssh.utils.SSHFile;
import playground.sebhoerl.mexec.ssh.utils.SSHUtils;

import java.io.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SSHSimulation extends AbstractSimulation<SSHSimulationData> implements Simulation {
    final private SSHEnvironment environment;

    final private SSHFile path;
    final private SSHFile scenarioPath;
    final private SSHFile controllerPath;
    final private SSHFile outputPath;

    final private ControllerData controllerData;

    final private SSHUtils ssh;

    private Config config = null;

    public SSHSimulation(SSHEnvironment environment, SSHSimulationData data, SSHFile path, SSHFile scenarioPath, SSHFile controllerPath, ControllerData controllerData, SSHUtils ssh) {
        super(data);

        this.environment = environment;
        this.path = path;
        this.scenarioPath = scenarioPath;
        this.controllerPath = controllerPath;
        this.controllerData = controllerData;
        this.outputPath = new SSHFile(path, "output");

        this.ssh = ssh;
    }

    @Override
    public void save() {
        if (config != null) {
            SSHFile configPath = new SSHFile(path, "config.xml");

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ConfigUtils.saveConfig(stream, config);

            try {
                ssh.write(configPath, new ByteArrayInputStream(stream.toByteArray()));
            } catch (IOException e) {
                throw new RuntimeException("Could not write config to " + configPath);
            }
        }

        environment.save();
    }

    @Override
    public void start() {
        if (isActive()) {
            throw new RuntimeException("Cannot start an active simulation.");
        }

        reset();

        SSHFile pidPath = new SSHFile(path, "matsim.pid");
        SSHFile runScriptPath = new SSHFile(path, "run.sh");

        // Configuration creation
        Map<String, String> placeholders = new HashMap<>();
        placeholders.putAll(environment.getScenario(data.scenarioId).getPlaceholders());
        placeholders.putAll(getPlaceholders());

        placeholders.put("scenario", scenarioPath.toString());
        placeholders.put("output", outputPath.toString());

        Config config = getConfig();
        Config transformed = PlaceholderUtils.transformConfig(config, placeholders);

        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ConfigUtils.saveConfig(stream, transformed);
            ssh.write(new SSHFile(path, "run_config.xml"), new ByteArrayInputStream(stream.toByteArray()));
        } catch (IOException e) {
            throw new RuntimeException("Error while creating run config");
        }

        // Build Java Command
        List<String> command = new LinkedList<>();
        command.add("java");
        if (getMemory() != null) command.add("-Xmx" + getMemory());
        command.add("-cp");
        command.add(new SSHFile(controllerPath, controllerData.classPath).toString());
        command.add(controllerData.className);
        command.add(new SSHFile(path, "run_config.xml").toString());
        command.add("1>");
        command.add(new SSHFile(path, "o.log").toString());
        command.add("2>");
        command.add(new SSHFile(path, "e.log").toString());
        command.add("&");
        String javaCommand = String.join(" ", command);

        // Build Run Script
        String runScript = "cd " + path + "\n" + javaCommand + "\necho $! > " + pidPath;

        try {
            ssh.write(runScriptPath, new ByteArrayInputStream(runScript.getBytes()));
        } catch (IOException e) {
            throw new RuntimeException("Error while writing the run file.");
        }

        // Start.
        try {
            ssh.execute("sh " + runScriptPath);
        } catch (IOException e) {
            throw new RuntimeException("Error while running the run script.");
        }

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {}

        try {
            BufferedReader streamReader = new BufferedReader(new InputStreamReader(ssh.read(pidPath)));
            data.pid = Long.parseLong(streamReader.readLine());
            streamReader.close();
        } catch (IOException e) {
            throw new RuntimeException("Error while reading PID.");
        }

        save();
    }

    @Override
    public void stop() {
        if (!isActive()) {
            throw new RuntimeException("Cannot start an inactive simulation.");
        }

        try {
            ssh.execute("kill " + data.pid);
        } catch (IOException e) {
            throw new RuntimeException("Error while killing process.");
        }
    }

    @Override
    public void reset() {
        if (isActive()) {
            throw new RuntimeException("Cannot reset an active simulation.");
        }

        SSHFile outputLogPath = new SSHFile(path, "o.log");
        SSHFile errorLogPath = new SSHFile(path, "e.log");

        ssh.deleteQuietly(outputLogPath);
        ssh.deleteQuietly(errorLogPath);
        ssh.deleteQuietly(outputPath);
    }

    @Override
    public boolean isActive() {
        try {
            SSHUtils.RunResult result = ssh.execute("ps " + data.pid);

            if (result.exitStatus == 0) {
                return true;
            }
        } catch (IOException e) {}

        return false;
    }

    @Override
    public Config getConfig() {
        if (config == null) {
            SSHFile source = new SSHFile(path, "config.xml");

            try {
                config = ConfigUtils.loadConfig(ssh.read(source));
            } catch (IOException e) {
                throw new RuntimeException("Could not read config file:" + source);
            }
        }

        return config;
    }

    @Override
    public Long getIteration() {
        SSHFile scorestatsPath = new SSHFile(path, "output/scorestats.txt");

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(ssh.read(scorestatsPath)));

            String line;
            long iteration = 0;

            reader.readLine();
            while ((line = reader.readLine()) != null) {
                try {
                    iteration = Math.max(iteration, Long.parseLong(line.substring(0, line.indexOf('\t'))));
                } catch (NumberFormatException | StringIndexOutOfBoundsException e) {}
            }

            return iteration;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public InputStream getOutputFile(String suffix) {
        SSHFile source = new SSHFile(outputPath, suffix);

        try {
            return ssh.read(source);
        } catch (IOException e) {
            throw new RuntimeException("Error file not found:" + source);
        }
    }

    @Override
    public String getOutputPath(String suffix) {
        return new SSHFile(outputPath, suffix).toString();
    }
}
