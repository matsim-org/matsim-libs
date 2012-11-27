package org.matsim.contrib.freight.vrp.algorithms.rr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

import org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators.RouteAgentFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.iniSolution.InitialSolutionFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.listener.RuinAndRecreateListener;
import org.matsim.contrib.freight.vrp.algorithms.rr.recreate.RecreationStrategy;
import org.matsim.contrib.freight.vrp.algorithms.rr.ruin.RuinStrategy;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblem;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblemSolution;
import org.matsim.contrib.freight.vrp.utils.RandomNumberGeneration;
import org.matsim.core.utils.collections.Tuple;

public class RuinAndRecreateAlgorithmBuilder {

	private Integer warmup;

	private Integer iterations;

	private RecreationStrategy recreationStrategy;

	private RuinStrategyManager ruinStrategyManager;

	private InitialSolutionFactory initialSolutionFactory;

	private VehicleRoutingProblem vrp;

	private Random random = RandomNumberGeneration.getRandom();

	private ThresholdFunction thresholdFunction = new ThresholdFunctionSchrimpf(
			0.1);

	private Collection<RuinAndRecreateListener> contolerListeners = new ArrayList<RuinAndRecreateListener>();

	public void setThresholdFunction(ThresholdFunction thresholdFunction) {
		this.thresholdFunction = thresholdFunction;
	}

	private VehicleRoutingProblemSolution iniSolution;

	public void setRandom(Random random) {
		this.random = random;
	}

	public RuinAndRecreateAlgorithmBuilder(VehicleRoutingProblem vrp) {
		super();
		ruinStrategyManager = new RuinStrategyManager();
		this.vrp = vrp;
	}

	public void addControlerListener(RuinAndRecreateListener l) {
		this.contolerListeners.add(l);
	}

	public void setInitialSolutionFactory(InitialSolutionFactory initialSolutionFactory) {
		this.initialSolutionFactory = initialSolutionFactory;
	}

	public void addRuinStrategy(RuinStrategy ruinStrategy, double probability) {
		ruinStrategyManager.addStrategy(ruinStrategy, probability);
	}

	public void setRecreationStrategy(RecreationStrategy recreationStrategy) {
		this.recreationStrategy = recreationStrategy;
	}
	
	public void setInitialSolution(VehicleRoutingProblemSolution iniSolution){
		this.iniSolution = iniSolution;
	}

	public void setWarmupIterations(int value) {
		this.warmup = value;
	}

	public void setIterations(int value) {
		this.iterations = value;
	}

	public RuinAndRecreate buildAlgorithm() {
		if (warmup == null)
			throw new IllegalStateException("no warmup iterations set");
		if (iterations == null)
			throw new IllegalStateException("no iterations set");
		if (initialSolutionFactory == null && iniSolution == null)
			throw new IllegalStateException("set either an initial solutionFactory or a concrete initial solution");
		if (recreationStrategy == null)
			throw new IllegalStateException("no recreationstrategy set");
		checkRuinManager();
		RuinAndRecreate rr = new RuinAndRecreate(vrp);
		rr.setCurrentSolution(iniSolution);
		rr.setInitialSolutionFactory(initialSolutionFactory);
		rr.setIterations(iterations);
		rr.setWarmUpIterations(warmup);
		rr.setRecreationStrategy(recreationStrategy);
		rr.setRuinStrategyManager(ruinStrategyManager);
		rr.setThresholdFunction(thresholdFunction);
		for (RuinAndRecreateListener l : contolerListeners) {
			rr.getControlerListeners().add(l);
		}

		return rr;
	}

	private void checkRuinManager() {
		if (ruinStrategyManager.getStrategies().size() == 0) {
			throw new IllegalStateException("no ruin strat set");
		}
		double propSum = 0.0;
		for (Tuple<RuinStrategy, Double> t : ruinStrategyManager
				.getStrategies()) {
			propSum += t.getSecond();
		}
		if (propSum != 1.0) {
			throw new IllegalStateException(
					"check ruin-probabilities. probabilities do not sum up to 1.0");
		}

	}

}
