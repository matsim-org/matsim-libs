package org.matsim.modechoice.replanning.scheduled.solver;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIntPair;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.modechoice.PlanCandidate;
import org.matsim.modechoice.ScheduledModeChoiceConfigGroup;
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
	static final int WINDOW_SIZE = 3;

	static final Logger log = LogManager.getLogger(ModeSchedulingSolver.class);

	private final int scheduleLength;
	private final int topK;
	private final double targetSwitchShare;

	public ModeSchedulingSolver(int scheduleLength, int topK,
								List<ScheduledModeChoiceConfigGroup.ModeTargetParameters> modeTargetParameters,
								double targetSwitchShare) {
		this.scheduleLength = scheduleLength;
		// topK must be divisible by window size
		this.topK = topK + Math.floorMod(-topK, WINDOW_SIZE);
		this.targetSwitchShare = targetSwitchShare;
	}

	public Map<Id<Person>, List<PlanCandidate>> solve(Map<Id<Person>, List<PlanCandidate>> plans) {

		if (scheduleLength > topK)
			throw new IllegalArgumentException("Schedule length must be less than or equal to topK");

		List<AgentSchedule> agents = new ArrayList<>();

		Reference2ObjectMap<String, IntIntPair> target = createTarget(plans);

		Random rnd = new Random(0);

		for (Map.Entry<Id<Person>, List<PlanCandidate>> kv : plans.entrySet()) {

			// nothing to optimize for empty plans
			List<PlanCandidate> candidates = kv.getValue();
			if (candidates.isEmpty())
				continue;

			// Fill candidates to topK randomly
			if (candidates.size() < topK) {
				for (int i = candidates.size(); i < topK; i++) {
					candidates.add(candidates.get(rnd.nextInt(candidates.size())));
				}
			}

			String[] planCategories = categorizePlans(candidates);
			// Irrelevant plan for the optimization, if all entries are the same
			// TODO might affect the target value
			if (Arrays.stream(planCategories).allMatch(p -> Objects.equals(p, planCategories[0])))
				continue;

			int length = candidates.get(0).size();
			byte[] weights = computePlanWeights(target, planCategories, candidates.size(), length);

			AgentSchedule schedule = new AgentSchedule(kv.getKey(), planCategories, weights, length);

			// All plans are available initially
			IntStream.range(0, topK).forEach(schedule.availablePlans::add);

			agents.add(schedule);
		}

		// TODO: target is not yet flexible
		int switchTarget = (int) (targetSwitchShare * agents.stream().mapToInt(a -> a.length).sum());

		Map<Id<Person>, List<PlanCandidate>> result = new HashMap<>();

		for (int k = 0; k < scheduleLength; k += WINDOW_SIZE) {

			// Initialize agents
			initialize(k, agents);

			ModeSchedulingProblem problem = new ModeSchedulingProblem(WINDOW_SIZE, agents, target, switchTarget);
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
				result.put(kv.getKey(), kv.getValue());
			}
		}

		return result;
	}

	private Reference2ObjectMap<String, IntIntPair> createTarget(Map<Id<Person>, List<PlanCandidate>> plans) {

		int trips = plans.values().stream()
			.mapToInt(p -> p.get(0).size())
			.sum();

		Reference2ObjectMap<String, IntIntPair> target = new Reference2ObjectLinkedOpenHashMap<>();

		// TODO: fixed car share of 50%#
		// fixed deviation
		target.put(TransportMode.car, IntIntPair.of((int) (trips * 0.495), (int) (trips * 0.505)));

		return target;
	}

	/**
	 * Initialize agents with random assignment.
	 */
	private void initialize(int k, List<AgentSchedule> agents) {

		// TODO: could choose plans such that error is minimized

		List<AgentSchedule> copy = new ArrayList<>(agents);

		Random rnd = new Random(k);
		Collections.shuffle(copy, rnd);

		for (AgentSchedule agent : copy) {
			agent.indices.clear();

			IntList avail = new IntArrayList(agent.availablePlans);
			Collections.shuffle(avail, rnd);

			for (int i = 0; i < WINDOW_SIZE; i++) {
				agent.indices.add(avail.getInt(i));
			}
		}

	}

	private String[] categorizePlans(List<PlanCandidate> candidates) {

		int trips = candidates.get(0).size();
		String[] categories = new String[candidates.size() * trips];

		for (int i = 0; i < candidates.size(); i++) {
			PlanCandidate candidate = candidates.get(i);

			// TODO: hard coded to car mode
			for (int j = 0; j < trips; j++) {
				categories[i * trips + j] = Objects.equals(candidate.getMode(j), TransportMode.car) ? TransportMode.car : null;
			}
		}

		return categories;
	}

	/**
	 * Aggregates categories against targets.
	 */
	@SuppressWarnings("StringEquality")
	private byte[] computePlanWeights(Reference2ObjectMap<String, IntIntPair> target, String[] categories, int candidates, int trips) {

		byte[] aggr = new byte[target.size() * candidates];

		for (int i = 0; i < candidates; i++) {
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

		return aggr;
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
