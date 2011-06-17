package vrp.algorithms.ruinAndRecreate;

import java.util.ArrayList;
import java.util.Collection;

import vrp.algorithms.ruinAndRecreate.api.TourAgent;
import vrp.algorithms.ruinAndRecreate.api.TourAgentFactory;
import vrp.algorithms.ruinAndRecreate.basics.RRTourAgentFactory;
import vrp.algorithms.ruinAndRecreate.basics.RRTourAgentWithTimeWindowFactory;
import vrp.algorithms.ruinAndRecreate.basics.Solution;
import vrp.algorithms.ruinAndRecreate.recreation.BestInsertion;
import vrp.algorithms.ruinAndRecreate.ruin.RadialRuin;
import vrp.algorithms.ruinAndRecreate.ruin.RandomRuin;
import vrp.algorithms.ruinAndRecreate.thresholdFunctions.SchrimpfsRRThresholdFunction;
import vrp.api.VRP;
import vrp.basics.Tour;
import vrp.basics.Vehicle;
import vrp.basics.VrpUtils;

/**
 * Creates ready to use ruin-and-recreate-algorithms.
 * 
 * @author stefan schroeder
 *
 */


public class RuinAndRecreateFactory {
	
	
	/**
	 * Standard ruin and recreate without time windows. This algo is configured according to Schrimpf et. al (2000).
	 * @param vrp
	 * @param tours
	 * @param vehicleCapacity
	 * @return
	 */
	public RuinAndRecreate createStandardAlgo(VRP vrp, Collection<Tour> tours, int vehicleCapacity){
		RRTourAgentFactory tourAgentFactory = new RRTourAgentFactory();
		Solution initialSolution = getInitialSolution(vrp,tours,tourAgentFactory,vehicleCapacity);
		RuinAndRecreate ruinAndRecreateAlgo = new RuinAndRecreate(vrp, initialSolution, 100);
		ruinAndRecreateAlgo.setWarmUpIterations(10);
		ruinAndRecreateAlgo.setTourAgentFactory(tourAgentFactory);
		ruinAndRecreateAlgo.setRuinStrategyManager(new RuinStrategyManager());
		
		BestInsertion recreationStrategy = new BestInsertion(vrp);
		recreationStrategy.setNewVehicleCapacity(vehicleCapacity);
		recreationStrategy.setTourAgentFactory(tourAgentFactory);
		ruinAndRecreateAlgo.setRecreationStrategy(recreationStrategy);
		
		RadialRuin radialRuin = new RadialRuin(vrp);
		radialRuin.setFractionOfAllNodes(0.3);
		
		RandomRuin randomRuin = new RandomRuin(vrp);
		randomRuin.setFractionOfAllNodes2beRuined(0.5);
		
		ruinAndRecreateAlgo.getRuinStrategyManager().addStrategy(radialRuin, 0.5);
		ruinAndRecreateAlgo.getRuinStrategyManager().addStrategy(randomRuin, 0.5);
		ruinAndRecreateAlgo.setThresholdFunction(new SchrimpfsRRThresholdFunction(0.1));
		
		return ruinAndRecreateAlgo;
	}
	
	public RuinAndRecreate createAlgoWithTimeWindows(VRP vrp, Collection<Tour> tours, int vehicleCapacity){
		RRTourAgentWithTimeWindowFactory tourAgentFactory = new RRTourAgentWithTimeWindowFactory();
		Solution initialSolution = getInitialSolution(vrp,tours,tourAgentFactory,vehicleCapacity);
		RuinAndRecreate ruinAndRecreateAlgo = new RuinAndRecreate(vrp, initialSolution, 100);
		ruinAndRecreateAlgo.setWarmUpIterations(10);
		ruinAndRecreateAlgo.setTourAgentFactory(tourAgentFactory);
		ruinAndRecreateAlgo.setRuinStrategyManager(new RuinStrategyManager());
		
		BestInsertion recreationStrategy = new BestInsertion(vrp);
		recreationStrategy.setNewVehicleCapacity(vehicleCapacity);
		recreationStrategy.setTourAgentFactory(tourAgentFactory);
		ruinAndRecreateAlgo.setRecreationStrategy(recreationStrategy);
		
		RadialRuin radialRuin = new RadialRuin(vrp);
		radialRuin.setFractionOfAllNodes(0.3);
		
		RandomRuin randomRuin = new RandomRuin(vrp);
		randomRuin.setFractionOfAllNodes2beRuined(0.5);
		
		ruinAndRecreateAlgo.getRuinStrategyManager().addStrategy(radialRuin, 0.5);
		ruinAndRecreateAlgo.getRuinStrategyManager().addStrategy(randomRuin, 0.5);
		ruinAndRecreateAlgo.setThresholdFunction(new SchrimpfsRRThresholdFunction(0.1));
		
		return ruinAndRecreateAlgo;
	}
	
	

	private Solution getInitialSolution(VRP vrp, Collection<Tour> tours, TourAgentFactory tourAgentFactory, int vehicleCapacity) {
		Collection<TourAgent> tourAgents = new ArrayList<TourAgent>();
		for(Tour tour : tours){
			Vehicle vehicle = VrpUtils.createVehicle(vehicleCapacity);
			tourAgents.add(tourAgentFactory.createTourAgent(vrp, tour, vehicle));
		}
		return new Solution(tourAgents);
	}

}
