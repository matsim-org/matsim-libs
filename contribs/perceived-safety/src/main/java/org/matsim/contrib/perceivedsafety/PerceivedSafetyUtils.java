package org.matsim.contrib.perceivedsafety;

import org.matsim.api.core.v01.TransportMode;

import java.util.Map;

/**
 * A utils class for the perceived safety contrib.
 * author: Simon Meinhardt - simei94
 */
public class PerceivedSafetyUtils {
    public static final String E_BIKE = "eBike";
    public static final String E_SCOOTER = "eScooter";

	private PerceivedSafetyUtils() {}

    public static void fillConfigWithPerceivedSafetyDefaultValues(PerceivedSafetyConfigGroup perceivedSafetyConfigGroup) {
        Map<String, Double> mode2MarginalUtilities = Map.of(TransportMode.car, 0.44, E_BIKE,0.84,
                E_SCOOTER,0.76, TransportMode.walk, 0.33);
        Map<String, Double> mode2MarginalUtilitiesSd = Map.of(TransportMode.car, 0.20, E_BIKE,0.22,
                E_SCOOTER,0.07, TransportMode.walk, 0.17);
        Map<String, Double> mode2DMax = Map.of(TransportMode.car, 0., E_BIKE, 0.,
                E_SCOOTER,0., TransportMode.walk, 0.);

        for (Map.Entry<String, Double> e: mode2MarginalUtilities.entrySet()) {
            PerceivedSafetyConfigGroup.PerceivedSafetyModeParams modeParams = perceivedSafetyConfigGroup.getOrCreatePerceivedSafetyModeParams(e.getKey());
            modeParams.setMarginalUtilityOfPerceivedSafetyPerM(e.getValue());
            modeParams.setMarginalUtilityOfPerceivedSafetyPerMSd(mode2MarginalUtilitiesSd.get(e.getKey()));
            modeParams.setDMaxPerM(mode2DMax.get(e.getKey()));
            perceivedSafetyConfigGroup.addModeParams(modeParams);
        }
        perceivedSafetyConfigGroup.setInputPerceivedSafetyThresholdPerM(4);
    }
}
