package playground.sebhoerl.mexec.data;

import java.util.HashMap;
import java.util.Map;

public class EnvironmentData<ControllerDataType extends ControllerData, ScenarioDataType extends ScenarioData, SimulationDataType extends SimulationData> {
    public Map<String, ControllerDataType> controllers = new HashMap<>();
    public Map<String, ScenarioDataType> scenarios = new HashMap<>();
    public Map<String, SimulationDataType> simulations = new HashMap<>();
}
