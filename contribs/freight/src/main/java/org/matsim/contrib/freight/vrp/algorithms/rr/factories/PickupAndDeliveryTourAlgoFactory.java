/*******************************************************************************
 * Copyright (C) 2011 Stefan Schroeder.
 * eMail: stefan.schroeder@kit.edu
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.matsim.contrib.freight.vrp.algorithms.rr.factories;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.matsim.contrib.freight.vrp.algorithms.rr.RRSolution;
import org.matsim.contrib.freight.vrp.algorithms.rr.RuinAndRecreate;
import org.matsim.contrib.freight.vrp.algorithms.rr.RuinAndRecreateFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.RuinAndRecreateListener;
import org.matsim.contrib.freight.vrp.algorithms.rr.RuinStrategyManager;
import org.matsim.contrib.freight.vrp.algorithms.rr.recreation.BestInsertion;
import org.matsim.contrib.freight.vrp.algorithms.rr.ruin.AvgDistanceBetweenJobs;
import org.matsim.contrib.freight.vrp.algorithms.rr.ruin.RadialRuin;
import org.matsim.contrib.freight.vrp.algorithms.rr.ruin.RandomRuin;
import org.matsim.contrib.freight.vrp.algorithms.rr.thresholdFunctions.SchrimpfsRRThresholdFunction;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.PickupAndDeliveryTourFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.RRTourAgentFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.TourCostProcessor;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.TourFactory;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblem;


/**
 * Creates ready to use ruin-and-recreate-algorithms.
 * 
 * @author stefan schroeder
 *
 */


public class PickupAndDeliveryTourAlgoFactory implements RuinAndRecreateFactory {
	
	private static Logger logger = Logger.getLogger(PickupAndDeliveryTourAlgoFactory.class);
	
	private Collection<RuinAndRecreateListener> ruinAndRecreationListeners = new ArrayList<RuinAndRecreateListener>();

	private int warmUp = 10;
	
	private int iterations = 100;
	
	public PickupAndDeliveryTourAlgoFactory(int warmUp, int iterations) {
		super();
		this.warmUp = warmUp;
		this.iterations = iterations;
	}

	public PickupAndDeliveryTourAlgoFactory() {
		super();
	}

	/* (non-Javadoc)
	 * @see vrp.algorithms.ruinAndRecreate.factories.RuinAndRecreateFactory#createAlgorithm(vrp.api.SingleDepotVRP, java.util.Collection, int)
	 */
	@Override
	public RuinAndRecreate createAlgorithm(VehicleRoutingProblem vrp, RRSolution initialSolution){
		TourCostProcessor tourCostProcessor = new TourCostProcessor(vrp.getCosts());
		TourFactory tourFactory = new PickupAndDeliveryTourFactory(vrp.getCosts(),vrp.getConstraints(), tourCostProcessor);
		RRTourAgentFactory tourAgentFactory = new RRTourAgentFactory(tourCostProcessor,tourFactory);
		RuinAndRecreate ruinAndRecreateAlgo = new RuinAndRecreate(vrp, initialSolution, iterations);
		ruinAndRecreateAlgo.setWarmUpIterations(warmUp);
		ruinAndRecreateAlgo.setTourAgentFactory(tourAgentFactory);
		ruinAndRecreateAlgo.setRuinStrategyManager(new RuinStrategyManager());
		
		BestInsertion recreationStrategy = new BestInsertion();
		ruinAndRecreateAlgo.setRecreationStrategy(recreationStrategy);
		
		RadialRuin radialRuin = new RadialRuin(vrp, new AvgDistanceBetweenJobs(vrp.getCosts()));
		radialRuin.setFractionOfAllNodes(0.2);
		
		RandomRuin randomRuin = new RandomRuin(vrp);
		randomRuin.setFractionOfAllNodes2beRuined(0.3);
		
		ruinAndRecreateAlgo.getRuinStrategyManager().addStrategy(radialRuin, 0.5);
		ruinAndRecreateAlgo.getRuinStrategyManager().addStrategy(randomRuin, 0.5);
		ruinAndRecreateAlgo.setThresholdFunction(new SchrimpfsRRThresholdFunction(0.1));
		
		for(RuinAndRecreateListener l : ruinAndRecreationListeners){
			ruinAndRecreateAlgo.getListeners().add(l);
		}
		
		return ruinAndRecreateAlgo;
	}
	

	public void setIterations(int iterations) {
		this.iterations = iterations;
	}

	public void setWarmUp(int nOfWarmUpIterations) {
		this.warmUp = nOfWarmUpIterations;
		
	}

	public void addRuinAndRecreateListener(RuinAndRecreateListener l){
		ruinAndRecreationListeners.add(l);
	}

}
