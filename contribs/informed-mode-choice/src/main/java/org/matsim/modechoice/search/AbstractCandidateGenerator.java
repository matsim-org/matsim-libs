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

import static org.matsim.modechoice.PlanModelService.ConstraintHolder;

/**
 * Holds fields required for injection.
 */
abstract class AbstractCandidateGenerator implements CandidateGenerator {

	@Inject
	protected ScoringParametersForPerson params;

	@Inject
	protected EstimateRouter router;

	@Inject
	protected Set<TripConstraint<?>> constraints;

	@Inject
	protected PlanModelService service;

	@Inject
	protected EstimateCalculator calculator;

	@Inject
	protected Provider<CandidatePruner> pruner;

	protected final InformedModeChoiceConfigGroup config;
	protected final Set<String> allModes;

	protected AbstractCandidateGenerator(InformedModeChoiceConfigGroup config) {
		this.config = config;
		this.allModes = new HashSet<>(config.getModes());
	}

}
