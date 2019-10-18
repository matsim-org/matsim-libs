/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2018.
 */

package ch.sbb.matsim.routing.pt.raptor;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.router.MainModeIdentifier;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;

/**
 * @author mrieser / SBB
 */
public class IntermodalAwareRouterModeIdentifier implements MainModeIdentifier {

    private final Set<String> transitModes;

    @Inject
    public IntermodalAwareRouterModeIdentifier(Config config) {
        this.transitModes = config.transit().getTransitModes();
    }

    /** Intermodal trips can have a number of different legs and interaction activities, e.g.:
     * non_network_walk | bike-interaction | bike | pt-interaction | transit-walk | pt-interaction | train | pt-interaction | non_network_walk
     * Thus, this main mode identifier uses the following heuristic to decide to which router mode a trip belongs:
     * - if there is a leg with a pt mode (based on config.transit().getTransitModes(), it returns that pt mode.
     * - if there is only a leg with mode transit_walk, one of the configured transit modes is returned.
     * - otherwise, the first mode not being an non_network_walk or transit_walk.
     */
    @Override
    public String identifyMainMode(List<? extends PlanElement> tripElements) {
        String identifiedMode = null;
        for (PlanElement pe : tripElements) {
            if (pe instanceof Leg) {
                String mode = ((Leg) pe).getMode();
                if (transitModes.contains(mode)) {
                    return mode;
                }
                if (TransportMode.transit_walk.equals(mode)) {
                    identifiedMode = TransportMode.pt;
                }
                if (identifiedMode == null
                        && !TransportMode.non_network_walk.equals(mode)
                        && !TransportMode.transit_walk.equals(mode)) {
                    identifiedMode = mode;
                }
            }
        }

        if (identifiedMode != null) {
            return identifiedMode;
        }

        throw new RuntimeException("could not identify main mode: " + tripElements);
    }
}
