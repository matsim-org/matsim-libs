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
import org.matsim.core.replanning.selectors.RandomUnscoredPlanSelector;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.modechoice.InformedModeChoiceConfigGroup;
import org.matsim.modechoice.PlanCandidate;
import org.matsim.modechoice.PlanModel;

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

	private final SubtourModeChoiceConfigGroup smc;
	private final Config globalConfig;
	private final GeneratorContext[] threadContexts;
	private final SelectSingleTripModeStrategy.Algorithm[] singleTrip;

	private final Set<String> nonChainBasedModes;

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


		smc = ConfigUtils.addOrGetModule(config, SubtourModeChoiceConfigGroup.class);
		nonChainBasedModes = this.config.getModes().stream()
				.filter(m -> !ArrayUtils.contains(smc.getChainBasedModes(), m))
				.collect(Collectors.toSet());

		this.threadContexts = new GeneratorContext[config.global().getNumberOfThreads()];
		this.singleTrip = new SelectSingleTripModeStrategy.Algorithm[config.global().getNumberOfThreads()];

		for (int i = 0; i < this.threadContexts.length; i++) {
			GeneratorContext context = generator.get();
			this.threadContexts[i] = context;
			this.singleTrip[i] = SelectSingleTripModeStrategy.newAlgorithm(context.singleGenerator, context.selector, context.pruner, nonChainBasedModes);
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

			PlanHistory hist = history.computeIfAbsent(person.getId(), k -> new PlanHistory(k, PlanModel.newInstance(plan)));
			if (plan.getType() == null) {
				plan.setType(PlanModel.guessPlanType(plan, config.getModes()));
			}

			String type = plan.getType();
			hist.add(plan, type);
		}

		// Thread local stores which thread uses which generator
		this.counter.set(0);
		this.assignment = ThreadLocal.withInitial(counter::getAndIncrement);
		this.executor = Executors.newFixedThreadPool(globalConfig.global().getNumberOfThreads());
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

		executor.submit(new ReplanningTask(person, history.get(person.getId())));
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
		private final PlanHistory planHistory;
		private final Random rnd;

		private ReplanningTask(HasPlansAndId<Plan, Person> person, PlanHistory planHistory) {
			this.person = person;
			this.planHistory = planHistory;
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

			GeneratorContext ctx = threadContexts[assignment.get()];
			SelectSingleTripModeStrategy.Algorithm sst = singleTrip[assignment.get()];

			PlanModel model = planHistory.model;

			Plan plan = person.createCopyOfSelectedPlanAndMakeSelected();
			model.setPlan(plan);

			if (rnd.nextDouble() < config.getProbaEstimate())
				model.reset();

			// Reduce avoid list as necessary
			while (config.getAvoidK() > 0 && planHistory.avoidList.size() > config.getAvoidK())
				planHistory.avoidList.poll();

			// Do change single trip on non-chain based modes with certain probability
			if (rnd.nextDouble() < smc.getProbaForRandomSingleTripMode() && SelectSubtourModeStrategy.hasSingleTripChoice(model, nonChainBasedModes)) {
				// return here if plan was modified
				PlanCandidate c = sst.chooseCandidate(model, planHistory.avoidList);

				if (c != null) {

					ArrayList<String[]> l = new ArrayList<>();
					l.add(c.getModes());

					// re-estimate as full plan
					c = ctx.generator.generatePredefined(model, l).get(0);

					c.applyTo(plan);
					plan.setType(c.getPlanType());
					return;
				}
			}

			List<TripStructureUtils.Subtour> subtours = new ArrayList<>(TripStructureUtils.getSubtours(plan, smc.getCoordDistance()));

			// Add whole trip if not already present
			if (subtours.stream().noneMatch(s -> s.getTrips().size() == model.trips())) {
				subtours.add(0, TripStructureUtils.getUnclosedRootSubtour(plan));
			}

			double threshold = Double.NaN;

			if (ctx.pruner != null) {

				Optional<? extends Plan> best = person.getPlans().stream()
						.filter(p -> p.getAttributes().getAttribute(PlanCandidate.ESTIMATE_ATTR) != null)
						.max(Comparator.comparingDouble(Plan::getScore));

				if (best.isPresent()) {
					threshold = (double) best.get().getAttributes().getAttribute(PlanCandidate.ESTIMATE_ATTR) - ctx.pruner.planThreshold(model);
				}
			}

			while (!subtours.isEmpty()) {

				TripStructureUtils.Subtour st = subtours.remove(rnd.nextInt(subtours.size()));
				boolean[] mask = new boolean[model.trips()];
				for (int i = 0; i < model.trips(); i++) {
					if (st.getTrips().contains(model.getTrip(i)))
						mask[i] = true;

				}

				Collection<PlanCandidate> candidates = ctx.generator.generate(model, null,
						mask, config.getTopK() + planHistory.avoidList.size(), 0, threshold);

				candidates.removeIf(c -> Arrays.equals(c.getModes(), model.getCurrentModes()) || planHistory.avoidList.contains(c.getModes()));
				if (!candidates.isEmpty()) {

					PlanCandidate select = ctx.selector.select(candidates);
					if (select != null) {
						select.applyTo(plan);
						plan.setType(select.getPlanType());
						return;
					}
				}
			}

			// TODO: handle if all options have been exhausted
		}
	}

	private static final class PlanHistory {

		private final PlanModel model;
		private final Map<String, DoubleList> scores = new HashMap<>();
		private final Map<String, DoubleList> estimates = new HashMap<>();

		private final Queue<String[]> avoidList = new LinkedList<>();

		public PlanHistory(Id<Person> id, PlanModel model) {
			this.model = model;
		}

		public void add(Plan plan, String type) {

			Object estimate = plan.getAttributes().getAttribute(PlanCandidate.ESTIMATE_ATTR);

			if (plan.getScore() == null)
				return;

			scores.computeIfAbsent(type, k -> new DoubleArrayList()).add((double) plan.getScore());
			if (estimate != null)
				estimates.computeIfAbsent(type, k -> new DoubleArrayList()).add((double) estimate);

			String[] modes = PlanCandidate.createModeArray(type);

			if (!avoidList.contains(modes))
				avoidList.add(modes);
		}

		public Set<String> types() {
			return scores.keySet();
		}

		public DoubleList getScores(String type) {
			return scores.get(type);
		}

		public DoubleList getEstimates(String type) {
			return estimates.getOrDefault(type, DoubleList.of());
		}
	}
}
