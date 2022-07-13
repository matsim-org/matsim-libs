package org.matsim.modechoice.commands;


import com.google.common.base.Joiner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.config.groups.SubtourModeChoiceConfigGroup;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.ParallelPersonAlgorithmUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.modechoice.EstimatorContext;
import org.matsim.modechoice.PlanModel;
import org.matsim.modechoice.constraints.RelaxedMassConservationConstraint;
import org.matsim.modechoice.constraints.RelaxedSubtourConstraint;
import picocli.CommandLine;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@CommandLine.Command(
		name = "count-choices",
		description = "Compute statistics for each agents choice set.",
		mixinStandardHelpOptions = true
)
public class CountChoices implements MATSimAppCommand, PersonAlgorithm {

	private static final Logger log = LogManager.getLogger(CountChoices.class);

	@CommandLine.Option(names = "--population", description = "Path to input population", required = true)
	private Path populationPath;

	@CommandLine.Option(names = "--subpopulation", description = "Subpopulation filter", defaultValue = "person")
	private String subpopulation;

	@CommandLine.Option(names = "--modes", description = "Modes to include in estimation", defaultValue = "car,walk,bike,pt,ride", split = ",")
	private Set<String> modes;

	@CommandLine.Option(names = "--chain-based-modes", description = "Chain-based modes", defaultValue = "car,bike", split = ",")
	private Set<String> chainBasedModes;

	@CommandLine.Option(names = "--max-choices", description = "Maximum number of choices to analyze", defaultValue = "10000000")
	private int maxChoices;

	@CommandLine.Option(names = "--output", description = "Path for output population", required = true)
	private Path output;

	private Writer writer;

	private RelaxedSubtourConstraint st;
	private RelaxedMassConservationConstraint mc;

	private final Joiner on = Joiner.on("\t");

	public static void main(String[] args) {
		new CountChoices().execute(args);
	}

	@Override
	public Integer call() throws Exception {


		Population population = PopulationUtils.readPopulation(populationPath.toString());

		log.info("Writing output distribution to {}", output);

		writer = Files.newBufferedWriter(output);
		writer.write(on.join("person", "trips", "single_trip_choices", "subtour_choices", "mc_choices", "incomplete") + "\n");

		SubtourModeChoiceConfigGroup stConfig = new SubtourModeChoiceConfigGroup();

		modes = modes.stream().map(String::intern).collect(Collectors.toSet());
		stConfig.setChainBasedModes(chainBasedModes.stream().map(String::intern).toArray(String[]::new));

		st = new RelaxedSubtourConstraint(stConfig);
		mc = new RelaxedMassConservationConstraint(stConfig);

		ParallelPersonAlgorithmUtils.run(population, Runtime.getRuntime().availableProcessors(), this);

		writer.close();

		return 0;
	}

	@Override
	public void run(Person person) {

		String subpop = PopulationUtils.getSubpopulation(person);
		if (subpopulation != null && !subpop.equals(subpopulation))
			return;

		PlanModel trips = PlanModel.newInstance(person.getSelectedPlan());

		List<String> usableModes = new ArrayList<>(modes);
		if (!PersonUtils.canUseCar(person))
			usableModes.remove(TransportMode.car);

		int nModes = usableModes.size();
		int choices = (int) Math.pow(nModes, trips.trips());

		EstimatorContext context = new EstimatorContext(person, null);

		int[] stContext = st.getContext(context, trips);
		RelaxedMassConservationConstraint.Context mcContext = mc.getContext(context, trips);

		int st_choices = 0;
		int mc_choices = 0;

		String[] modes = new String[trips.trips()];
		int[] indices = new int[trips.trips()];

		int k = 0;
		do {
			for (int i = 0; i < modes.length; i++) {
				modes[i] = usableModes.get(indices[i]);
			}

			if (st.isValid(stContext, modes))
				st_choices++;

			if (mc.isValid(mcContext, modes))
				mc_choices++;

		} while (increment(indices, 0, usableModes.size()) && k++ < maxChoices);

		// on zero length modes, this will be incorrectly set to 1
		if (modes.length == 0) {
			st_choices = 0;
			mc_choices = 0;
		}

		try {
			// the writer is synchronized
			writer.write(on.join(person.getId(), trips.trips(),
					choices, st_choices, mc_choices, k >= maxChoices ? 1 : 0) + "\n");

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static boolean increment(int[] indices, int idx, int length) {

		if (indices.length == idx)
			return false;

		if (indices[idx] == length - 1) {
			indices[idx] = 0;
			return increment(indices, idx + 1, length);
		}

		indices[idx]++;
		return true;
	}

}
