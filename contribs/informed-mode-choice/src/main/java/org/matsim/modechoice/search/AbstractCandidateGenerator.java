package org.matsim.modechoice.search;

import com.google.inject.Inject;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.modechoice.*;
import org.matsim.modechoice.constraints.TripConstraint;
import org.matsim.modechoice.estimators.FixedCostsEstimator;
import org.matsim.modechoice.estimators.LegEstimator;
import org.matsim.modechoice.estimators.MinMaxEstimate;
import org.matsim.modechoice.estimators.TripEstimator;

import java.util.List;
import java.util.Map;
import java.util.Set;

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

	protected final InformedModeChoiceConfigGroup config;

	protected AbstractCandidateGenerator(InformedModeChoiceConfigGroup config) {
		this.config = config;
	}

}
