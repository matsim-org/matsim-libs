package org.matsim.contrib.freight.vrp.algorithms.rr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.contrib.freight.vrp.algorithms.rr.recreation.BestInsertion;
import org.matsim.contrib.freight.vrp.algorithms.rr.ruin.AvgDistanceBetweenJobs;
import org.matsim.contrib.freight.vrp.algorithms.rr.ruin.RadialRuin;
import org.matsim.contrib.freight.vrp.algorithms.rr.ruin.RandomRuin;
import org.matsim.contrib.freight.vrp.algorithms.rr.thresholdFunctions.SchrimpfsRRThresholdFunction;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.DistributionOfferMaker;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.PickupAndDeliveryOfferMaker;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.RRTourAgentFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.TourCostAndTWProcessor;
import org.matsim.contrib.freight.vrp.basics.CarrierCostFunction;
import org.matsim.contrib.freight.vrp.basics.RandomNumberGeneration;
import org.matsim.contrib.freight.vrp.basics.TourPlan;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblem;

public class DistributionTourWithTimeWindowsAlgoFactory implements RuinAndRecreateFactory {

	private static Logger logger = Logger.getLogger(PickupAndDeliveryTourWithTimeWindowsAlgoFactory.class);
	
	private Collection<RuinAndRecreateListener> ruinAndRecreationListeners = new ArrayList<RuinAndRecreateListener>();

	private int warmUp = 10;
	
	private int iterations = 50;
	
	private Random random = RandomNumberGeneration.getRandom();


	public void setRandom(Random random) {
		this.random = random;
	}

	public DistributionTourWithTimeWindowsAlgoFactory() {
		super();
	}

	public void addRuinAndRecreateListener(RuinAndRecreateListener l){
		ruinAndRecreationListeners.add(l);
	}
	

	public void setIterations(int iterations) {
		this.iterations = iterations;
	}
	
	@Override
	public RuinAndRecreate createAlgorithm(VehicleRoutingProblem vrp, RRSolution initialSolution) {
		TourCostAndTWProcessor tourCostProcessor = new TourCostAndTWProcessor(vrp.getCosts());
		RRTourAgentFactory tourAgentFactory = new RRTourAgentFactory(tourCostProcessor,vrp.getCosts().getCostParams(), 
				new DistributionOfferMaker(vrp.getCosts(), vrp.getGlobalConstraints()));
		RuinAndRecreate ruinAndRecreateAlgo = new RuinAndRecreate(vrp, initialSolution, iterations);
		ruinAndRecreateAlgo.setWarmUpIterations(warmUp);
		ruinAndRecreateAlgo.setTourAgentFactory(tourAgentFactory);
		ruinAndRecreateAlgo.setRuinStrategyManager(new RuinStrategyManager());
		
		BestInsertion recreationStrategy = new BestInsertion();
		recreationStrategy.setRandom(random);
		
		ruinAndRecreateAlgo.setRecreationStrategy(recreationStrategy);
		
		RadialRuin radialRuin = new RadialRuin(vrp, new AvgDistanceBetweenJobs(vrp.getCosts()));
		radialRuin.setFractionOfAllNodes(0.3);
		radialRuin.setRandom(random);
		
		RandomRuin randomRuin = new RandomRuin(vrp);
		randomRuin.setFractionOfAllNodes2beRuined(0.4);
		randomRuin.setRandom(random);
		
		ruinAndRecreateAlgo.getRuinStrategyManager().addStrategy(radialRuin, 0.5);
		ruinAndRecreateAlgo.getRuinStrategyManager().addStrategy(randomRuin, 0.5);
		
		ruinAndRecreateAlgo.setThresholdFunction(new SchrimpfsRRThresholdFunction(0.1));
		
		for(RuinAndRecreateListener l : ruinAndRecreationListeners){
			ruinAndRecreateAlgo.getListeners().add(l);
		}
		
		return ruinAndRecreateAlgo;
	}
	
	@Override
	public RuinAndRecreate createAlgorithm(VehicleRoutingProblem vrp, TourPlan initialSolution) {
		TourCostAndTWProcessor tourCostProcessor = new TourCostAndTWProcessor(vrp.getCosts());
		RRTourAgentFactory tourAgentFactory = new RRTourAgentFactory(tourCostProcessor,vrp.getCosts().getCostParams(), 
				new DistributionOfferMaker(vrp.getCosts(), vrp.getGlobalConstraints()));
		RuinAndRecreate ruinAndRecreateAlgo = new RuinAndRecreate(vrp, initialSolution, iterations);
		ruinAndRecreateAlgo.setWarmUpIterations(warmUp);
		ruinAndRecreateAlgo.setTourAgentFactory(tourAgentFactory);
		ruinAndRecreateAlgo.setRuinStrategyManager(new RuinStrategyManager());
		
		BestInsertion recreationStrategy = new BestInsertion();
		recreationStrategy.setRandom(random);
		
		ruinAndRecreateAlgo.setRecreationStrategy(recreationStrategy);
		
		RadialRuin radialRuin = new RadialRuin(vrp, new AvgDistanceBetweenJobs(vrp.getCosts()));
		radialRuin.setFractionOfAllNodes(0.3);
		radialRuin.setRandom(random);
		
		RandomRuin randomRuin = new RandomRuin(vrp);
		randomRuin.setFractionOfAllNodes2beRuined(0.5);
		randomRuin.setRandom(random);
		
		ruinAndRecreateAlgo.getRuinStrategyManager().addStrategy(radialRuin, 0.5);
		ruinAndRecreateAlgo.getRuinStrategyManager().addStrategy(randomRuin, 0.5);
		ruinAndRecreateAlgo.setThresholdFunction(new SchrimpfsRRThresholdFunction(0.1));
		
		for(RuinAndRecreateListener l : ruinAndRecreationListeners){
			ruinAndRecreateAlgo.getListeners().add(l);
		}
		
		return ruinAndRecreateAlgo;
	}

	@Override
	public void setWarmUp(int nOfWarmUpIterations) {
		this.warmUp = nOfWarmUpIterations;
		
	}
	
}
