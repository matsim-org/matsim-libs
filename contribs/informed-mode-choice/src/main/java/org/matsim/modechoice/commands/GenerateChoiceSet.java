package org.matsim.modechoice.commands;


import com.google.inject.Inject;
import com.google.inject.Injector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.MATSimApplication;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.ParallelPersonAlgorithmUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.modechoice.InformedModeChoiceConfigGroup;
import org.matsim.modechoice.PlanCandidate;
import org.matsim.modechoice.search.TopKChoicesGenerator;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@CommandLine.Command(
		name = "generate-choice-set",
		description = "Generate a static mode-choice set for all agents in the population"
)
public class GenerateChoiceSet implements MATSimAppCommand, PersonAlgorithm {

	private static final Logger log = LogManager.getLogger(GenerateChoiceSet.class);

	@CommandLine.Option(names = "--config", description = "Path to scenario config", required = true)
	private Path configPath;

	@CommandLine.Option(names = "--scenario", description = "Full qualified classname of the MATSim application scenario class. The IMC modules must be specified there.", required = true)
	private Class<? extends MATSimApplication> scenario;

	@CommandLine.Option(names = "--population", description = "Path to input population")
	private Path populationPath;

	@CommandLine.Option(names = "--subpopulation", description = "Subpopulation filter", defaultValue = "person")
	private String subpopulation;

	@CommandLine.Option(names = "--top-k", description = "Use top k estimates", defaultValue = "5")
	private int topK;

	@CommandLine.Option(names = "--modes", description = "Modes to include in estimation", defaultValue = "car,walk,bike,pt,ride", split = ",")
	private Set<String> modes;

	@CommandLine.Option(names = "--output", description = "Path for output population", required = true)
	private Path output;


	// TODO: option for time dependent network

	@Inject
	private TopKChoicesGenerator generator;

	public static void main(String[] args) {
		new GenerateChoiceSet().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		Config config = ConfigUtils.loadConfig(configPath.toString());

		if (populationPath != null)
			config.plans().setInputFile(populationPath.toString());

		config.controler().setLastIteration(0);
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);

		InformedModeChoiceConfigGroup imc = ConfigUtils.addOrGetModule(config, InformedModeChoiceConfigGroup.class);

		imc.setTopK(topK);
		imc.setModes(modes);

		Controler controler = MATSimApplication.prepare(scenario, config);

		// THis is currently needed because vehicle id mapping needs to be initialized
		controler.run();

		Injector injector = controler.getInjector();
		injector.injectMembers(this);

		log.info("Estimating choice set...");

		ParallelPersonAlgorithmUtils.run(controler.getScenario().getPopulation(), config.global().getNumberOfThreads(), this);

		PopulationUtils.writePopulation(controler.getScenario().getPopulation(), output.toString());

		return 0;
	}

	@Override
	public void run(Person person) {

		String subpop = PopulationUtils.getSubpopulation(person);
		if (subpopulation != null && !subpop.equals(subpopulation))
			return;

		Plan plan = person.getSelectedPlan();

		Collection<PlanCandidate> candidates = generator.generate(plan);

		// remove all unselected plans
		Set<Plan> plans = new HashSet<>(person.getPlans());
		plans.remove(plan);
		plans.forEach(person::removePlan);

		for (PlanCandidate c : candidates) {

			if (plan == null)
				plan = person.createCopyOfSelectedPlanAndMakeSelected();

			c.applyTo(plan);
			plan = null;
		}
	}
}
