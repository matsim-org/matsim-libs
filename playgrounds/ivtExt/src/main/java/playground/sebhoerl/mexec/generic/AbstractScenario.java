package playground.sebhoerl.mexec.generic;

import playground.sebhoerl.mexec.ConfigUtils;
import playground.sebhoerl.mexec.Scenario;
import playground.sebhoerl.mexec.data.ScenarioData;
import playground.sebhoerl.mexec.local.LocalEnvironment;
import playground.sebhoerl.mexec.placeholders.PlaceholderUtils;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractScenario<DataType extends ScenarioData> implements Scenario {
    final protected DataType data;

    public AbstractScenario(DataType data) {
        this.data = data;
    }

    @Override
    public String getId() {
        return data.id;
    }

    @Override
    public String getMainConfigPath() {
        return data.mainConfigPath;
    }

    @Override
    public void setMainConfigPath(String configPath) {
        data.mainConfigPath = configPath;
    }

    @Override
    public List<String> getAdditionalConfigFiles() {
        return data.additionalConfigFiles;
    }

    @Override
    public Map<String, String> getPlaceholders() {
        return data.placeholders;
    }
}
