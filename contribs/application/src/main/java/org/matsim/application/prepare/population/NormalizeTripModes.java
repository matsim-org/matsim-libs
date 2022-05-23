package org.matsim.application.prepare.population;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@CommandLine.Command(
		name = "normalize-trip-modes",
		description = "Adjust subtour modes so that one subtour can not mix chain-based with other modes and all subtours are mass-conserving.",
		showDefaultValues = true
)
public class NormalizeTripModes implements MATSimAppCommand {


	@CommandLine.Option(names = "--input", description = "Path to input population", required = true)
	private Path input;

	@CommandLine.Option(names = "--output", description = "Path for output population", required = true)
	private Path output;

	@CommandLine.Option(names = "--chain-based-modes", description = "Chain-based modes", defaultValue = "car,bike", split = ",")
	private Set<String> chainBasedModes;

	@CommandLine.Option(names = "--modes", description = "Create a plan for each mode", defaultValue = "walk", split = ",")
	private List<String> modes;

	public static void main(String[] args) {
		new NormalizeTripModes().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		Population population = PopulationUtils.readPopulation(input.toString());

		if (output.getParent() != null)
			Files.createDirectories(output.getParent());

		for (Person person : population.getPersons().values()) {

			Plan plan = person.getSelectedPlan();

			// remove all unselected plans
			Set<Plan> plans = new HashSet<>(person.getPlans());
			plans.remove(plan);
			plans.forEach(person::removePlan);

			for (String mode : modes) {

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

		PopulationUtils.writePopulation(population, output.toString());

		return 0;
	}

}
