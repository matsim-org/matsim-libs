package org.matsim.application.prepare.population;

import com.google.common.collect.Lists;
import org.matsim.analysis.DefaultAnalysisMainModeIdentifier;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.TripsToLegsAlgorithm;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * Removes information from a popoulation file.
 *
 * @author rakow
 */
@CommandLine.Command(
        name = "clean-population",
        description = "Remove information from population, such as routes or unselected plans.",
        showDefaultValues = true
)
public class CleanPopulation implements Callable<Integer> {

    @CommandLine.Option(names = "--plans", description = "Input original plan file", required = true)
    private Path plans;

    @CommandLine.Option(names = "--remove-unselected-plans", description = "Keep only the selected plan.", defaultValue = "false")
    private boolean rmUnselected;

	@CommandLine.Option(names = "--remove-activity-location", description = "Remove link and facility from activities", defaultValue = "false")
    private boolean rmActivityLocations;

	@CommandLine.Option(names = "--remove-routes", description = "Remove route information", defaultValue = "false")
	private boolean rmRoutes;

    @CommandLine.Option(names = "--output", description = "Output file name", required = true)
    private Path output;

    public static void main(String[] args) {
        System.exit(new CommandLine(new CleanPopulation()).execute(args));
    }

    @Override
    public Integer call() throws Exception {

		Population population = PopulationUtils.readPopulation(plans.toString());

		if (output == null)
			output = Path.of(plans.toAbsolutePath().toString().replace(".xml", "-no-routes.xml"));

		Files.createDirectories(output.getParent());
		
		// Using the analysis main mode identifier instead of the routing mode based one on purpose
		// to be able to process older population files without any routing modes!
		TripsToLegsAlgorithm trips2Legs = new TripsToLegsAlgorithm(new DefaultAnalysisMainModeIdentifier());

		for (Person person : population.getPersons().values()) {

			if (rmUnselected) {
				Plan selected = person.getSelectedPlan();
				for (Plan plan : Lists.newArrayList(person.getPlans())) {
					if (plan != selected)
						person.removePlan(plan);
				}
			}

			for (Plan plan : person.getPlans()) {
				trips2Legs.run(plan);

				for (PlanElement el : plan.getPlanElements()) {
					if (rmRoutes) {
						if (el instanceof Leg) {
							((Leg) el).setRoute(null);
						}
					}

					if (rmActivityLocations) {
						if (el instanceof Activity) {
							((Activity) el).setLinkId(null);
							((Activity) el).setFacilityId(null);
						}
					}
				}
			}
		}

        PopulationUtils.writePopulation(population, output.toString());

        return 0;
    }
}
