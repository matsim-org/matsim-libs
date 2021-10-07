package org.matsim.application.automatedCalibration;

import java.util.List;
import java.util.Map;

public interface ParameterTuner {
    Map<String, List<Double>> recommendChanges(Map<String, Map<String, Double>> errorMap);
}
