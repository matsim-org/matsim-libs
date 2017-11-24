package playground.lsieber.scenario.reducer;

import java.io.File;
import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import ch.ethz.idsc.queuey.datalys.MultiFileTools;
import playground.clruch.ScenarioOptions;
import playground.clruch.utils.PropertiesExt;

public abstract class IdscSettingsLoader {
    protected File workingDirectory;
    protected Config config;
    protected PropertiesExt simOptions;

    public IdscSettingsLoader() throws IOException {
        loadSettings();
    }

    private void loadSettings() throws IOException {
        workingDirectory = MultiFileTools.getWorkingDirectory();
        simOptions = PropertiesExt.wrap(ScenarioOptions.load(workingDirectory));
        File file = new File(workingDirectory, simOptions.getString("simuConfig"));
        config = ConfigUtils.loadConfig(file.toString());
    }

    public Scenario loadScenario() throws IOException {
        return ScenarioUtils.loadScenario(config);
    }

    public Config getConfig() {
        return config;
    }

    public PropertiesExt getSimOptions() {
        return simOptions;
    }

    public File getWorkingDirectory() {
        return workingDirectory;
    }
}
