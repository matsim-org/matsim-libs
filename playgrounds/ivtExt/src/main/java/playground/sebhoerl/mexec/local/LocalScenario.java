package playground.sebhoerl.mexec.local;

import org.apache.xerces.jaxp.DocumentBuilderFactoryImpl;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import playground.sebhoerl.mexec.Config;
import playground.sebhoerl.mexec.ConfigUtils;
import playground.sebhoerl.mexec.Scenario;
import playground.sebhoerl.mexec.Simulation;
import playground.sebhoerl.mexec.data.ScenarioData;
import playground.sebhoerl.mexec.generic.AbstractScenario;
import playground.sebhoerl.mexec.placeholders.PlaceholderUtils;

import java.io.*;
import java.util.Set;

public class LocalScenario extends AbstractScenario implements Scenario {
    final private LocalEnvironment environment;
    final private File path;

    public LocalScenario(LocalEnvironment environment, ScenarioData data, File path) {
        super(data);

        this.environment = environment;
        this.path = path;
    }

    @Override
    public Set<String> getAvailablePlaceholders() {
        File configPath = new File(path, data.mainConfigPath);
        return PlaceholderUtils.findPlaceholdersInConfig(ConfigUtils.loadConfig(configPath));
    }

    @Override
    public void save() {
        environment.save();
    }
}
