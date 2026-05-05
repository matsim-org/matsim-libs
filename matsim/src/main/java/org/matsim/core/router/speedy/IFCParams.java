/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** */
package org.matsim.core.router.speedy;

/**
 * Holds all tunable parameters for {@link InertialFlowCutter}.
 * Created by {@link RoutingParameterTuner} from a {@link NetworkProfile}.
 *
 * @author Steffen Axer
 */
public record IFCParams(
        int fmMinSize,
        int fmMaxPasses,
        int maxflowMinSize,
        int maxflowBorderDepth,
        int parallelMinSize,
        int reducedDirectionsThreshold,
        int reducedRatiosThreshold
) {}

