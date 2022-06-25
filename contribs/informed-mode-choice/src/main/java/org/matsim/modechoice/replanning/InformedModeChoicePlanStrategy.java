package org.matsim.modechoice.replanning;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.selectors.RandomUnscoredPlanSelector;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.modechoice.InformedModeChoiceConfigGroup;
import org.matsim.modechoice.PlanCandidate;
import org.matsim.modechoice.search.TopKChoicesGenerator;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * The main strategy for informed mode choice.
 */
public class InformedModeChoicePlanStrategy implements PlanStrategy {

	private final RandomUnscoredPlanSelector<Plan, Person> unscored = new RandomUnscoredPlanSelector<>();

	private final InformedModeChoiceConfigGroup config;
	private final TopKChoicesGenerator generator;
	private final ScoringParametersForPerson scoringParams;

	private OLSMultipleLinearRegression reg;

	public InformedModeChoicePlanStrategy(InformedModeChoiceConfigGroup config, ScoringParametersForPerson scoringParams, TopKChoicesGenerator generator) {
		this.config = config;
		this.scoringParams = scoringParams;
		this.generator = generator;
	}

	@Override
	public void init(ReplanningContext replanningContext) {

		reg = new OLSMultipleLinearRegression();

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
		ScoringParameters params = scoringParams.getScoringParameters(person.getSelectedPlan().getPerson());

		// TODO: problem is that the best is also grounded on one estimator
		// TODO: we could just estimate the difference directly with modes as input features

		// Collect for each plan the differences and the present of modes and differences to best

		// TODO: one agents plan might deviate stronger than the mean

		// TODO: substract the positive score gained from actitivies
		// remove CharyparNagelActivityScoring

		for (Plan plan : person.getPlans()) {

			double base = baseScore(plan, params);

			double legScore = plan.getScore() - base;

			List<String> modes = config.getModes();

			double[] ft = new double[modes.size()];

			for (int i = 0; i < modes.size(); i++) {
				int count = StringUtils.countMatches(plan.getType(), modes.get(i));
				ft[i] = count;
			}

			// Double estimate

		}

		Collection<PlanCandidate> candidates = generator.generate(person.getSelectedPlan());
	}


	/**
	 * Calculate the basis score for a plan achieved by performing activities.
	 */
	public double baseScore(Plan plan, ScoringParameters params) {
		CharyparNagelActivityScoring scorer = new CharyparNagelActivityScoring(params);
		List<Activity> activities = PopulationUtils.getActivities(plan, TripStructureUtils.StageActivityHandling.ExcludeStageActivities);

		for (int i = 0; i < activities.size(); i++) {

			Activity act = activities.get(i);

			if (i == 0) {
				scorer.handleActivity(act);
			} else if (i == activities.size() - 1)
				scorer.handleLastActivity(act);
			else
				scorer.handleActivity(act);
		}

		return scorer.getScore();
	}

	@Override
	public void finish() {
		// Nothing to do
	}
}
