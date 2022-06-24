package org.matsim.modechoice.replanning;

import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.selectors.RandomUnscoredPlanSelector;
import org.matsim.modechoice.PlanCandidate;
import org.matsim.modechoice.search.TopKChoicesGenerator;

import java.util.Collection;
import java.util.Comparator;

/**
 * The main strategy for informed mode choice.
 */
public class InformedModeChoicePlanStrategy implements PlanStrategy {

	private final RandomUnscoredPlanSelector<Plan, Person> unscored = new RandomUnscoredPlanSelector<>();

	private final TopKChoicesGenerator generator;

	public InformedModeChoicePlanStrategy(TopKChoicesGenerator generator) {
		this.generator = generator;
	}

	@Override
	public void init(ReplanningContext replanningContext) {

		// Nothing to do
	}

	@Override
	public void run(HasPlansAndId<Plan, Person> person) {

		// TODO: needs own multithreading

		Plan unscored = this.unscored.selectPlan(person);

		// If there are unscored plans, they need to be executed first
		if (unscored != null) {
			person.setSelectedPlan(unscored);
			return;
		}

		Plan best = person.getPlans().stream().max(Comparator.comparingDouble(Plan::getScore)).orElseThrow();

		// TODO: problem is that the best is also grounded on one estimator
		// TODO: we could just estimate the difference directly with modes as input features

		// Collect for each plan the differences and the present of modes and differences to best

		// TODO: one agents plan might deviate stronger than the mean


		for (Plan plan : person.getPlans()) {

			plan.getScore();

		}


		Collection<PlanCandidate> candidates = generator.generate(person.getSelectedPlan());


	}

	@Override
	public void finish() {
		// Nothing to do
	}
}
