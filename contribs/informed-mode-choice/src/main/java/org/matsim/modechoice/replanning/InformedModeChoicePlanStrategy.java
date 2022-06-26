package org.matsim.modechoice.replanning;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.selectors.RandomUnscoredPlanSelector;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.modechoice.InformedModeChoiceConfigGroup;
import org.matsim.modechoice.PlanCandidate;
import org.matsim.modechoice.search.TopKChoicesGenerator;

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;

/**
 * The main strategy for informed mode choice.
 */
public class InformedModeChoicePlanStrategy implements PlanStrategy {

	private static final Logger log = LogManager.getLogger(InformedModeChoicePlanStrategy.class);

	private final RandomUnscoredPlanSelector<Plan, Person> unscored = new RandomUnscoredPlanSelector<>();

	private final InformedModeChoiceConfigGroup config;
	private final TopKChoicesGenerator generator;

	private final SimpleRegression[] reg;
	private final Scenario scenario;
	private final OutputDirectoryHierarchy controlerIO;

	public InformedModeChoicePlanStrategy(InformedModeChoiceConfigGroup config, TopKChoicesGenerator generator,
	                                      Scenario scenario, OutputDirectoryHierarchy controlerIO) {
		this.config = config;
		this.generator = generator;
		this.reg = new SimpleRegression[config.getModes().size()];
		this.scenario = scenario;
		this.controlerIO = controlerIO;
	}

	@Override
	public void init(ReplanningContext replanningContext) {


		String f = controlerIO.getIterationFilename(replanningContext.getIteration(), "scoreEstimates.csv.gz");

		try (CSVPrinter csv = new CSVPrinter(IOUtils.getBufferedWriter(f), CSVFormat.DEFAULT)) {

			csv.print("person");
			csv.print("subpopulation");
			csv.print("plan");
			csv.print("type");
			csv.print("score");
			csv.print(PlanCandidate.MAX_ESTIMATE);
			csv.print(PlanCandidate.MIN_ESTIMATE);

			for (String mode : config.getModes()) {
				csv.print(mode);
			}
			csv.println();

			for (Person person : scenario.getPopulation().getPersons().values()) {

				int i = 0;
				for (Plan plan : person.getPlans()) {

					csv.print(person.getId());
					csv.print(PopulationUtils.getSubpopulation(person));
					csv.print(i++);
					csv.print(plan.getType());
					csv.print(plan.getScore());
					csv.print(plan.getAttributes().getAttribute(PlanCandidate.MAX_ESTIMATE));
					csv.print(plan.getAttributes().getAttribute(PlanCandidate.MIN_ESTIMATE));

					for (String mode : config.getModes()) {
						csv.print(StringUtils.countMatches(plan.getType(), mode));
					}

					csv.println();
				}
			}

		} catch (IOException e) {
			log.error("Could not write Estimate csv", e);
		}
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

/*
		Plan best = person.getPlans().stream()
				.filter(p -> p.getAttributes().getAttribute(PlanCandidate.MAX_ESTIMATE) != null)
				.max(Comparator.comparingDouble(Plan::getScore)).orElse(null);

		if (best != null) {


			double bestEstimate = (double) best.getAttributes().getAttribute(PlanCandidate.MAX_ESTIMATE);
			double[] baseLine = PlanCandidate.occurrences(config.getModes(), best.getType());

			// Collect for each plan the differences and the present of modes and differences to best

			// TODO: one agents plan might deviate stronger than the mean

			for (Plan plan : person.getPlans()) {

				Double maxEstimate = (Double) plan.getAttributes().getAttribute(PlanCandidate.MAX_ESTIMATE);
				if (maxEstimate == null || plan == best)
					continue;

				double[] row = PlanCandidate.occurrences(config.getModes(), plan.getType());

				// Difference in execution should ideally be the same as difference in estimation
				double y = (best.getScore() - plan.getScore()) - (bestEstimate - maxEstimate);
				// if y is negative the score was underestimated

				for (int i = 0; i < baseLine.length; i++) {
					reg[i].addData(row[i] - baseLine[i], y);
				}
			}

		}
 */

		//Plan best = person.getPlans().stream().max(Comparator.comparingDouble(Plan::getScore)).orElseThrow();

		Collection<PlanCandidate> candidates = generator.generate(person.getSelectedPlan());

		// TODO: should replace old plans with same type
		// TODO: should remove and not regenrate unpromising plan types

		applyCandidates(person, person.getSelectedPlan(), candidates);

	}

	private void applyCandidates(HasPlansAndId<Plan, Person> person, Plan plan, Collection<PlanCandidate> candidates) {

		for (PlanCandidate c : candidates) {

			if (plan == null)
				plan = person.createCopyOfSelectedPlanAndMakeSelected();

			c.applyTo(plan);
			plan = null;
		}

	}

	@Override
	public void finish() {

	}
}
