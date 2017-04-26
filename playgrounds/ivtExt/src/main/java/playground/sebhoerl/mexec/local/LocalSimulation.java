package playground.sebhoerl.mexec.local;

import org.apache.commons.io.FileUtils;
import playground.polettif.publicTransitMapping.workbench.Run;
import playground.sebhoerl.mexec.Config;
import playground.sebhoerl.mexec.ConfigUtils;
import playground.sebhoerl.mexec.Simulation;
import playground.sebhoerl.mexec.data.ControllerData;
import playground.sebhoerl.mexec.generic.AbstractSimulation;
import playground.sebhoerl.mexec.local.data.LocalSimulationData;
import playground.sebhoerl.mexec.local.os.OSDriver;
import playground.sebhoerl.mexec.placeholders.PlaceholderUtils;

import java.io.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class LocalSimulation extends AbstractSimulation<LocalSimulationData> implements Simulation {
    final private LocalEnvironment environment;

    final private File path;
    final private File scenarioPath;
    final private File controllerPath;
    final private File outputPath;

    final private ControllerData controllerData;

    final private OSDriver driver;

    private Config config = null;

    public LocalSimulation(LocalEnvironment environment, LocalSimulationData data, File path, File scenarioPath, File controllerPath, ControllerData controllerData, OSDriver driver) {
        super(data);

        this.environment = environment;
        this.path = path;
        this.scenarioPath = scenarioPath;
        this.controllerPath = controllerPath;
        this.controllerData = controllerData;
        this.outputPath = new File(path, "output");
        this.driver = driver;
    }

    @Override
    public void save() {
        if (config != null) {
            ConfigUtils.saveConfig(new File(path, "config.xml"), config);
        }

        environment.save();
    }

    @Override
    public Config getConfig() {
        if (config == null) {
            File source = new File(path, "config.xml");
            config = ConfigUtils.loadConfig(source);
        }

        return config;
    }

    @Override
    public boolean isActive() {
        if (data.pid == null) return false;
        return driver.isProcessActive(data.pid);
    }

    @Override
    public void start() {
        if (isActive()) {
            throw new RuntimeException("Cannot start an active simulation.");
        }

        reset();

        File pidPath = new File(path, "matsim.pid");
        File runScriptPath = new File(path, "run.sh");
        File outputLogPath = new File(path, "o.log");
        File errorLogPath = new File(path, "e.log");

        // Configuration creation
        Map<String, String> placeholders = new HashMap<>();
        placeholders.putAll(environment.getScenario(data.scenarioId).getPlaceholders());
        placeholders.putAll(getPlaceholders());

        placeholders.put("scenario", scenarioPath.toString());
        placeholders.put("output", outputPath.toString());

        Config config = ConfigUtils.loadConfig(new File(path, "config.xml"));
        Config transformed = PlaceholderUtils.transformConfig(config, placeholders);
        ConfigUtils.saveConfig(new File(path, "run_config.xml"), transformed);

        data.pid = driver.startProcess(data, path, controllerData, controllerPath, outputLogPath, errorLogPath);
        save();
    }

    @Override
    public void stop() {
        if (!isActive()) {
            throw new RuntimeException("Cannot start an inactive simulation.");
        }

        driver.stopProcess(data.pid);

        while (isActive()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}
        }
    }

    @Override
    public void reset() {
        if (isActive()) {
            throw new RuntimeException("Cannot reset an active simulation.");
        }

        File outputLogPath = new File(path, "o.log");
        File errorLogPath = new File(path, "e.log");

        FileUtils.deleteQuietly(outputLogPath);
        FileUtils.deleteQuietly(errorLogPath);
        FileUtils.deleteQuietly(outputPath);
    }

    @Override
    public Long getIteration() {
        File scorestatsPath = new File(path, "output/scorestats.txt");

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(scorestatsPath)));

            String line;
            long iteration = 0;

            reader.readLine();
            while ((line = reader.readLine()) != null) {
                try {
                    iteration = Math.max(iteration, Long.parseLong(line.substring(0, line.indexOf('\t'))));
                } catch (NumberFormatException | StringIndexOutOfBoundsException e) {}
            }

            return iteration;
        } catch (FileNotFoundException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public InputStream getOutputFile(String suffix) {
        File source = new File(outputPath, suffix);

        try {
            return new FileInputStream(source);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Error file not found:" + source);
        }
    }

    @Override
    public String getOutputPath(String suffix) {
        return new File(outputPath, suffix).toString();
    }
}
