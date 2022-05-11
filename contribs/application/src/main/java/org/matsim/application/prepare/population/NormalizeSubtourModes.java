package org.matsim.application.prepare.population;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;

@CommandLine.Command(
		name = "normalize-subtour-modes",
		description = "Adjust subtour modes so that one subtour can not mix chain-based with other modes and all subtours are mass-conserving.",
		showDefaultValues = true
)
public class NormalizeSubtourModes implements MATSimAppCommand {


	@CommandLine.Option(names = "--input", description = "Path to input population", required = true)
	private Path input;

	@CommandLine.Option(names = "--output", description = "Path for output population", required = true)
	private Path output;

	@CommandLine.Option(names = "--chain-based-modes", description = "Chain-based modes", defaultValue = "car,bike", split = ",")
	private Set<String> chainBasedModes;

	@CommandLine.Option(names = "--modes", description = "New modes to choose from", defaultValue = "walk", split = ",")
	private List<String> modes;

	public static void main(String[] args) {
		new NormalizeSubtourModes().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		Population population = PopulationUtils.readPopulation(input.toString());

		SplittableRandom rnd = new SplittableRandom(4117);

		if (output.getParent() != null)
			Files.createDirectories(output.getParent());

		for (Person person : population.getPersons().values()) {

			String mode = modes.get(rnd.nextInt(modes.size()));

			Collection<TripStructureUtils.Subtour> subtours = TripStructureUtils.getSubtours(person.getSelectedPlan());

			for (TripStructureUtils.Subtour subtour : subtours) {

				for (TripStructureUtils.Trip trip : subtour.getTrips()) {

					for (Leg leg : trip.getLegsOnly()) {

						leg.setRoute(null);
						leg.setMode(mode);
						TripStructureUtils.setRoutingMode(leg, mode);
					}
				}
			}
		}

		PopulationUtils.writePopulation(population, output.toString());

		return 0;
	}

}
