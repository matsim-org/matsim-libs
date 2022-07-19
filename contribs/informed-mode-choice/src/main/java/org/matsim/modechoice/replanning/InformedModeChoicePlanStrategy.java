package org.matsim.modechoice.replanning;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.SubtourModeChoiceConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.core.replanning.selectors.RandomUnscoredPlanSelector;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.modechoice.InformedModeChoiceConfigGroup;
import org.matsim.modechoice.PlanCandidate;
import org.matsim.modechoice.PlanModel;
import org.matsim.modechoice.search.TopKChoicesGenerator;

import javax.inject.Provider;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * The main strategy for informed mode choice.
 */
public class InformedModeChoicePlanStrategy implements PlanStrategy {

	private static final Logger log = LogManager.getLogger(InformedModeChoicePlanStrategy.class);

	private final RandomUnscoredPlanSelector<Plan, Person> unscored = new RandomUnscoredPlanSelector<>();

	private final InformedModeChoiceConfigGroup config;
	private final Config globalConfig;
	private final GeneratorContext[] threadContexts;
	private final SelectSingleTripModeStrategy.Algorithm[] singleTrip;
	private final Scenario scenario;
	private final OutputDirectoryHierarchy controlerIO;

	private final AtomicInteger counter = new AtomicInteger(0);

	/**
	 * Maps thread to array index of generators. Each threads will use its on generator exclusively.
	 */
	private ThreadLocal<Integer> assignment = ThreadLocal.withInitial(counter::getAndIncrement);

	private ExecutorService executor;

	private final IdMap<Person, PlanHistory> history;

	public InformedModeChoicePlanStrategy(Config config, Scenario scenario, OutputDirectoryHierarchy controlerIO,
	                                      Provider<GeneratorContext> generator) {
		this.globalConfig = config;
		this.config = ConfigUtils.addOrGetModule(config, InformedModeChoiceConfigGroup.class);
		this.scenario = scenario;
		this.controlerIO = controlerIO;


		SubtourModeChoiceConfigGroup smc = ConfigUtils.addOrGetModule(config, SubtourModeChoiceConfigGroup.class);

		List<String> nonChainBasedModes = this.config.getModes().stream()
				.filter(m -> !ArrayUtils.contains(smc.getChainBasedModes(), m))
				.collect(Collectors.toList());

		this.threadContexts = new GeneratorContext[config.global().getNumberOfThreads()];
		this.singleTrip = new SelectSingleTripModeStrategy.Algorithm[config.global().getNumberOfThreads()];

		for (int i = 0; i < this.threadContexts.length; i++) {
			GeneratorContext context = generator.get();
			this.threadContexts[i] = context;
			this.singleTrip[i] = SelectSingleTripModeStrategy.newAlgorithm(context.singleGenerator, context.selector, nonChainBasedModes);
		}

		history = new IdMap<>(Person.class, scenario.getPopulation().getPersons().size());
	}

	@Override
	public void init(ReplanningContext replanningContext) {

		final int writePlansInterval = globalConfig.controler().getWritePlansInterval();
		final int lastIteration = globalConfig.controler().getLastIteration();


		if (replanningContext.getIteration() == lastIteration ||
				(writePlansInterval > 0 && (replanningContext.getIteration() % writePlansInterval == 0))
		) {
			writeEstimates(replanningContext.getIteration());
			writeHistory(replanningContext.getIteration());
		}

		// Only for debugging
		for (Person person : scenario.getPopulation().getPersons().values()) {
			Plan plan = person.getSelectedPlan();

			PlanHistory hist = history.computeIfAbsent(person.getId(), PlanHistory::new);
			hist.add(plan);
		}

		// Thread local stores which thread uses which generator
		this.counter.set(0);
		this.assignment = ThreadLocal.withInitial(counter::getAndIncrement);
		this.executor = Executors.newWorkStealingPool(globalConfig.global().getNumberOfThreads());
	}

