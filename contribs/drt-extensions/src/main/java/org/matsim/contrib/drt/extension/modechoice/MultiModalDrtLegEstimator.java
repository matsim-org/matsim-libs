package org.matsim.contrib.drt.extension.modechoice;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.drt.estimator.DrtEstimator;
import org.matsim.contrib.drt.fare.DrtFareParams;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
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
public class MultiModalDrtLegEstimator implements LegEstimator {

	private static final Logger log = LogManager.getLogger(MultiModalDrtLegEstimator.class);

	protected final Map<String, DrtEstimator> estimators = new HashMap<>();
	protected final Map<String, DrtFareParams> fareParams = new HashMap<>();

	@Inject
	public MultiModalDrtLegEstimator(Config config, Map<DvrpMode, DrtEstimator> estimators) {
		for (Map.Entry<DvrpMode, DrtEstimator> e : estimators.entrySet()) {
			this.estimators.put(e.getKey().value(), e.getValue());
		}

		MultiModeDrtConfigGroup drtConfigs = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);
		for (DrtConfigGroup modal : drtConfigs.getModalElements()) {
			if (modal.getDrtFareParams().isPresent()) {
				fareParams.put(modal.getMode(), modal.getDrtFareParams().get());
			}
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

		double fare = 0;
		if (fareParams.containsKey(mode)) {
			DrtFareParams fareParams = this.fareParams.get(mode);
			fare = fareParams.distanceFare_m * route.getDistance()
				+ fareParams.timeFare_h * route.getDirectRideTime() / 3600.0
				+ fareParams.baseFare;

			fare = Math.max(fare, fareParams.minFarePerTrip);
		}

		// By default, waiting time is scored as travel time
		return params.constant +
			params.marginalUtilityOfDistance_m * est.rideDistance() +
			params.marginalUtilityOfTraveling_s * est.rideTime() +
			params.marginalUtilityOfTraveling_s * est.waitingTime() +
			context.scoring.marginalUtilityOfMoney * params.monetaryDistanceCostRate * est.rideDistance() +
			context.scoring.marginalUtilityOfMoney * fare;

	}
}
