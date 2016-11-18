package playground.sebhoerl.mexec.local;

import org.apache.xerces.jaxp.DocumentBuilderFactoryImpl;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import playground.sebhoerl.mexec.Config;
import playground.sebhoerl.mexec.ConfigUtils;
import playground.sebhoerl.mexec.Scenario;
import playground.sebhoerl.mexec.Simulation;
import playground.sebhoerl.mexec.data.ScenarioData;
import playground.sebhoerl.mexec.placeholders.PlaceholderUtils;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LocalScenario implements Scenario {
    final private LocalEnvironment environment;
    final private ScenarioData data;
    final private File path;

    public LocalScenario(LocalEnvironment environment, ScenarioData data, File path) {
        this.environment = environment;
        this.data = data;
        this.path = path;
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
    public Set<String> getAvailablePlaceholders() {
        File configPath = new File(path, data.mainConfigPath);
        return PlaceholderUtils.findPlaceholdersInConfig(ConfigUtils.loadConfig(configPath));
    }

    @Override
    public Map<String, String> getPlaceholders() {
        return data.placeholders;
    }

    @Override
    public void save() {
        environment.save();
    }
}
