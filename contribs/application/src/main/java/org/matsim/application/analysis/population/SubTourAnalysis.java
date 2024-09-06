package org.matsim.application.analysis.population;

import com.google.common.math.Quantiles;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.ChooseRandomLegModeForSubtour;
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.core.router.DefaultAnalysisMainModeIdentifier;
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

	@CommandLine.Option(names = "--chain-based-modes", description = "Chain-based modes", defaultValue = "car,bike", split = ",")
	private Set<String> chainBasedModes;

	@CommandLine.Option(names = "--subpopulation", description = "Subpopulation filter", defaultValue = "person")
	private String subpopulation;

	@CommandLine.Option(names = "--person", description = "Filter single person")
	private String person;

	@CommandLine.Option(names = "--behaviour", description = "Subtour mode-choice behaviour", defaultValue = "betweenAllAndFewerConstraints")
	private SubtourModeChoice.Behavior behavior;

	@CommandLine.Option(names = "--st-proba", description = "Probability for single trip mode-choice", defaultValue = "0.5")
	private double singleTrip;

	@CommandLine.Option(names = "--coord-dist", description = "Use coordinate distance for subtours if greater 0", defaultValue = "0")
	private double coordDist;

	@CommandLine.Option(names = "--iter", description = "Iterate strategy to output choice sets", defaultValue = "1")
	private int iter;

	public static void main(String[] args) {
		new SubTourAnalysis().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		Population population = PopulationUtils.readPopulation(input.toString());

		List<TripStructureUtils.Subtour> subtours = new ArrayList<>();

		Set<String> modes = new HashSet<>(chainBasedModes);

		List<Person> persons = new ArrayList<>();

		for (Person agent : population.getPersons().values()) {
			Plan plan = agent.getSelectedPlan();

			String subpop = PopulationUtils.getSubpopulation(agent);
			if (!subpopulation.isEmpty() && !subpop.equals(subpopulation)) continue;

			List<Leg> legs = TripStructureUtils.getLegs(plan);

			for (Leg leg : legs) {
				modes.add(leg.getMode());
			}

			if (person != null && !agent.getId().toString().equals(person))
				continue;

			Collection<TripStructureUtils.Subtour> subtour = TripStructureUtils.getSubtours(plan);
			subtours.addAll(subtour);
			persons.add(agent);
		}

		log.info("Detected modes: {}", modes);

		ChooseRandomLegModeForSubtour strategy = new ChooseRandomLegModeForSubtour(new DefaultAnalysisMainModeIdentifier(), plan -> modes,
				modes.toArray(new String[0]), chainBasedModes.toArray(new String[0]), new Random(1234), behavior, singleTrip, coordDist);

		int closed = 0;
		int massConserving = 0;

		for (TripStructureUtils.Subtour st : subtours) {
			if (st.isClosed())
				closed++;

			if (strategy.isMassConserving(st))
				massConserving++;
		}

		if (person != null && persons.size() > 0) {

			Person p = persons.get(0);

			log.info(p.getAttributes());
			log.info(p.getSelectedPlan());
		}

		log.info("Subtours: {} | closed: {}% | massConserving: {}%", subtours.size(), 100d * closed / subtours.size(), 100d * massConserving / subtours.size());

		DoubleList nChoices = new DoubleArrayList(persons.size());

		// Iterate the strategy for testing purpose
		for (int i = 0; i < iter; i++) {

			Set<ChooseRandomLegModeForSubtour.Candidate> c = new HashSet<>();
			int empty = 0;
			int tripMissing = 0;

			for (Person p : persons) {

				Plan plan = p.getSelectedPlan();

				List<TripStructureUtils.Trip> trips = new ArrayList<>(TripStructureUtils.getTrips(plan));
				List<ChooseRandomLegModeForSubtour.Candidate> cset = strategy.determineChoiceSet(plan);

				// person with no trips always have no choice set
				if (cset.isEmpty() && !trips.isEmpty()) {
					empty++;
				}

				// remove all trips that are considered for a change
				for (ChooseRandomLegModeForSubtour.Candidate cs : cset) {
					cs.getSubtour().getTrips().forEach(trips::remove);
				}

				// If áll trips are in one of the choice sets, this should be empty now
				if (!trips.isEmpty())
					tripMissing++;

				if (person != null) {

					log.info("Choices:");
					for (ChooseRandomLegModeForSubtour.Candidate cs : cset) {
						log.info(cs);
					}
				}

				nChoices.add(cset.size());
				c.addAll(cset);
				strategy.run(plan);
			}

			OptionalDouble avg = nChoices.doubleStream().average();

			log.info("Total choice sets: {}, persons with no choices: {}, (avg: {}, q90: {}), person with uncovered trips in choice-set: {}",
					c.size(), empty, avg.orElse(0), Quantiles.percentiles().index(90).compute(nChoices), tripMissing);
		}

		return 0;
	}
}
