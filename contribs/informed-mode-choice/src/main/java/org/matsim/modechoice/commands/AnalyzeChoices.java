package org.matsim.modechoice.commands;

import it.unimi.dsi.fastutil.objects.Object2DoubleArrayMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.population.PopulationUtils;
import org.matsim.modechoice.PlanCandidate;
import org.matsim.modechoice.PlanModel;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@CommandLine.Command(
		name = "analyze-choices",
		description = "Write plan and choice output to tsv."
)
public class AnalyzeChoices implements MATSimAppCommand {

	@CommandLine.Parameters(paramLabel = "INPUT", arity = "1")
	private List<Path> input;

	@CommandLine.Option(names = "--modes", description = "Modes to include for distance estimate", defaultValue = "car,walk,bike,pt,ride", split = ",")
	private Set<String> modes;

	@CommandLine.Option(names = "--subpopulation", description = "Subpopulation filter", defaultValue = "person")
	private String subpopulation;
	@CommandLine.Option(names = "--output", description = "Output tsv", required = true)
	private Path output;


	public static void main(String[] args) {
		new AnalyzeChoices().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		Population population = PopulationUtils.readPopulation(input.get(0).toString());

		try (CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(output), CSVFormat.MONGODB_TSV)) {

			List<String> header = ObjectArrayList.of("person", "k", "selected", "score", "type", "estimate");

			for (String mode : modes) {
				header.add(mode + "_dist");
			}

			printer.printRecord(header);

			Object2DoubleMap<String> dists = new Object2DoubleArrayMap<>();

			for (Person person : population.getPersons().values()) {

				if (!subpopulation.isEmpty() && !subpopulation.equals(PopulationUtils.getSubpopulation(person)))
					continue;

				List<Plan> plans = person.getPlans().stream()
						.filter(p -> p.getAttributes().getAttribute(PlanCandidate.ESTIMATE_ATTR) != null)
						.sorted(Comparator.comparingDouble(p -> -(double) p.getAttributes().getAttribute(PlanCandidate.ESTIMATE_ATTR)))
						.collect(Collectors.toList());

				Map<String, Plan> types = new HashMap<>();
				Set<Plan> toRemove = new HashSet<>();

				// make sure plan types are distinct
				for (Plan plan : plans) {
					// make sure to always keep the selected plan
					if (person.getSelectedPlan() == plan) {
						if (types.containsKey(plan.getType())) {
							toRemove.add(types.get(plan.getType()));
						} else
							types.put(plan.getType(), plan);
					} else {
						if (types.containsKey(plan.getType()))
							toRemove.add(plan);
						else
							types.put(plan.getType(), plan);
					}
				}

				plans.removeAll(toRemove);

				dists.clear();

				for (int k = 0; k < plans.size(); k++) {

					Plan plan = plans.get(k);

					PlanModel model = PlanModel.newInstance(plan);

					// Distance stats over all modes
					for (String m : modes) {
						double dist = 0;
						String[] planModes = model.getCurrentModesMutable();
						for (int i = 0; i < planModes.length; i++) {
							if (m.equals(planModes[i]))
								dist += model.distance(i);
						}
						dists.put(m, dist);
					}

					ObjectArrayList<Object> record = ObjectArrayList.of(person.getId(), k, person.getSelectedPlan() == plan ? 1 : 0, plan.getScore(),
							plan.getType(), plan.getAttributes().getAttribute(PlanCandidate.ESTIMATE_ATTR));

					record.addAll(dists.values());

					printer.printRecord(record);
				}
			}
		}

		return 0;
	}
}
