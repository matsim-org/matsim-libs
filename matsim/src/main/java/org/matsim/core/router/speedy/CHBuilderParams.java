/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** */
package org.matsim.core.router.speedy;

/**
 * Holds all tunable parameters for {@link CHBuilder}.
 * Created by {@link RoutingParameterTuner} from a {@link NetworkProfile}.
 *
 * @author Steffen Axer
 */
public record CHBuilderParams(
        int hopLimit,
        int deferredHopLimit,
        int settledLimit,
        int maxSettledLimit,
        int deferredMaxSettledLimit,
        int skipWitnessDegreeProduct,
        int prioHopLimit,
        int prioSettledLimit,
        int deferredPrioHopLimit,
        int deferredPrioSettledLimit,
        int cellReorderThreshold,
        int adaptiveContractionThreshold,
        int deferDegreeProduct,
        int reestimateSkipDegree,
        int reestimateInterval
) {}


