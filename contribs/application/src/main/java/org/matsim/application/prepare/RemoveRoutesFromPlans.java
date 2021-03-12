package org.matsim.application.prepare;

import com.google.common.collect.Lists;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.population.PopulationUtils;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * Removes route information from a plans file.
 *
 * @author rakow
 */
@CommandLine.Command(
        name = "removeRoutesFromPlans",
        description = "Remove route information from a plans file.",
        showDefaultValues = true
)
public class RemoveRoutesFromPlans implements Callable<Integer> {

    @CommandLine.Option(names = "--plans", description = "Input original plan file", required = true)
    private Path plans;

    @CommandLine.Option(names = "--keep-selected", description = "Keep only the selected plan.", defaultValue = "false")
    private boolean keepOnlySelected;

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

		for (Person person : population.getPersons().values()) {

			if (keepOnlySelected) {
				Plan selected = person.getSelectedPlan();
				for (Plan plan : Lists.newArrayList(person.getPlans())) {
					if (plan != selected)
						person.removePlan(plan);
				}
			}

			for (Plan plan : person.getPlans()) {
				for (PlanElement el : plan.getPlanElements()) {
					if (el instanceof Leg) {
						((Leg) el).setRoute(null);
					}
				}
			}
		}

        PopulationUtils.writePopulation(population, output.toString());

        return 0;
    }
}
