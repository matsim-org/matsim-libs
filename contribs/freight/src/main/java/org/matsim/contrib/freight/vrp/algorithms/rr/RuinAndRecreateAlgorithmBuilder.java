package org.matsim.contrib.freight.vrp.algorithms.rr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

import org.matsim.contrib.freight.vrp.algorithms.rr.listener.RuinAndRecreateControlerListener;
import org.matsim.contrib.freight.vrp.algorithms.rr.serviceProvider.ServiceProviderAgentFactory;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblem;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblemType;
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
	
	private ThresholdFunction thresholdFunction = new ThresholdFunctionSchrimpf(0.1);
	
	private Collection<RuinAndRecreateControlerListener> contolerListeners = new ArrayList<RuinAndRecreateControlerListener>();
	
	private VehicleRoutingProblemType vrpType;

	public void setThresholdFunction(ThresholdFunction thresholdFunction) {
		this.thresholdFunction = thresholdFunction;
	}

	private ServiceProviderAgentFactory serviceProviderAgentFactory;
	
	public void setRandom(Random random) {
		this.random = random;
	}

	public RuinAndRecreateAlgorithmBuilder(VehicleRoutingProblem vrp) {
		super();
		ruinStrategyManager = new RuinStrategyManager();
		this.vrp = vrp;
	}
		
	public void addControlerListener(RuinAndRecreateControlerListener l){
		this.contolerListeners.add(l);
	}
	
	public void setProblemType(VehicleRoutingProblemType vrpType){
		this.vrpType = vrpType;
	}
	
	public void setServiceProviderAgentFactory(ServiceProviderAgentFactory serviceProviderAgentFactory){
		this.serviceProviderAgentFactory = serviceProviderAgentFactory;
	}

	public void setInitialSolutionFactory(InitialSolutionFactory initialSolutionFactory){
		this.initialSolutionFactory = initialSolutionFactory;
	}
	
	public void addRuinStrategy(RuinStrategy ruinStrategy, double probability){
		ruinStrategyManager.addStrategy(ruinStrategy, probability);
	}
	
	public void setRecreationStrategy(RecreationStrategy recreationStrategy){
		this.recreationStrategy = recreationStrategy;
	}
	
	public void setWarmupIterations(int value){
		this.warmup = value;
	}
	
	public void setIterations(int value){
		this.iterations = value;
	}

	public RuinAndRecreate buildAlgorithm(){
		if(warmup == null) throw new IllegalStateException("no warmup iterations set");
		if(iterations == null) throw new IllegalStateException("no iterations set");
		if(initialSolutionFactory == null) throw new IllegalStateException("no initialsolutionfactory set");
		if(recreationStrategy == null) throw new IllegalStateException("no recreationstrategy set");
		if(serviceProviderAgentFactory == null) throw new IllegalStateException("no serviceproviderFactory set");
		checkRuinManager();
		RuinAndRecreate rr = new RuinAndRecreate(vrp);
		rr.setInitialSolutionFactory(initialSolutionFactory);
		rr.setIterations(iterations);
		rr.setWarmUpIterations(warmup);
		rr.setRecreationStrategy(recreationStrategy);
		rr.setRuinStrategyManager(ruinStrategyManager);
		rr.setTourAgentFactory(serviceProviderAgentFactory);
		rr.setThresholdFunction(thresholdFunction);
		for(RuinAndRecreateControlerListener l : contolerListeners){
			rr.getControlerListeners().add(l);
		}
		
		return rr;
	}

	private void checkRuinManager() {
		if(ruinStrategyManager.getStrategies().size() == 0){
			throw new IllegalStateException("no ruin strat set");
		}
		double propSum = 0.0;
		for(Tuple<RuinStrategy,Double> t : ruinStrategyManager.getStrategies()){
			propSum += t.getSecond();
		}
		if(propSum != 1.0){
			throw new IllegalStateException("check ruin-probabilities. probabilities do not sum up to 1.0");
		}
		
	}

}