	private void writeEstimates(int iteration) {
		String f = controlerIO.getIterationFilename(iteration, "scoreEstimates.tsv.gz");

		boolean explainScores = globalConfig.planCalcScore().isExplainScores();

		try (CSVPrinter csv = new CSVPrinter(IOUtils.getBufferedWriter(f), CSVFormat.MONGODB_TSV)) {

			csv.print("person");
			csv.print("subpopulation");
			csv.print("plan");
			csv.print("type");
			csv.print("score");
			if (explainScores)
				csv.print(ScoringFunction.SCORE_EXPLANATION);

			csv.print(PlanCandidate.ESTIMATE_ATTR);

			for (String mode : config.getModes()) {
				csv.print(mode);
			}
			csv.println();

			for (Person person : scenario.getPopulation().getPersons().values()) {

				int i = 0;
				for (Plan plan : person.getPlans()) {

					csv.print(person.getId());
					csv.print(PopulationUtils.getSubpopulation(person));
					csv.print(i++);
					csv.print(plan.getType());
					csv.print(plan.getScore());
					if (explainScores)
						csv.print(plan.getAttributes().getAttribute(ScoringFunction.SCORE_EXPLANATION));

					csv.print(plan.getAttributes().getAttribute(PlanCandidate.ESTIMATE_ATTR));

					for (String mode : config.getModes()) {
						csv.print(StringUtils.countMatches(plan.getType(), mode));
					}

					csv.println();
				}
			}

		} catch (IOException e) {
			log.error("Could not write Estimate tsv", e);
		}
	}

	private void writeHistory(int iteration) {

		String f = controlerIO.getIterationFilename(iteration, "estimateHistory.tsv.gz");

		try (CSVPrinter csv = new CSVPrinter(IOUtils.getBufferedWriter(f), CSVFormat.MONGODB_TSV)) {

			csv.printRecord("person", "type", "scoreHistory", "estimateHistory");

			for (Person person : scenario.getPopulation().getPersons().values()) {

				PlanHistory hist = history.get(person.getId());

				for (String type : hist.types()) {
					csv.printRecord(person.getId(), type,
							hist.getScores(type).doubleStream().mapToObj(String::valueOf).collect(Collectors.joining(";")),
							hist.getEstimates(type).doubleStream().mapToObj(String::valueOf).collect(Collectors.joining(";"))
					);
				}
			}

		} catch (IOException e) {
			log.error("Could not estimate history tsv", e);
		}

	}

	@Override
	public void run(HasPlansAndId<Plan, Person> person) {

		Plan unscored = this.unscored.selectPlan(person);

		// If there are unscored plans, they need to be executed first
		if (unscored != null) {
			person.setSelectedPlan(unscored);
			return;
		}

/*
		Plan best = person.getPlans().stream()
				.filter(p -> p.getAttributes().getAttribute(PlanCandidate.MAX_ESTIMATE) != null)
				.max(Comparator.comparingDouble(Plan::getScore)).orElse(null);

		if (best != null) {


			double bestEstimate = (double) best.getAttributes().getAttribute(PlanCandidate.MAX_ESTIMATE);
			double[] baseLine = PlanCandidate.occurrences(config.getModes(), best.getType());

			// Collect for each plan the differences and the present of modes and differences to best

			// TODO: one agents plan might deviate stronger than the mean

			for (Plan plan : person.getPlans()) {

				Double maxEstimate = (Double) plan.getAttributes().getAttribute(PlanCandidate.MAX_ESTIMATE);
				if (maxEstimate == null || plan == best)
					continue;

				double[] row = PlanCandidate.occurrences(config.getModes(), plan.getType());

				// Difference in execution should ideally be the same as difference in estimation
				double y = (best.getScore() - plan.getScore()) - (bestEstimate - maxEstimate);
				// if y is negative the score was underestimated

				for (int i = 0; i < baseLine.length; i++) {
					reg[i].addData(row[i] - baseLine[i], y);
				}
			}

		}
 */

		//Plan best = person.getPlans().stream().max(Comparator.comparingDouble(Plan::getScore)).orElseThrow();
		executor.submit(new ReplanningTask(person));
	}

