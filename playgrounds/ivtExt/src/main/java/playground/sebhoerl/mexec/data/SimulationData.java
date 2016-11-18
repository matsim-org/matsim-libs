package playground.sebhoerl.mexec.data;

import java.util.HashMap;
import java.util.Map;

public class SimulationData {
    public String id;
    public String controllerId;
    public String scenarioId;
    public Long memory = null;
    public Map<String, String> placeholders = new HashMap<>();
}
