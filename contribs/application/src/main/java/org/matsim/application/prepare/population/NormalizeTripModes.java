package org.matsim.application.prepare.population;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.*;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@CommandLine.Command(name = "normalize-trip-modes", description = "Adjust trip modes to be the same throughout the day.", showDefaultValues = true)
public class NormalizeTripModes implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(NormalizeTripModes.class);

	@CommandLine.Option(names = "--input", description = "Path to input population", required = true)
	private Path input;

	@CommandLine.Option(names = "--output", description = "Path for output population", required = true)
	private Path output;

	@CommandLine.Option(names = "--subpopulation", description = "Subpopulation filter", defaultValue = "person")
	private String subpopulation;

	@CommandLine.Option(names = "--chain-based-modes", description = "Chain-based modes", defaultValue = "car,bike", split = ",")
	private Set<String> chainBasedModes;

	@CommandLine.Option(names = "--modes", description = "Modes to choose from.", defaultValue = "walk", split = ",")
	private List<String> modes;

	@CommandLine.Option(names = "--duplicate-per-mode", description = "Duplicate selected plan for each mode", defaultValue = "false")
	private boolean perMode;

	@CommandLine.Option(names = "--remove-staging-activities", description = "Remove all staging activities.", defaultValue = "false")
	private boolean rmStaging;

	private final SplittableRandom rnd = new SplittableRandom(4117);

	public static void main(String[] args) {
		new NormalizeTripModes().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		Population population = PopulationUtils.readPopulation(input.toString());

		if (output.getParent() != null) Files.createDirectories(output.getParent());

		int processed = 0;
		int skipped = 0;

		for (Person person : population.getPersons().values()) {

			String subpop = PopulationUtils.getSubpopulation(person);
			if (!subpop.equals(subpopulation)) continue;

			if (perMode)
				duplicatePlans(person);
			else if (rmStaging)
				skipped += selectModeAndClean(population.getFactory(), person) ? 0 : 1;
			else
				skipped += selectMode(person) ? 0 : 1;

			processed++;
		}

		log.info("Processed {} out of {} persons", processed, population.getPersons().size());

		if (skipped > 0)
			log.warn("Skipped {} persons due to car availability", skipped);


		PopulationUtils.writePopulation(population, output.toString());

		return 0;
	}

	private void duplicatePlans(Person person) {

		Plan plan = person.getSelectedPlan();

		// remove all unselected plans
		Set<Plan> plans = new HashSet<>(person.getPlans());
		plans.remove(plan);
		plans.forEach(person::removePlan);

		for (String mode : modes) {

			if (!PersonUtils.canUseCar(person) && mode.equals("car"))
				continue;

			plan.setType(mode);

			for (TripStructureUtils.Trip trip : TripStructureUtils.getTrips(plan)) {
				for (Leg leg : trip.getLegsOnly()) {

					leg.setRoute(null);
					leg.setMode(mode);
					TripStructureUtils.setRoutingMode(leg, mode);
				}

			}

			plan = person.createCopyOfSelectedPlanAndMakeSelected();
		}

	}

	private boolean selectMode(Person person) {

		List<String> select = new ArrayList<>(modes);

		if (!PersonUtils.canUseCar(person))
			select.removeIf(m -> m.equals("car"));

		if (select.isEmpty()) {
			// No modes to select from. Probably tried to set to all car plans, but person is not allowed to use car."
			return false;
		}

		String mode = select.get(rnd.nextInt(select.size()));

		for (TripStructureUtils.Trip trip : TripStructureUtils.getTrips(person.getSelectedPlan())) {
			for (Leg leg : trip.getLegsOnly()) {
				leg.setRoute(null);
				leg.setMode(mode);
				TripStructureUtils.setRoutingMode(leg, mode);
			}
		}

		return true;
	}

	private boolean selectModeAndClean(PopulationFactory f, Person person) {

		List<String> select = new ArrayList<>(modes);

		if (!PersonUtils.canUseCar(person))
			select.removeIf(m -> m.equals("car"));

		if (select.isEmpty()) {
			// No modes to select from. Probably tried to set to all car plans, but person is not allowed to use car."
			return false;
		}

		Plan plan = person.getSelectedPlan();
		plan.setScore(null);

		String mode = select.get(rnd.nextInt(select.size()));

		List<Activity> activities = TripStructureUtils.getActivities(plan, TripStructureUtils.StageActivityHandling.ExcludeStageActivities);

		plan.getPlanElements().clear();

		for (int i = 0; i < activities.size() - 1; i++) {
			plan.addActivity(activities.get(i));
			plan.addLeg(f.createLeg(mode));
		}

		plan.addActivity(activities.get(activities.size() - 1));

		return true;
	}
}
