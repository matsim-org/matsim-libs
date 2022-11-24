package org.matsim.contrib.drt.estimator;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.drt.estimator.run.DrtEstimatorConfigGroup;
import org.matsim.contrib.drt.estimator.run.MultiModeDrtEstimatorConfigGroup;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.dvrp.run.DvrpModes;
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

	private final Map<String, DrtEstimator> estimators = new HashMap<>();

	@Inject
	public MultiModalDrtLegEstimator(Injector injector) {

		MultiModeDrtEstimatorConfigGroup config = injector.getInstance(MultiModeDrtEstimatorConfigGroup.class);

		// Collect all mode specific bindings
		for (DrtEstimatorConfigGroup el : config.getModalElements()) {
			estimators.put(el.mode, injector.getInstance(Key.get(DrtEstimator.class, DvrpModes.mode(el.mode))));
		}
	}

	@Override
	public double estimate(EstimatorContext context, String mode, Leg leg, ModeAvailability option) {

		if (!(leg.getRoute() instanceof DrtRoute route))
			throw new IllegalStateException("Drt leg routes must be of type DrtRoute.");

		OptionalTime departureTime = leg.getDepartureTime();

		DrtEstimator estimator = Objects.requireNonNull(estimators.get(mode), "No drt estimator found for mode :" + mode);

		DrtEstimator.Estimate est = estimator.estimate(route, departureTime);
		ModeUtilityParameters params = context.scoring.modeParams.get(mode);

		// TODO: How is waiting time scored ?
		return params.constant +
				params.marginalUtilityOfDistance_m * est.distance() +
				params.marginalUtilityOfTraveling_s * est.travelTime() +
			//	context.scoring.marginalUtilityOfWaitingPt_s * est.waitingTIme() +
				context.scoring.marginalUtilityOfMoney * params.monetaryDistanceCostRate * est.distance() +
				context.scoring.marginalUtilityOfMoney * est.fare();

	}
}
