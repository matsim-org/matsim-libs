package org.matsim.application.analysis.population;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.analysis.DefaultAnalysisMainModeIdentifier;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.ChooseRandomLegModeForSubtour;
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.core.router.TripStructureUtils;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.*;

@CommandLine.Command(
		name = "subtours",
		description = "Analyze subtours of a population."
)
public class SubTourAnalysis implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(SubTourAnalysis.class);

	@CommandLine.Parameters(arity = "1", description = "Path to population")
	private Path input;

	@CommandLine.Option(names = "--chain-modes", description = "Chain-based modes", defaultValue = "car,bike", split = ",")
	private List<String> chainBasedModes;

	@Override
	public Integer call() throws Exception {

		Population population = PopulationUtils.readPopulation(input.toString());

		List<TripStructureUtils.Subtour> subtours = new ArrayList<>();

		Set<String> modes = new HashSet<>();

		for (Person agent : population.getPersons().values()) {
			Plan plan = agent.getSelectedPlan();

			List<Leg> legs = TripStructureUtils.getLegs(plan);

			for (Leg leg : legs) {
				modes.add(leg.getMode());
			}

			subtours.addAll(TripStructureUtils.getSubtours(plan));
		}

		log.info("Detected modes: {}", modes);

		ChooseRandomLegModeForSubtour strategy = new ChooseRandomLegModeForSubtour(new DefaultAnalysisMainModeIdentifier(), plan -> modes,
				modes.toArray(new String[0]), chainBasedModes.toArray(new String[0]), new Random(1234), SubtourModeChoice.Behavior.fromAllModesToSpecifiedModes, 0.0);


		return 0;
	}
}
