package org.matsim.contrib.drt.estimator;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.drt.analysis.DrtEventSequenceCollector;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalWaitTimesAnalyzer;
import org.matsim.contrib.drt.optimizer.insertion.DetourTimeEstimator;
import org.matsim.contrib.drt.routing.DrtRouteCreator;
import org.matsim.contrib.drt.run.DrtModeRoutingModule;
import org.matsim.contrib.dvrp.router.DvrpRoutingModule;
import org.matsim.modechoice.EstimatorContext;
import org.matsim.modechoice.ModeAvailability;
import org.matsim.modechoice.estimators.LegEstimator;

/**
 * Estimates drt trips based only daily averages. No spatial or temporal differentiation is taken into account for the estimate.
 * This estimator is suited for small scenarios with few vehicles and trips and consequently few data points.
 */
public class BasicDRTTripEstimator implements LegEstimator<ModeAvailability> {

	// TODO: these might be bound separately (per mode)


	DrtZonalSystem zones;
	DrtZonalWaitTimesAnalyzer analyzer;
	DrtEventSequenceCollector collector;

	DetourTimeEstimator insertion; // vielleicht

	DvrpRoutingModule.AccessEgressFacilityFinder finder;
	DrtRouteCreator creator;

	DrtModeRoutingModule module;

	@Override
	public double estimate(EstimatorContext context, String mode, Leg leg, ModeAvailability option) {
		return 0;
	}
}
