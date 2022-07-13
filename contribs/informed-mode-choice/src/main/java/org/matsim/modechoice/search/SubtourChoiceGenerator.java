package org.matsim.modechoice.search;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.modechoice.*;
import org.matsim.modechoice.constraints.RelaxedMassConservationConstraint;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Collection;
import java.util.Map;


/**
 * Creates choices based on subtours.
 */
public class SubtourChoiceGenerator extends AbstractCandidateGenerator {

	@Inject
	private RelaxedMassConservationConstraint st;

	@Inject
	protected SubtourChoiceGenerator(InformedModeChoiceConfigGroup config) {
		super(config);
	}

	@Override
	public Collection<PlanCandidate> generate(PlanModel planModel, @Nullable boolean[] mask) {

		EstimatorContext context = new EstimatorContext(planModel.getPerson(), params.getScoringParameters(planModel.getPerson()));

		RelaxedMassConservationConstraint.Context mc = st.getContext(context, planModel);

		return null;
	}
}
