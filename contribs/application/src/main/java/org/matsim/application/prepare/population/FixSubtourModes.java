package org.matsim.application.prepare.population;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.ChooseRandomLegModeForSubtour;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.core.router.DefaultAnalysisMainModeIdentifier;
import org.matsim.core.router.TripStructureUtils;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@CommandLine.Command(name = "fix-subtour-modes", description = "Fix modes for subtours that contain chain and non-chain based modes, by choosing one of the found modes randomly.", showDefaultValues = true)
public class FixSubtourModes implements MATSimAppCommand, PersonAlgorithm {

	private static final Logger log = LogManager.getLogger(FixSubtourModes.class);

	@CommandLine.Option(names = "--input", description = "Path to input population", required = true)
	private Path input;

	@CommandLine.Option(names = "--output", description = "Path for output population", required = true)
	private Path output;

	@CommandLine.Option(names = "--subpopulation", description = "Subpopulation filter", defaultValue = "person")
	private String subpopulation;

	@CommandLine.Option(names = "--chain-based-modes", description = "Chain-based modes", defaultValue = "car,bike", split = ",")
	private Set<String> chainBasedModes;

	@CommandLine.Option(names = "--all-plans", description = "Whether to fix all plans, or only selected", defaultValue = "false")
	private boolean allPlans;

	@CommandLine.Option(names = "--mass-conservation", description = "Whether to check for the mass conservation of the whole plan", defaultValue = "true", negatable = true)
	private boolean massConservation;

	@CommandLine.Option(names = "--coord-dist", description = "Use coordinate distance for subtours if greater 0", defaultValue = "0")
	private double coordDist;

	private final SplittableRandom rnd = new SplittableRandom();
	private ChooseRandomLegModeForSubtour strategy = null;

	// Counters
	private int processed;
	private int fixed;
	private int mc;

	public static void main(String[] args) {
		new FixSubtourModes().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		Population population = PopulationUtils.readPopulation(input.toString());

		if (output.getParent() != null) Files.createDirectories(output.getParent());

		init();

		for (Person person : population.getPersons().values()) {
			run(person);
		}

		if (mc > 0) {
			log.info("Fixed {} mass conservation violations.", mc);
		}

		log.info("Processed {} out of {} persons. Modified {} subtours", processed, population.getPersons().size(), fixed);

		PopulationUtils.writePopulation(population, output.toString());

		return 0;
	}

	/**
	 * Needs to be called before {@link #run(Person)}
	 */
	public void init() {

		processed = 0;
		fixed = 0;
		mc = 0;

		if (massConservation) {
			String[] modes = chainBasedModes.toArray(new String[0]);
			strategy = new ChooseRandomLegModeForSubtour(new DefaultAnalysisMainModeIdentifier(), null,
					modes, modes, new Random(1234),
					SubtourModeChoice.Behavior.betweenAllAndFewerConstraints, 0, coordDist);
		}
	}

	@Override
	public void run(Person person) {

		String subpop = PopulationUtils.getSubpopulation(person);
		if (!subpopulation.isEmpty() && !subpop.equals(subpopulation)) return;

		List<? extends Plan> plans = allPlans ? person.getPlans() : List.of(person.getSelectedPlan());

		outer:
		for (Plan plan : plans) {

			try {
				Collection<TripStructureUtils.Subtour> subtours = TripStructureUtils.getSubtours(plan, coordDist);
				for (TripStructureUtils.Subtour st : subtours) {

					if (fixSubtour(person, st))
						fixed++;

					// This replicates the behaviour of betweenAllAndFewerConstraints
					if (strategy != null && st.getParent() != null && !strategy.isMassConserving(st)) {
						mc++;
						fixPlan(plan);
						continue outer;
					}

				}
			} catch (Exception e) {
				log.warn("Exception occurred when handling person {}: {}. Whole plan will be set to walk.", person.getId(), e);

				for (Leg leg : TripStructureUtils.getLegs(plan)) {
					leg.setRoute(null);
					leg.setMode(TransportMode.walk);
					TripStructureUtils.setRoutingMode(leg, TransportMode.walk);
				}

				fixed++;
			}
		}

		processed++;

	}

	private void fixPlan(Plan plan) {

		for (TripStructureUtils.Trip trip : TripStructureUtils.getTrips(plan)) {
			for (Leg leg : trip.getLegsOnly()) {
				leg.setRoute(null);
				leg.setMode(TransportMode.walk);
				TripStructureUtils.setRoutingMode(leg, TransportMode.walk);
			}
		}
	}

	/**
	 * Fix a subtour if it violates the constraints.
	 *
	 * @return whether subtour was adjusted
	 */
	public boolean fixSubtour(Person person, TripStructureUtils.Subtour st) {
		boolean containsChainBasedMode = false;
		Set<String> originalModes = new HashSet<>();

		// Trips that are not part of any child subtour
		Set<TripStructureUtils.Trip> childTrips = new HashSet<>(st.getTripsWithoutSubSubtours());

		for (TripStructureUtils.Trip trip : st.getTrips()) {

			String mainMode = TripStructureUtils.identifyMainMode(trip.getTripElements());

			if (childTrips.contains(trip) && chainBasedModes.contains(mainMode))
				containsChainBasedMode = true;

			// if trips are part of another subtour, they are allowed to have different modes
			if (childTrips.contains(trip))
				originalModes.add(mainMode);

		}

		// don't mix chain-based with other modes in one subtour
		if (containsChainBasedMode && originalModes.size() > 1) {
			selectMode(person, st);
			return true;
		}

		return false;
	}

	private void selectMode(Person p, TripStructureUtils.Subtour st) {

		List<String> modes = st.getTrips().stream().map(t -> TripStructureUtils.identifyMainMode(t.getTripElements()))
				.collect(Collectors.toList());

		if (!PersonUtils.canUseCar(p) && modes.contains("car"))
			throw new IllegalStateException("Person " + p.getId() + " uses car as mode, which should not be allowed.");

		String mode = modes.get(rnd.nextInt(modes.size()));

		for (TripStructureUtils.Trip trip : st.getTrips()) {
			for (Leg leg : trip.getLegsOnly()) {
				leg.setRoute(null);
				leg.setMode(mode);
				TripStructureUtils.setRoutingMode(leg, mode);
			}
		}
	}
}
