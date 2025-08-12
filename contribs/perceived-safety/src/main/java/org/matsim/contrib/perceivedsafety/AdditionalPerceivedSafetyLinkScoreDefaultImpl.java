package org.matsim.contrib.perceivedsafety;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

import java.util.Random;

/**
 * Default Implementation of scoring algorithm for perceived safety. The old approach with class PerceivedSafetyScoring
 * was translated into this class.
 * @author simei94
 */
public class AdditionalPerceivedSafetyLinkScoreDefaultImpl implements AdditionalPerceivedSafetyLinkScore {
    private static final Logger log = LogManager.getLogger(AdditionalPerceivedSafetyLinkScoreDefaultImpl.class);

    private static final int INITIAL_VAR_PERCEIVED_SAFETY = 4;

    private final int inputPerceivedSafetyThreshold;

    // 0 if none of the modes below are provided, e.g PT
    private double betaPerceivedSafety = 0.;
    // same for the standard deviation
    private double sdPerceivedSafety = 0.;
    // while dmax is set to one
    private double dMax = 1.;

    Random generator = new Random(42);
    private final Vehicles vehicles;
    private final PerceivedSafetyConfigGroup perceivedSafetyConfigGroup;

    @Inject
    AdditionalPerceivedSafetyLinkScoreDefaultImpl(Scenario scenario) {
        PerceivedSafetyConfigGroup perceivedSafetyCfg = ConfigUtils.addOrGetModule(scenario.getConfig(), PerceivedSafetyConfigGroup.class);

        this.inputPerceivedSafetyThreshold = perceivedSafetyCfg.getInputPerceivedSafetyThresholdPerM();

        this.vehicles = scenario.getVehicles();
        this.perceivedSafetyConfigGroup = perceivedSafetyCfg;
    }

    @Override
    public double computeLinkBasedScore(Link link, Id<Vehicle> vehicleId) {
        if (vehicles.getVehicles().get(vehicleId) == null) {
            log.fatal("Vehicle with id {} on link {} is not linked to any vehicle of the scenario (null). This should never happen! Aborting!", vehicleId, link.getId());
            throw new NullPointerException();
        }

        String currentMode = vehicles.getVehicles().get(vehicleId).getType().getNetworkMode();

        if (perceivedSafetyConfigGroup.getModes().get(currentMode) == null) {
            log.fatal("Trying to compute perceived safety score for mode {}," +
                    "but there are no perceived safety parameters for this mode in {}. Aborting!", currentMode, perceivedSafetyConfigGroup.getName());
            throw new NullPointerException();
        }

        PerceivedSafetyConfigGroup.PerceivedSafetyModeParams params = perceivedSafetyConfigGroup.getModes().get(currentMode);

        // set mean beta_psafe, depends on the transport mode
        betaPerceivedSafety = params.getMarginalUtilityOfPerceivedSafetyPerM();
        // set sd beta_psafe, depends on the transport mode
        sdPerceivedSafety = params.getMarginalUtilityOfPerceivedSafetyPerMSd();
        // set dmax, depends on the transport mode
        dMax = params.getDMaxPerM();

        double distance = link.getLength();
        double perceivedSafetyValueOnLink = computePerceivedSafetyValueOnLink(link, currentMode, inputPerceivedSafetyThreshold);
        double distanceBasedPerceivedSafety = perceivedSafetyValueOnLink * distance;

        // in case you want to estimate the additional score based on the weighted mean
        if (dMax == 0) {
            dMax = distance;
        }

        //divide by dmax at the end
        distanceBasedPerceivedSafety = distanceBasedPerceivedSafety / dMax;
        // run Monte Carlo Simulation for the safety perceptions
        double r = generator.nextGaussian();
        // multiply with the random beta parameter
        return (betaPerceivedSafety + r * sdPerceivedSafety) * distanceBasedPerceivedSafety;
    }

    @Override
    public double computeTeleportationBasedScore(double distance, String currentMode) {
        PerceivedSafetyConfigGroup.PerceivedSafetyModeParams params = perceivedSafetyConfigGroup.getModes().get(currentMode);
        betaPerceivedSafety = params.getMarginalUtilityOfPerceivedSafetyPerM();
        sdPerceivedSafety = params.getMarginalUtilityOfPerceivedSafetyPerMSd();
        dMax = params.getDMaxPerM();

//        the current scoring of walk/teleported modes is not working. a list of links is retrieved from the leg, which is 0 for teleported legs.
//        thus, the perceived safety score of walk legs only consists of the last link, which is added separately to the list of links.
//        I think one needs to come up with an estimation for mode walk based on the travelled distance.

//        idea: do analysis of walk perceived safety and save it shp file. then retrieve value when walk leg is scored.

//        because of above issues: currently returns 0.
        return 0;
    }

    @Override
    public double computePerceivedSafetyValueOnLink(Link link, String mode, int threshold) {
//        initialize value in case there is no network attr
        int varPerceivedSafety = INITIAL_VAR_PERCEIVED_SAFETY;

        if (link.getAttributes().getAttribute(perceivedSafetyConfigGroup.getModes().get(mode).getNetworkAttributeName()) != null) {
            varPerceivedSafety = (int) link.getAttributes().getAttribute(perceivedSafetyConfigGroup.getModes().get(mode).getNetworkAttributeName());
        } else {
//            maybe this is too harsh. We also could just give back 0 as perceivedSafetyScore, but maybe with a log.info at the least.
//            For now, we will abort. -sm0825
            log.fatal("Link {} has no perceived safety attribute {}! The perceived safety score for mode {} cannot be calculated! Aborting!", link.getId(),
                    perceivedSafetyConfigGroup.getModes().get(mode).getNetworkAttributeName(),mode);
            throw new NullPointerException();
        }
        return (varPerceivedSafety - threshold);
    }
}
