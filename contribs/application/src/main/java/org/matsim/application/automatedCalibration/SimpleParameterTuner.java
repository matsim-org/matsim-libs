package org.matsim.application.automatedCalibration;

import java.util.List;
import java.util.Map;

public class SimpleParameterTuner implements ParameterTuner{



    @Override
    public Map<String, List<Double>> recommendChanges(Map<String, Map<String, Double>> errorMap) {
        double maxCar = errorMap.get("car").values().stream().mapToDouble(e -> e).max().orElseThrow();



        return null;
    }
}
