package org.matsim.application.prepare.population;

import com.google.common.collect.Lists;
import org.matsim.api.core.v01.population.*;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.TripsToLegsAlgorithm;
import org.matsim.core.router.DefaultAnalysisMainModeIdentifier;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Removes route information from a plans file.
 *
 * @author rakow
 * @deprecated Use {@link CleanPopulation}
 */
@CommandLine.Command(
        name = "remove-routes",
        description = "Remove route information from a plans file.",
        showDefaultValues = true
)
@Deprecated
public class RemoveRoutesFromPlans implements MATSimAppCommand {

    @CommandLine.Option(names = "--plans", description = "Input original plan file", required = true)
    private Path plans;

    @CommandLine.Option(names = "--keep-selected", description = "Keep only the selected plan.", defaultValue = "false")
    private boolean keepOnlySelected;

	@CommandLine.Option(names = "--clean-activities", description = "Remove link and facility from activities", defaultValue = "false")
    private boolean cleanActivities;

    @CommandLine.Option(names = "--output", description = "Output file name")
    private Path output;

    public static void main(String[] args) {
        System.exit(new CommandLine(new RemoveRoutesFromPlans()).execute(args));
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

			if (keepOnlySelected) {
				Plan selected = person.getSelectedPlan();
				for (Plan plan : Lists.newArrayList(person.getPlans())) {
					if (plan != selected)
						person.removePlan(plan);
				}
			}

			for (Plan plan : person.getPlans()) {
				trips2Legs.run(plan);

				for (PlanElement el : plan.getPlanElements()) {
					if (el instanceof Leg) {
						((Leg) el).setRoute(null);
					}

					if (cleanActivities) {
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
