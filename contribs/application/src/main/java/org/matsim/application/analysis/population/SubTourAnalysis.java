package org.matsim.application.analysis.population;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.analysis.DefaultAnalysisMainModeIdentifier;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.ChooseRandomLegModeForSubtour;
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.core.router.TripStructureUtils;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.*;

@CommandLine.Command(
		name = "subtours",
		description = "Analyze subtours of a population."
)
public class SubTourAnalysis implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(SubTourAnalysis.class);

	@CommandLine.Parameters(arity = "1", paramLabel = "POPULATION", description = "Path to population")
	private Path input;

	@CommandLine.Option(names = "--chain-modes", description = "Chain-based modes", defaultValue = "car,bike", split = ",")
	private Set<String> chainBasedModes;

	@CommandLine.Option(names = "--ignore-plans", description = "Ignore plans containing certain modes", defaultValue = "freight", split = ",")
	private Set<String> ignoreModes;

	@CommandLine.Option(names = "--behaviour", description = "Subtour mode-choice behaviour", defaultValue = "betweenAllLessConstraints")
	private SubtourModeChoice.Behavior behavior;

	@CommandLine.Option(names = "--st-proba", description = "Probability for single trip mode-choice", defaultValue = "0.2")
	private double singleTrip;

	@CommandLine.Option(names = "--iter", description = "Iterate strategy to output choice sets", defaultValue = "1")
	private int iter;

	public static void main(String[] args) {
		new SubTourAnalysis().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		Population population = PopulationUtils.readPopulation(input.toString());

		List<TripStructureUtils.Subtour> subtours = new ArrayList<>();

		Set<String> modes = new HashSet<>();

		outer:
		for (Person agent : population.getPersons().values()) {
			Plan plan = agent.getSelectedPlan();

			List<Leg> legs = TripStructureUtils.getLegs(plan);

			for (Leg leg : legs) {
				if (ignoreModes.contains(leg.getMode()))
					break outer;

				modes.add(leg.getMode());
			}

			Collection<TripStructureUtils.Subtour> subtour = TripStructureUtils.getSubtours(plan);
			subtours.addAll(subtour);
		}

		log.info("Detected modes: {}", modes);

		ChooseRandomLegModeForSubtour strategy = new ChooseRandomLegModeForSubtour(new DefaultAnalysisMainModeIdentifier(), plan -> modes,
				modes.toArray(new String[0]), chainBasedModes.toArray(new String[0]), new Random(1234), behavior, singleTrip);

		int closed = 0;
		int massConserving = 0;

		for (TripStructureUtils.Subtour st : subtours) {
			if (st.isClosed())
				closed++;

			if (strategy.isMassConserving(st))
				massConserving++;
		}

		log.info("Subtours: {} | closed: {}% | massConserving: {}%", subtours.size(), 100d * closed / subtours.size(), 100d * massConserving / subtours.size());


		// Iterate the strategy for testing purpose
		for (int i = 0; i < iter; i++) {

			Set<ChooseRandomLegModeForSubtour.Candidate> c = new HashSet<>();
			int empty = 0;

			for (Person p : population.getPersons().values()) {

				Plan plan = p.getSelectedPlan();

				List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(plan);
				List<ChooseRandomLegModeForSubtour.Candidate> cs = strategy.determineChoiceSet(plan);

				// person with no trips always have no choice set
				if (cs.isEmpty() && !trips.isEmpty()) {
						empty++;
						strategy.determineChoiceSet(plan);
				}

				c.addAll(cs);
				strategy.run(plan);
			}

			log.info("Total choice sets: {}, persons with no choices: {}", c.size(), empty);

		}


		return 0;
	}
}
