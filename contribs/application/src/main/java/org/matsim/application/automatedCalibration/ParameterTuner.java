package org.matsim.application.automatedCalibration;

import org.matsim.core.config.Config;

import java.util.Map;

public interface ParameterTuner {
    /**
     * Tune the parameter.
     *
     * @param errorMap Mode (1st level map key)-->  Average distance of distance group (2nd level map key) --> error (in normalized value)
     */
    void tune(Config config, Map<String, Map<Double, Double>> errorMap);
}
