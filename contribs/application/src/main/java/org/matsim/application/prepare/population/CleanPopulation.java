package org.matsim.application.prepare.population;

import com.google.common.collect.Lists;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.*;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.algorithms.TripsToLegsAlgorithm;
import org.matsim.core.router.RoutingModeMainModeIdentifier;
import org.matsim.core.router.TripStructureUtils;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Removes information from a population file.
 *
 * @author rakow
 */
@CommandLine.Command(
	name = "clean-population",
	description = "Remove information from population, such as routes or unselected plans.",
	mixinStandardHelpOptions = true,
	showDefaultValues = true
)
public class CleanPopulation implements MATSimAppCommand, PersonAlgorithm {

	private static final Logger log = LogManager.getLogger(CleanPopulation.class);

	@CommandLine.Option(names = "--plans", description = "Input original plan file", required = true)
	private Path plans;

	@CommandLine.Option(names = "--remove-unselected-plans", description = "Keep only the selected plan.", defaultValue = "false")
	private boolean rmUnselected;

	@CommandLine.Option(names = "--remove-activity-location", description = "Remove link and facility from activities", defaultValue = "false")
	private boolean rmActivityLocations;

	@CommandLine.Option(names = "--remove-activity-facilities", description = "Remove facility ids from activities", defaultValue = "false")
	private boolean rmActivityFacilities;

	@CommandLine.Option(names = "--remove-routes", description = "Remove route information", defaultValue = "false")
	private boolean rmRoutes;

	@CommandLine.Option(names = "--trips-to-legs", description = "Convert trips to single legs (Removes all routing and individual legs).", defaultValue = "false")
	private boolean tripsToLegs;

	@CommandLine.Option(names = "--remove-legs", description = "Remove the legs of trips for given modes, e.g. pt", split = ",", defaultValue = "")
	private Set<String> rmLegs;

	@CommandLine.Option(names = "--output", description = "Output file name", required = true)
	private Path output;

	// Using the analysis main mode identifier instead of the routing mode based one on purpose
	// to be able to process older population files without any routing modes!
	private final TripsToLegsAlgorithm trips2Legs = new TripsToLegsAlgorithm(new RoutingModeMainModeIdentifier());

	//Needed for registration of this application in scenarios
	@SuppressWarnings("unused")
	public CleanPopulation() {
	}

	private CleanPopulation(boolean rmUnselected, boolean rmActivityLocations, boolean rmActivityFacilities, boolean rmRoutes, boolean tripsToLegs, Set<String> rmLegs) {
		this.rmUnselected = rmUnselected;
		this.rmActivityLocations = rmActivityLocations;
		this.rmActivityFacilities = rmActivityFacilities;
		this.rmRoutes = rmRoutes;
		this.tripsToLegs = tripsToLegs;
		this.rmLegs = rmLegs;
	}

	public static void main(String[] args) {
		System.exit(new CommandLine(new CleanPopulationBuilder().createCleanPopulation()).execute(args));
	}

	@Override
	public Integer call() throws Exception {

		Population population = PopulationUtils.readPopulation(plans.toString());

		if (!rmRoutes && !rmActivityLocations && !rmUnselected && !tripsToLegs && !rmActivityFacilities && (rmLegs == null || rmLegs.isEmpty())) {
			log.error("None of the 'remove' commands specified.");
			return 2;
		}

		if (output.getParent() != null)
			Files.createDirectories(output.getParent());

		cleanPopulation(population);

		PopulationUtils.writePopulation(population, output.toString());
		return 0;
	}

	@Override
	public void run(Person person) {
		if (rmUnselected) {
			removeUnselectedPlans(person);
		}

		for (Plan plan : person.getPlans()) {
			if (tripsToLegs)
				trips2Legs.run(plan);

			if (rmLegs != null && !rmLegs.isEmpty()) {
				removeLegs(plan, rmLegs);
			}

			for (PlanElement el : plan.getPlanElements()) {
				if (rmRoutes) {
					removeRouteFromLeg(el);
				}

				if (rmActivityLocations) {
					removeActivityLocation(el);
				}

				if (rmActivityFacilities && el instanceof Activity act) {
					act.setFacilityId(null);
				}
			}
		}
	}

