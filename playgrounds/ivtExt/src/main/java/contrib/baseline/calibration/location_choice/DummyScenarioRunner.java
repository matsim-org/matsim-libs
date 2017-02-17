package contrib.baseline.calibration.location_choice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DummyScenarioRunner {
    public Map<String, Double> runScenario(String name, int iteration, int percentage, String[] purposes, List<Double> epsilons) {
        Map<String, Double> response = new HashMap<>();

        response.put("remote_work", 10.0 + epsilons.get(0));
        response.put("leisure", 10.0 + epsilons.get(1));
        response.put("shop", 10.0 + epsilons.get(2));
        response.put("escort_kids", 10.0 + epsilons.get(3));
        response.put("escort_other", 10.0 + epsilons.get(4));

        return response;
    }
}