	private void applyCandidates(HasPlansAndId<Plan, Person> person, Collection<PlanCandidate> candidates) {

		for (PlanCandidate c : candidates) {

			List<? extends Plan> same = person.getPlans().stream()
					.filter(p -> c.getPlanType().equals(p.getType()))
					.collect(Collectors.toList());

			// if this type exists, overwrite estimates
			if (!same.isEmpty()) {
				same.forEach(c::applyAttributes);

			} else {

				Plan plan = person.createCopyOfSelectedPlanAndMakeSelected();
				c.applyTo(plan);
				plan.setType(c.getPlanType());

				plan.getAttributes().removeAttribute(ScoringFunction.SCORE_EXPLANATION);

				// TODO: If this plan is new, it needs to be routed
			}
		}
	}

	@Override
	public void finish() {

		executor.shutdown();

		try {
			boolean b = executor.awaitTermination(1, TimeUnit.HOURS);

			if (!b) {
				log.error("Not all replanning tasks could finish");
				throw new RuntimeException("Not all replanning tasks could finish");
			}

		} catch (InterruptedException e) {
			log.error("Not all replanning tasks could finish", e);
			throw new RuntimeException(e);
		}
	}

	private final class ReplanningTask implements Runnable {

		private final HasPlansAndId<Plan, Person> person;
		private final Random rnd;

		private ReplanningTask(HasPlansAndId<Plan, Person> person) {
			this.person = person;
			this.rnd = MatsimRandom.getLocalInstance();
		}

		@Override
		public void run() {

			try {
				plan();
			} catch (Throwable t) {
				log.error("Thread threw an exception", t);
				throw t;
			}

		}

		private void plan() {

			Set<String> missing = new HashSet<>();
			// First fill missing plan types
			for (Plan plan : person.getPlans())
				if (plan.getAttributes().getAttribute(PlanCandidate.TYPE_ATTR) == null) {
					String guess = PlanCandidate.guessPlanType(plan, config.getModes());

					missing.add(guess);
					plan.setType(guess);
				}

			Plan best = person.getPlans().stream()
					.max(Comparator.comparingDouble(Plan::getScore)).orElse(null);

			person.setSelectedPlan(best);

			GeneratorContext tc = threadContexts[assignment.get()];

			PlanModel model = PlanModel.newInstance(best);

			Collection<PlanCandidate> results = tc.generator.generate(model, null, null, config.getTopK(), 0);

			for (PlanCandidate c : results) {
				missing.remove(c.getPlanType());
			}

			// TODO: should replace old plans with same type
			// TODO: should remove and not regenerate unpromising plan types

			applyCandidates(person, results);

			// Estimate missing scores for already exising plans
			if (!missing.isEmpty()) {

				List<PlanCandidate> predefined = tc.generator.generatePredefined(model, missing.stream().map(PlanCandidate::createModeArray).collect(Collectors.toList()));
				applyCandidates(person, predefined);
			}


			person.setSelectedPlan(person.getPlans().get(rnd.nextInt(person.getPlans().size())));
		}
	}

	private static final class PlanHistory {

		private final Map<String, DoubleList> scores = new HashMap<>();
		private final Map<String, DoubleList> estimates = new HashMap<>();

		public PlanHistory(Id<Person> id) {
		}

		public void add(Plan plan) {

			Object estimate = plan.getAttributes().getAttribute(PlanCandidate.ESTIMATE_ATTR);

			if (plan.getType() == null || estimate == null || plan.getScore() == null)
				return;

			scores.computeIfAbsent(plan.getType(), k -> new DoubleArrayList()).add((double) plan.getScore());
			estimates.computeIfAbsent(plan.getType(), k -> new DoubleArrayList()).add((double) estimate);
		}

		public Set<String> types() {
			return scores.keySet();
		}

		public DoubleList getScores(String type) {
			return scores.get(type);
		}

		public DoubleList getEstimates(String type) {
			return estimates.get(type);
		}
	}
}
