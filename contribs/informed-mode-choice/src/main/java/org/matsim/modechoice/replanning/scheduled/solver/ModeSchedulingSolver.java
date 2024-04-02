package org.matsim.modechoice.replanning.scheduled.solver;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIntPair;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.modechoice.ModeTargetParameters;
import org.matsim.modechoice.PlanCandidate;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

/**
 * Solves the scheduling problem.
 */
public final class ModeSchedulingSolver {

	/**
	 * Number of plans to optimize simultaneously.
	 */
	public static final int WINDOW_SIZE = 3;

	static final Logger log = LogManager.getLogger(ModeSchedulingSolver.class);

	private final int scheduleLength;
	private final ModeTarget target;
	private final double targetSwitchShare;

	public ModeSchedulingSolver(int scheduleLength, ModeTarget target, double targetSwitchShare) {
		this.scheduleLength = scheduleLength;
		this.target = target;
		this.targetSwitchShare = targetSwitchShare;
	}

	public Map<Id<Person>, List<PlanCandidate>> solve(Map<Id<Person>, List<PlanCandidate>> plans) {

		if (plans.isEmpty())
			throw new IllegalArgumentException("No plans to optimize");

		List<AgentSchedule> agents = new ArrayList<>();

		Reference2ObjectMap<String, IntIntPair> target = createTarget(plans);

		Random rnd = new Random(0);

		for (Map.Entry<Id<Person>, List<PlanCandidate>> kv : plans.entrySet()) {

			// nothing to optimize for empty plans
			List<PlanCandidate> candidates = kv.getValue();
			if (candidates.isEmpty())
				continue;

			// Not mapped plans are not optimized
			if (!this.target.mapping().containsKey(kv.getKey()))
				continue;

			AgentSchedule schedule = createInitialSchedule(kv.getKey(), candidates, target, rnd);

			// Schedule can be filtered
			if (schedule == null)
				continue;

			// All plans are available initially
			IntStream.range(0, scheduleLength).forEach(schedule.availablePlans::add);

			agents.add(schedule);
		}

		if (agents.isEmpty())
			throw new IllegalArgumentException("No relevant plans to optimize");

		int switchTarget = (int) (targetSwitchShare * agents.stream().mapToInt(a -> a.length).sum());

		Map<Id<Person>, List<PlanCandidate>> result = new LinkedHashMap<>();

		for (int k = 0; k < scheduleLength; k += WINDOW_SIZE) {

			// Initialize agents
			initialize(k, agents);

			ModeSchedulingProblem problem = new ModeSchedulingProblem(WINDOW_SIZE, agents, target, switchTarget);

			preSolve(k, problem);

			ModeSchedulingProblem solution = solve(problem);

			solution.printScore(k);

			// underlying objects have been copied, need to be reassigned
			agents = solution.getAgents();

			for (AgentSchedule agent : solution.getAgents()) {

				// Fill result with candidates
				List<PlanCandidate> res = result.computeIfAbsent(agent.getId(), id -> new ArrayList<>());
				for (int idx : agent.indices) {
					res.add(plans.get(agent.getId()).get(idx));
				}

				agent.currentPlan = agent.indices.getInt(agent.indices.size() - 1);
				agent.availablePlans.removeIf(agent.indices::contains);
			}
		}

		// Copy original plan order for irrelevant agents
		for (Map.Entry<Id<Person>, List<PlanCandidate>> kv : plans.entrySet()) {
			if (!result.containsKey(kv.getKey())) {
				List<PlanCandidate> candidates = kv.getValue();

				// Not optimized plans need to be filled
				if (candidates.size() < scheduleLength && !candidates.isEmpty()) {
					for (int i = candidates.size(); i < scheduleLength; i++) {
						candidates.add(candidates.get(rnd.nextInt(candidates.size())));
					}
				}

				result.put(kv.getKey(), candidates);
			}
		}

		return result;
	}

	private AgentSchedule createInitialSchedule(Id<Person> id, List<PlanCandidate> candidates, Reference2ObjectMap<String, IntIntPair> target, Random rnd) {

		// Number of trips per plan
		int trips = candidates.get(0).size();

		String[] categories = new String[scheduleLength * trips];

		// Summed number of categories
		byte[] weights = new byte[scheduleLength * target.size()];

		String key = this.target.mapping().get(id);
		Object2DoubleMap<String> t = this.target.targets().get(key);

		for (int i = 0; i < candidates.size(); i++) {
			PlanCandidate candidate = candidates.get(i);

			categorizePlans(categories, trips, i, candidate, key, t);
			computePlanWeights(weights, categories, trips, i, target);
		}

		// Irrelevant plan for the optimization
		if (Arrays.stream(categories).allMatch(p -> Objects.equals(p, null)))
			return null;

		// TODO: fill needed categories, instead of randomly
		// Fill candidates to topK randomly
		while (candidates.size() < scheduleLength) {
			PlanCandidate candidate = candidates.get(rnd.nextInt(candidates.size()));
			candidates.add(candidate);

			int i = candidates.size() - 1;
			categorizePlans(categories, trips, i, candidate, key, t);
			computePlanWeights(weights, categories, trips, i, target);
		}

		return new AgentSchedule(id, categories, weights, trips);
	}

