package playground.lsieber.oldCode;

import java.io.File;
import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import ch.ethz.idsc.queuey.datalys.MultiFileTools;
import playground.clruch.options.ScenarioOptions;

@Deprecated // TODO this is not needed as ipmlemented in another class
public abstract class IdscSettingsLoader {
    protected File workingDirectory;
    protected Config config;
    protected ScenarioOptions simOptions;

    public IdscSettingsLoader() throws IOException {
        loadSettings();
    }

    private void loadSettings() throws IOException {
        workingDirectory = MultiFileTools.getWorkingDirectory();
        simOptions = ScenarioOptions.load(workingDirectory);
        File file = new File(workingDirectory, simOptions.getString("simuConfig"));
        config = ConfigUtils.loadConfig(file.toString());
    }

    public Scenario loadScenario() throws IOException {
        return ScenarioUtils.loadScenario(config);
    }

    public Config getConfig() {
        return config;
    }

    public ScenarioOptions getSimOptions() {
        return simOptions;
    }

    public File getWorkingDirectory() {
        return workingDirectory;
    }
}
