package org.matsim.freight.carriers.jsprit;

import com.graphhopper.jsprit.core.algorithm.SearchStrategy;
import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.listener.*;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.freight.carriers.Carrier;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.NavigableMap;
import java.util.TreeMap;

public class JspritStrategyAnalyzer implements StrategySelectedListener, AlgorithmEndsListener, IterationEndsListener, IterationStartsListener, AlgorithmStartsListener {

	private static final Logger log = LogManager.getLogger(JspritStrategyAnalyzer.class);
	private int iterationsCounter;
	private final LinkedHashMap <Integer, IterationResult> iterationSolutionCosts;
	private final NavigableMap<Integer, Double> foundNewBestSolutions;

	private double iterationStartTime;
	private double algorithmStartTime;
	private final Carrier carrier;
	public JspritStrategyAnalyzer(Carrier carrier) {
		this.carrier = carrier;
		iterationSolutionCosts = new LinkedHashMap<>();
		foundNewBestSolutions = new TreeMap<>();

	}

	public record IterationResult(double cost, String strategyId, double iterationComputationTimeInSeconds) {}

	@Override
	public void informIterationStarts(int i, VehicleRoutingProblem problem, Collection<VehicleRoutingProblemSolution> solutions) {
		iterationsCounter = i;
		// before the first iteration starts, take the initial solution costs and time
		if (i == 1) {
			double initialSolutionComputationTime = (System.currentTimeMillis() - algorithmStartTime) / 1000.0;
			double initialSolutionCosts = solutions.iterator().next().getCost();
			iterationSolutionCosts.put(0, new IterationResult(initialSolutionCosts, "initialSolution", initialSolutionComputationTime));
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
		iterationSolutionCosts.put(iterationsCounter, new IterationResult(discoveredSolution.getSolution().getCost(), discoveredSolution.getStrategyId(), iterationComputationTime));
	}

	@Override
	public void informAlgorithmEnds(VehicleRoutingProblem problem, Collection<VehicleRoutingProblemSolution> solutions) {
		// fill in the value of the last iteration if no new best solution was found in the last iteration
		if (!foundNewBestSolutions.containsKey(iterationsCounter))
			foundNewBestSolutions.put(iterationsCounter, foundNewBestSolutions.lastEntry().getValue());
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
		if (i == 1 || foundNewBestSolutions.lastEntry().getValue() > best) {
			log.info("Carrier {}: New best solution found in iteration {}: {}", carrier.getId(), i, best);
			foundNewBestSolutions.put(i, best);
		}
	}

	public LinkedHashMap <Integer, IterationResult> getIterationSolutionCosts() {
		return iterationSolutionCosts;
	}

	public NavigableMap<Integer, Double> getFoundNewBestSolutions() {
		return foundNewBestSolutions;
	}

}
