package org.matsim.contrib.drt.extension.estimator;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.dvrp.run.DvrpMode;
import org.matsim.core.scoring.functions.ModeUtilityParameters;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.modechoice.EstimatorContext;
import org.matsim.modechoice.ModeAvailability;
import org.matsim.modechoice.estimators.LegEstimator;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


/**
 * Aggregate class for informed-mode-choice that makes sure to invoke the correct estimator for each drt mode.
 */
public class MultiModalDrtLegEstimator implements LegEstimator<ModeAvailability> {

	private static final Logger log = LogManager.getLogger(MultiModalDrtLegEstimator.class);

	protected final Map<String, DrtEstimator> estimators = new HashMap<>();

	@Inject
	public MultiModalDrtLegEstimator(Map<DvrpMode, DrtEstimator> estimators) {
		for (Map.Entry<DvrpMode, DrtEstimator> e : estimators.entrySet()) {
			this.estimators.put(e.getKey().value(), e.getValue());
		}
	}

	@Override
	public double estimate(EstimatorContext context, String mode, Leg leg, ModeAvailability option) {

		if (!(leg.getRoute() instanceof DrtRoute route))
			throw new IllegalStateException("Drt leg routes must be of type DrtRoute.");

		OptionalTime departureTime = leg.getDepartureTime();

		DrtEstimator estimator = Objects.requireNonNull(estimators.get(mode), String.format("No drt estimator found for mode %s. Check log for errors.", mode));

		DrtEstimator.Estimate est = estimator.estimate(route, departureTime);
		ModeUtilityParameters params = context.scoring.modeParams.get(mode);

		// By default, waiting time is scored as travel time
		return params.constant +
			params.marginalUtilityOfDistance_m * est.distance() +
			params.marginalUtilityOfTraveling_s * est.travelTime() +
			params.marginalUtilityOfTraveling_s * est.waitingTime() +
			context.scoring.marginalUtilityOfMoney * params.monetaryDistanceCostRate * est.distance() +
			context.scoring.marginalUtilityOfMoney * est.fare();

	}
}
