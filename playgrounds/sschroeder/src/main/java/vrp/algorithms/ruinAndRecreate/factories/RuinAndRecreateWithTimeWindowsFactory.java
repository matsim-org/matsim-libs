package vrp.algorithms.ruinAndRecreate.factories;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;

import vrp.algorithms.ruinAndRecreate.RuinAndRecreate;
import vrp.algorithms.ruinAndRecreate.RuinStrategyManager;
import vrp.algorithms.ruinAndRecreate.api.RuinAndRecreateListener;
import vrp.algorithms.ruinAndRecreate.api.TourAgent;
import vrp.algorithms.ruinAndRecreate.api.TourAgentFactory;
import vrp.algorithms.ruinAndRecreate.basics.RRTourAgentWithTimeWindowFactory;
import vrp.algorithms.ruinAndRecreate.basics.Solution;
import vrp.algorithms.ruinAndRecreate.recreation.BestInsertion;
import vrp.algorithms.ruinAndRecreate.ruin.RadialRuin;
import vrp.algorithms.ruinAndRecreate.ruin.RandomRuin;
import vrp.algorithms.ruinAndRecreate.thresholdFunctions.SchrimpfsRRThresholdFunction;
import vrp.api.SingleDepotVRP;
import vrp.api.VRP;
import vrp.basics.SingleDepotInitialSolutionFactoryImpl;
import vrp.basics.Tour;
import vrp.basics.Vehicle;
import vrp.basics.VrpUtils;

public class RuinAndRecreateWithTimeWindowsFactory implements RuinAndRecreateFactory{

	private static Logger logger = Logger.getLogger(RuinAndRecreateWithTimeWindowsFactory.class);
	
	private Collection<RuinAndRecreateListener> ruinAndRecreationListeners = new ArrayList<RuinAndRecreateListener>();

	private int warmUp = 10;
	
	private int iterations = 50;
	
	public RuinAndRecreateWithTimeWindowsFactory(int warmup, int iterations) {
		this.warmUp = warmup;
		this.iterations = iterations;
	}

	public RuinAndRecreateWithTimeWindowsFactory() {
		super();
	}

	public void addRuinAndRecreateListener(RuinAndRecreateListener l){
		ruinAndRecreationListeners.add(l);
	}
	

	public void setIterations(int iterations) {
		this.iterations = iterations;
	}
	
	@Override
	public RuinAndRecreate createAlgorithm(SingleDepotVRP vrp,Collection<Tour> tours, int vehicleCapacity) {
		RRTourAgentWithTimeWindowFactory tourAgentFactory = new RRTourAgentWithTimeWindowFactory(vrp);
		Solution initialSolution = getInitialSolution(vrp,tours,tourAgentFactory,vehicleCapacity);
		RuinAndRecreate ruinAndRecreateAlgo = new RuinAndRecreate(vrp, initialSolution, iterations);
		ruinAndRecreateAlgo.setWarmUpIterations(warmUp);
		ruinAndRecreateAlgo.setTourAgentFactory(tourAgentFactory);
		ruinAndRecreateAlgo.setRuinStrategyManager(new RuinStrategyManager());
		
		BestInsertion recreationStrategy = new BestInsertion(vrp);
		recreationStrategy.setTourAgentFactory(tourAgentFactory);
		recreationStrategy.setInitialSolutionFactory(new SingleDepotInitialSolutionFactoryImpl());
		ruinAndRecreateAlgo.setRecreationStrategy(recreationStrategy);
		
		RadialRuin radialRuin = new RadialRuin(vrp);
		radialRuin.setFractionOfAllNodes(0.3);
		
		RandomRuin randomRuin = new RandomRuin(vrp);
		randomRuin.setFractionOfAllNodes2beRuined(0.5);
		
		ruinAndRecreateAlgo.getRuinStrategyManager().addStrategy(radialRuin, 0.5);
		ruinAndRecreateAlgo.getRuinStrategyManager().addStrategy(randomRuin, 0.5);
		ruinAndRecreateAlgo.setThresholdFunction(new SchrimpfsRRThresholdFunction(0.1));
		
		for(RuinAndRecreateListener l : ruinAndRecreationListeners){
			ruinAndRecreateAlgo.getListeners().add(l);
		}
		
		return ruinAndRecreateAlgo;
	}
	
	private Solution getInitialSolution(VRP vrp, Collection<Tour> tours, TourAgentFactory tourAgentFactory, int vehicleCapacity) {
		Collection<TourAgent> tourAgents = new ArrayList<TourAgent>();
		for(Tour tour : tours){
			Vehicle vehicle = VrpUtils.createVehicle(vehicleCapacity);
			tourAgents.add(tourAgentFactory.createTourAgent(tour, vehicle));
		}
		return new Solution(tourAgents);
	}

	@Override
	public void setWarmUp(int nOfWarmUpIterations) {
		this.warmUp = nOfWarmUpIterations;
		
	}

}