	private void categorizePlans(String[] categories, int trips, int idx, PlanCandidate candidate, String key, Object2DoubleMap<String> t) {
		for (int j = 0; j < trips; j++) {
			String mode = candidate.getMode(j);
			String category = null;
			if (t.containsKey(mode)) {
				category = key + "[" + mode + "]";
			}
			categories[idx * trips + j] = category != null ? category.intern() : null;
		}
	}

	/**
	 * Aggregates categories against targets.
	 */
	@SuppressWarnings("StringEquality")
	private void computePlanWeights(byte[] aggr, String[] categories, int trips, int i, Reference2ObjectMap<String, IntIntPair> target) {
		int k = 0;
		for (String ref : target.keySet()) {
			int idx = i * target.size() + k++;
			for (int j = 0; j < trips; j++) {
				if (categories[i * trips + j] == ref) {
					aggr[idx]++;
				}
			}
		}
	}

	private Reference2ObjectMap<String, IntIntPair> createTarget(Map<Id<Person>, List<PlanCandidate>> plans) {

		Reference2ObjectMap<String, IntIntPair> target = new Reference2ObjectLinkedOpenHashMap<>();

		for (Map.Entry<String, Object2DoubleMap<String>> kv : this.target.targets().entrySet()) {

			String key = kv.getKey();
			ModeTargetParameters params = this.target.params().get(key);

			// number of trips for persons belonging to the target
			int trips = plans.entrySet().stream()
				.filter(p -> !p.getValue().isEmpty())
				.filter(p -> this.target.mapping().get(p.getKey()).equals(key))
				.mapToInt(p -> p.getValue().get(0).size())
				.sum();

			for (Object2DoubleMap.Entry<String> e : kv.getValue().object2DoubleEntrySet()) {
				int lower = (int) (trips * (e.getDoubleValue() - params.tolerance / 2));
				int upper = (int) (trips * (e.getDoubleValue() + params.tolerance / 2));

				String t = key + "[" + e.getKey() + "]";
				IntIntPair bounds = IntIntPair.of(lower, upper);
				target.put(t.intern(), bounds);

				log.info("Target {} {} with {} trips.", t, bounds, trips);
			}
		}

		return target;
	}

	/**
	 * Initialize agents with random assignment.
	 */
	private void initialize(int k, List<AgentSchedule> agents) {

		List<AgentSchedule> copy = new ArrayList<>(agents);

		Random rnd = new Random(k);
		Collections.shuffle(copy, rnd);

		IntList avail = new IntArrayList(scheduleLength);

		for (AgentSchedule agent : copy) {
			agent.indices.clear();

			avail.clear();
			Collections.shuffle(agent.availablePlans, rnd);
			avail.addAll(agent.availablePlans);

			for (int i = 0; i < WINDOW_SIZE; i++) {
				agent.indices.add(avail.getInt(i));
			}
		}
	}

	/**
	 * Tries to construct an initial solution.
	 */
	private void preSolve(int k, ModeSchedulingProblem problem) {

		ScoreCalculator score = new ScoreCalculator();
		score.resetWorkingSolution(problem);

		int[] targets = new int[problem.getTargets().size()];
		int i = 0;
		for (Map.Entry<String, IntIntPair> kv : problem.getTargets().entrySet()) {
			targets[i++] = (kv.getValue().leftInt() + kv.getValue().rightInt()) / 2;
		}

		List<AgentSchedule> copy = new ArrayList<>(problem.getAgents());

		Random rnd = new Random(k);
		Collections.shuffle(copy, rnd);

		IntList avail = new IntArrayList(scheduleLength);

		for (AgentSchedule agent : copy) {

			// Avail stores other available plans for initialisation
			avail.clear();
			avail.addAll(agent.availablePlans);
			avail.removeAll(agent.indices);

			for (int idx = 0; idx < WINDOW_SIZE; idx++) {
				score.applyImprovement(agent, idx, avail, targets);
			}
		}
	}

	private ModeSchedulingProblem solve(ModeSchedulingProblem problem) {

		// Loading fails if xerces is on the classpath
		SolverFactory<ModeSchedulingProblem> factory = SolverFactory.createFromXmlResource(
			"org/matsim/modechoice/scheduled/solver.xml"
		);

		Solver<ModeSchedulingProblem> solver = factory.buildSolver();

		AtomicLong ts = new AtomicLong(System.currentTimeMillis());

		solver.addEventListener(event -> {

			// Only log every x seconds
			if (ts.get() + 30_000 < System.currentTimeMillis()) {
				log.info("New best solution: {}", event.getNewBestScore());
				ts.set(System.currentTimeMillis());
			}
		});

		return solver.solve(problem);
	}
}
