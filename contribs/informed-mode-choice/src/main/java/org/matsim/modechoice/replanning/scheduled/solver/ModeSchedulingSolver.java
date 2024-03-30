package org.matsim.modechoice.replanning.scheduled.solver;

import it.unimi.dsi.fastutil.ints.IntIntPair;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
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

		for (Map.Entry<Id<Person>, List<PlanCandidate>> kv : plans.entrySet()) {

			// nothing to optimize for empty plans
			List<PlanCandidate> candidates = kv.getValue();
			if (candidates.isEmpty())
				continue;

			// Fill candidates to topK
			if (candidates.size() < topK) {
				for (int i = candidates.size(); i < topK; i++) {
					candidates.add(candidates.get(i % candidates.size()));
				}
			}

			String[] planCategories = categorizePlans(candidates);
			// Irrelevant plan for the optimization, if all entries are the same
			// TODO might affect the target value
			if (Arrays.stream(planCategories).allMatch(p -> Objects.equals(p, planCategories[0])))
				continue;

			AgentSchedule schedule = new AgentSchedule(kv.getKey(), planCategories, candidates.get(0).size());

			// All plans are available initially
			IntStream.range(0, topK).forEach(schedule.availablePlans::add);

			agents.add(schedule);
		}

		agents.forEach(this::init);

		// TODO: target is not yet flexible
		Reference2ObjectMap<String, IntIntPair> target = createTarget(plans);
		int switchTarget = (int) (targetSwitchShare * agents.stream().mapToInt(a -> a.length).sum());

		Map<Id<Person>, List<PlanCandidate>> result = new HashMap<>();

		for (int k = 0; k < scheduleLength; k += WINDOW_SIZE) {

			ModeSchedulingProblem problem = new ModeSchedulingProblem(WINDOW_SIZE, agents, target, switchTarget);
			ModeSchedulingProblem solution = solve(problem);

			solution.printScore(k);

			// TODO: strange behaviour
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
				agents.forEach(this::init);
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

		Reference2ObjectMap<String, IntIntPair> target = new Reference2ObjectOpenHashMap<>();

		// TODO: fixed car share of 50%#
		// fixed deviation
		target.put(TransportMode.car, IntIntPair.of((int) (trips * 0.495), (int) (trips * 0.505)));

		return target;
	}

	private void init(AgentSchedule agent) {

		// Add initial plans
		agent.indices.clear();
		IntStream.range(0, topK)
			.filter(agent.availablePlans::contains)
			.limit(WINDOW_SIZE)
			.forEach(agent.indices::add);

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