	public void cleanPopulation(Population population) {
		for (Person person : population.getPersons().values()) {
			run(person);
		}
	}

	/**
	 * Remove the legs of trips that contain any of the given modes.
	 */
	public static void removeLegs(Plan plan, Set<String> modes) {

		final List<PlanElement> planElements = plan.getPlanElements();

		// Remove all pt trips
		for (TripStructureUtils.Trip trip : TripStructureUtils.getTrips(plan)) {

			// Check if any of the modes is in the trip
			Optional<Leg> cleanLeg = trip.getLegsOnly().stream().filter(l -> modes.contains(l.getMode())).findFirst();

			if (cleanLeg.isEmpty())
				continue;

			// Replaces all trip elements and inserts single leg
			final List<PlanElement> fullTrip =
				planElements.subList(
					planElements.indexOf(trip.getOriginActivity()) + 1,
					planElements.indexOf(trip.getDestinationActivity()));

			fullTrip.clear();

			Leg leg = PopulationUtils.createLeg(cleanLeg.get().getMode());
			TripStructureUtils.setRoutingMode(leg, cleanLeg.get().getMode());
			fullTrip.add(leg);
		}
	}

	/**
	 * Remove link and facility information from activity.
	 */
	public static void removeActivityLocation(PlanElement el) {
		if (el instanceof Activity act) {
			act.setLinkId(null);

//			only remove facility id if the act has a coord.
//			otherwise the act would have no location information (linkId=null, facilityId=null, coord=null). -sm0625
			if (act.getCoord() != null) {
				act.setFacilityId(null);
			} else {
				log.info("Activity {}\n has no coord. Its activity facility is therefore not removed because it would have ended up without any location: linkId=null, facilityId=null, coord=null.", act);
			}
		}
	}

	/**
	 * Remove route information from leg.
	 */
	public static void removeRouteFromLeg(PlanElement el) {
		if (el instanceof Leg leg) {
			leg.setRoute(null);
		}
	}

	/**
	 * Remove all unselected plans for given person.
	 */
	public static void removeUnselectedPlans(Person person) {
		Plan selected = person.getSelectedPlan();
		for (Plan plan : Lists.newArrayList(person.getPlans())) {
			if (plan != selected)
				person.removePlan(plan);
		}
	}

	public static class CleanPopulationBuilder {
		private boolean rmUnselected;
		private boolean rmActivityLocations;
		private boolean rmActivityFacilities;
		private boolean rmRoutes;
		private boolean tripsToLegs;
		private Set<String> rmLegs;

		public CleanPopulationBuilder setRmUnselected(boolean rmUnselected) {
			this.rmUnselected = rmUnselected;
			return this;
		}

		public CleanPopulationBuilder setRmActivityLocations(boolean rmActivityLocations) {
			this.rmActivityLocations = rmActivityLocations;
			return this;
		}

		public CleanPopulationBuilder setRmActivityFacilities(boolean rmActivityFacilities) {
			this.rmActivityFacilities = rmActivityFacilities;
			return this;
		}

		public CleanPopulationBuilder setRmRoutes(boolean rmRoutes) {
			this.rmRoutes = rmRoutes;
			return this;
		}

		public CleanPopulationBuilder setTripsToLegs(boolean tripsToLegs) {
			this.tripsToLegs = tripsToLegs;
			return this;
		}

		public CleanPopulationBuilder setRmLegs(Set<String> rmLegs) {
			this.rmLegs = rmLegs;
			return this;
		}

		public CleanPopulation createCleanPopulation() {
			return new CleanPopulation(rmUnselected, rmActivityLocations, rmActivityFacilities, rmRoutes, tripsToLegs, rmLegs);
		}
	}
}
