package org.matsim.contrib.drt.optimizer.constraints;

/**
 * @author Sebastian Hörl, IRT SystemX
 */
public record DrtRouteConstraints( //
		double maxTravelTime, //
		double maxRideTime, //
		double maxWaitTime//
) {

}
