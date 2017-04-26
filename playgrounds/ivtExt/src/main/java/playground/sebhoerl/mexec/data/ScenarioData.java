package playground.sebhoerl.mexec.data;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ScenarioData {
    public String id;
    public String mainConfigPath = "config.xml";
    public List<String> additionalConfigFiles = new LinkedList<>();
    public Map<String, String> placeholders = new HashMap<>();
}
