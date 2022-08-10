package org.matsim.modechoice.search;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.modechoice.*;
import org.matsim.modechoice.constraints.TripConstraint;
import org.matsim.modechoice.estimators.FixedCostsEstimator;
import org.matsim.modechoice.estimators.TripEstimator;
import org.matsim.modechoice.pruning.CandidatePruner;

import java.util.*;
import java.util.function.Predicate;

/**
 * Holds fields required for injection.
 */
abstract class AbstractCandidateGenerator implements CandidateGenerator {

	@Inject
	protected Map<String, TripEstimator<?>> tripEstimator;

	@Inject
	protected Map<String, FixedCostsEstimator<?>> fixedCosts;

	@Inject
	protected ScoringParametersForPerson params;

	@Inject
	protected EstimateRouter router;

	@Inject
	protected Set<TripConstraint<?>> constraints;

	@Inject
	protected PlanModelService service;

	@Inject
	protected Provider<CandidatePruner> pruner;

	protected final InformedModeChoiceConfigGroup config;
	protected final Set<String> allModes;

	protected AbstractCandidateGenerator(InformedModeChoiceConfigGroup config) {
		this.config = config;
		this.allModes = new HashSet<>(config.getModes());
	}

	@SuppressWarnings("unchecked")
	protected final List<ConstraintHolder<?>> buildConstraints(EstimatorContext context, PlanModel planModel) {

		List<TopKChoicesGenerator.ConstraintHolder<?>> constraints = new ArrayList<>();
		for (TripConstraint<?> c : this.constraints) {
			constraints.add(new TopKChoicesGenerator.ConstraintHolder<>(
					(TripConstraint<Object>) c,
					c.getContext(context, planModel)
			));
		}

		return constraints;
	}

	protected record ConstraintHolder<T>(TripConstraint<T> constraint, T context) implements Predicate<String[]> {

		@Override
		public boolean test(String[] modes) {
			return constraint.isValid(context, modes);
		}

		public boolean testMode(String[] currentModes, ModeEstimate mode, boolean[] mask) {
			return constraint.isValidMode(context, currentModes, mode, mask);
		}

	}

}
