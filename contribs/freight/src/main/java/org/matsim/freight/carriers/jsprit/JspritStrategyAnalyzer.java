package org.matsim.freight.carriers.jsprit;

import com.graphhopper.jsprit.core.algorithm.SearchStrategy;
import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.listener.*;
import com.graphhopper.jsprit.core.algorithm.ruin.listener.RuinListener;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.job.Job;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.freight.carriers.Carrier;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.NavigableMap;
import java.util.TreeMap;

public class JspritStrategyAnalyzer implements StrategySelectedListener, IterationEndsListener, IterationStartsListener, AlgorithmStartsListener, RuinListener {

	private static final Logger log = LogManager.getLogger(JspritStrategyAnalyzer.class);
	private int iterationsCounter;
	private final LinkedHashMap<Integer, IterationResult> iterationSolutionCosts;
	private final NavigableMap<Integer, Double> foundNewBestSolutions;

	private double iterationStartTime;
	private double algorithmStartTime;
	private int removedJobsWhileRuin;
	private int routesAfterRuin;
	private final Carrier carrier;

	public JspritStrategyAnalyzer(Carrier carrier) {
		this.carrier = carrier;
		iterationSolutionCosts = new LinkedHashMap<>();
		foundNewBestSolutions = new TreeMap<>();

	}

	public record IterationResult(double costsOfThisIteration, String strategyId, double iterationComputationTimeInSeconds,
								  int numberOfRoutesOfThisIterationSolution, int removedJobsWhileRuin,
								  int routesAfterRuin) {
	}

	@Override
	public void ruinStarts(Collection<VehicleRoute> routes) {
	}

	@Override
	public void ruinEnds(Collection<VehicleRoute> routes, Collection<Job> unassignedJobs) {
		removedJobsWhileRuin = unassignedJobs.size();
		routesAfterRuin = routes.size();
	}

	@Override
	public void removed(Job job, VehicleRoute fromRoute) {
	}

	@Override
	public void informIterationStarts(int i, VehicleRoutingProblem problem, Collection<VehicleRoutingProblemSolution> solutions) {
		iterationsCounter = i;
		// before the first iteration starts, take the initial solution costs and time
		if (i == 1) {
			double initialSolutionComputationTime = (System.currentTimeMillis() - algorithmStartTime) / 1000.0;
			double initialSolutionCosts = solutions.iterator().next().getCost();
			iterationSolutionCosts.put(0, new IterationResult(initialSolutionCosts, "initialSolution", initialSolutionComputationTime,
				solutions.iterator().next().getRoutes().size(), 0, 0));
			iterationStartTime = System.currentTimeMillis();
			foundNewBestSolutions.put(0, initialSolutionCosts);
		}
	}

	@Override
	public void informAlgorithmStarts(VehicleRoutingProblem problem, VehicleRoutingAlgorithm algorithm,
									  Collection<VehicleRoutingProblemSolution> solutions) {
		algorithmStartTime = System.currentTimeMillis();
	}

	@Override
	public void informSelectedStrategy(SearchStrategy.DiscoveredSolution discoveredSolution, VehicleRoutingProblem vehicleRoutingProblem,
									   Collection<VehicleRoutingProblemSolution> vehicleRoutingProblemSolutions) {
		double iterationComputationTime = (System.currentTimeMillis() - iterationStartTime) / 1000.0;
		iterationSolutionCosts.put(iterationsCounter,
			new IterationResult(discoveredSolution.getSolution().getCost(), discoveredSolution.getStrategyId(), iterationComputationTime,
				discoveredSolution.getSolution().getRoutes().size(), removedJobsWhileRuin, routesAfterRuin));
	}

	@Override
	public void informIterationEnds(int i, VehicleRoutingProblem problem, Collection<VehicleRoutingProblemSolution> solutions) {
		if (i != iterationsCounter) {
			log.error("Inconsistent iteration number: {} vs {}", i, iterationsCounter);
		}
		double best = Double.MAX_VALUE;
		for (VehicleRoutingProblemSolution sol : solutions) {
			if (sol.getCost() < best)
				best = sol.getCost();
		}
		if (foundNewBestSolutions.lastEntry().getValue() > best) {
			foundNewBestSolutions.put(i, best);
		}
	}

	public LinkedHashMap<Integer, IterationResult> getIterationSolutionCosts() {
		return iterationSolutionCosts;
	}

	public NavigableMap<Integer, Double> getFoundNewBestSolutions() {
		return foundNewBestSolutions;
	}

}
