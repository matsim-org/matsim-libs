package org.matsim.modechoice.commands;


import com.google.inject.Injector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ScenarioOptions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.ParallelPersonAlgorithmUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.modechoice.InformedModeChoiceConfigGroup;
import org.matsim.modechoice.PlanCandidate;
import org.matsim.modechoice.PlanModel;
import org.matsim.modechoice.search.TopKChoicesGenerator;
import picocli.CommandLine;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@CommandLine.Command(
	name = "generate-choice-set",
	description = "Generate a static mode-choice set for all agents in the population"
)
public class GenerateChoiceSet implements MATSimAppCommand, PersonAlgorithm {

	private static final Logger log = LogManager.getLogger(GenerateChoiceSet.class);

	@CommandLine.Mixin
	private ScenarioOptions scenario;

	@CommandLine.Option(names = "--subpopulation", description = "Subpopulation filter", defaultValue = "person")
	private String subpopulation;

	@CommandLine.Option(names = "--top-k", description = "Use top k estimates")
	private Integer topK;

	@CommandLine.Option(names = "--modes", description = "Modes to include in estimation", split = ",")
	private Set<String> modes;

	@CommandLine.Option(names = "--pruning", description = "Pruning to use. Disabled by default.")
	private String pruning;

	@CommandLine.Option(names = "--keep-selected-plan", description = "Keep selected plan in choice set", defaultValue = "false")
	private boolean keepSelectedPlan;

	@CommandLine.Option(names = "--output", description = "Path for output population", required = true)
	private Path output;

	@CommandLine.Option(names = "--output-dist", description = "Write estimation distribution to output. Filename is derived from output", defaultValue = "false")
	private boolean outputDist;

	private Writer distWriter;

	private ThreadLocal<TopKChoicesGenerator> generatorCache;
	private AtomicInteger counter = new AtomicInteger();

	public static void main(String[] args) {
		new GenerateChoiceSet().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		Config config = scenario.getConfig();

		config.controller().setLastIteration(0);
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);

		InformedModeChoiceConfigGroup imc = ConfigUtils.addOrGetModule(config, InformedModeChoiceConfigGroup.class);

		Controler controler = scenario.createControler();

		log.info("Using k={}, pruning={}", topK, pruning);

		if (topK != null)
			imc.setTopK(topK);

		if (modes != null)
			imc.setModes(modes);

		imc.setPruning(pruning);

		Injector injector = controler.getInjector();

		generatorCache = ThreadLocal.withInitial(() -> injector.getInstance(TopKChoicesGenerator.class));

		int persons = 0;

		// copy the original plan, so no modifications are made
		for (Person person : controler.getScenario().getPopulation().getPersons().values()) {
			String subpop = PopulationUtils.getSubpopulation(person);
			if (subpopulation != null && !subpop.equals(subpopulation))
				continue;

			Plan selected = person.getSelectedPlan();
			selected.setScore(null);

			Plan copy = person.createCopyOfSelectedPlanAndMakeSelected();
			copy.setType("source");

			person.setSelectedPlan(selected);
			persons++;
		}

		// This is currently needed because vehicle id mapping needs to be initialized
		controler.run();

		injector.injectMembers(this);

		log.info("Estimating choice set...");

		if (outputDist) {

			String name = output.getFileName().toString().replace(".gz", "").replace(".xml", ".tsv");
			Path out = output.getParent().resolve(name);

			log.info("Writing output distribution to {}", out);

			distWriter = Files.newBufferedWriter(out);
			distWriter.write("person\tn\testimates\n");
		}

		counter.set(0);

		ParallelPersonAlgorithmUtils.run(controler.getScenario().getPopulation(), config.global().getNumberOfThreads(), this);

		PopulationUtils.writePopulation(controler.getScenario().getPopulation(), output.toString());

		if (distWriter != null)
			distWriter.close();

		log.info("Generated {} plans for {} persons, (avg. {})", counter.get(), persons, counter.get() / (double) persons);

		return 0;
	}

	@Override
	public void run(Person person) {

		String subpop = PopulationUtils.getSubpopulation(person);
		if (subpopulation != null && !subpop.equals(subpopulation))
			return;

		Plan plan = person.getPlans().stream().filter(p -> "source".equals(p.getType())).findFirst().orElseThrow();

		PlanModel model = PlanModel.newInstance(plan);

		TopKChoicesGenerator generator = generatorCache.get();
		Collection<PlanCandidate> candidates = generator.generate(model, null, null);

		// remove all other plans, except source
		Set<Plan> plans = new HashSet<>(person.getPlans());
		plans.remove(plan);
		plans.forEach(person::removePlan);

		// This will result in a copy of the original plan created
		if (keepSelectedPlan)
			plan = null;

		for (PlanCandidate c : candidates) {

			counter.incrementAndGet();

			if (plan == null)
				plan = person.createCopyOfSelectedPlanAndMakeSelected();

			c.applyTo(plan);
			plan.setType(c.getPlanType());
			plan = null;
		}

		if (distWriter != null) {

			// the writer is synchronized
			try {
				distWriter.write(person.getId() + "\t" + candidates.size() + "\t" +
					candidates.stream()
						.map(PlanCandidate::getUtility).map(String::valueOf)
						.collect(Collectors.joining(";")) +
					"\n");

			} catch (IOException e) {
				throw new RuntimeException(e);
			}

		}

	}
}
