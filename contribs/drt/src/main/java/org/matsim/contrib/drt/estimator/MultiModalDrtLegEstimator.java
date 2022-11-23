package org.matsim.contrib.drt.estimator;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.drt.estimator.run.DrtEstimatorConfigGroup;
import org.matsim.contrib.drt.estimator.run.MultiModeDrtEstimatorConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.modechoice.EstimatorContext;
import org.matsim.modechoice.ModeAvailability;
import org.matsim.modechoice.estimators.LegEstimator;

import java.util.HashMap;
import java.util.Map;


/**
 * Aggregate class for informed-mode-choice that makes sure to invoke the correct estimator for each drt mode.
 */
public class MultiModalDrtLegEstimator implements LegEstimator<ModeAvailability> {

	private final Map<String, DrtAccessEgressEstimator> accessEgress = new HashMap<>();
	private final Map<String, DrtEstimator> estimators = new HashMap<>();

	@Inject
	public MultiModalDrtLegEstimator(Injector injector) {

		MultiModeDrtEstimatorConfigGroup config = injector.getInstance(MultiModeDrtEstimatorConfigGroup.class);

		// Collect all mode specific bindings
		for (DrtEstimatorConfigGroup el : config.getModalElements()) {
			accessEgress.put(el.mode, injector.getInstance(Key.get(DrtAccessEgressEstimator.class, DvrpModes.mode(el.mode))));
			estimators.put(el.mode, injector.getInstance(Key.get(DrtEstimator.class, DvrpModes.mode(el.mode))));
		}
	}

	@Override
	public double estimate(EstimatorContext context, String mode, Leg leg, ModeAvailability option) {

		System.out.println("Estimate");
		// TODO:

		return 0;
	}
}
