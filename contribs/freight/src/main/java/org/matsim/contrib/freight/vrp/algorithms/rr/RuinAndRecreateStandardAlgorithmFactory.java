/*******************************************************************************
 * Copyright (c) 2011 Stefan Schroeder.
 * eMail: stefan.schroeder@kit.edu
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Stefan Schroeder - initial API and implementation
 ******************************************************************************/
package org.matsim.contrib.freight.vrp.algorithms.rr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.contrib.freight.vrp.algorithms.rr.serviceProvider.ServiceProviderAgent;
import org.matsim.contrib.freight.vrp.algorithms.rr.serviceProvider.ServiceProviderAgentFactory;
import org.matsim.contrib.freight.vrp.basics.Driver;
import org.matsim.contrib.freight.vrp.basics.DriverImpl;
import org.matsim.contrib.freight.vrp.basics.TourPlan;
import org.matsim.contrib.freight.vrp.basics.Vehicle;
import org.matsim.contrib.freight.vrp.basics.VehicleRoute;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblem;
import org.matsim.contrib.freight.vrp.utils.RandomNumberGeneration;

public class RuinAndRecreateStandardAlgorithmFactory implements RuinAndRecreateFactory {

	private static Logger logger = Logger.getLogger(RuinAndRecreateStandardAlgorithmFactory.class);
	
	private Collection<RuinAndRecreateListener> ruinAndRecreationListeners = new ArrayList<RuinAndRecreateListener>();

	private Random random = RandomNumberGeneration.getRandom();
	
	private final ServiceProviderAgentFactory serviceProviderFactory;
	
	public int warmup = 50;
	
	public int iterations = 1000;
	
	public String jobDistance = "vrpCost";
	

	public RuinAndRecreateStandardAlgorithmFactory(ServiceProviderAgentFactory serviceProviderFactory) {
		super();
		this.serviceProviderFactory = serviceProviderFactory;
	}

	public void setRandom(Random random) {
		this.random = random;
	}

	
	public void addRuinAndRecreateListener(RuinAndRecreateListener l){
		ruinAndRecreationListeners.add(l);
	}
	

	@Override
	public final RuinAndRecreate createAlgorithm(VehicleRoutingProblem vrp) {

		RuinAndRecreate ruinAndRecreateAlgo = new RuinAndRecreate(vrp);
		InitialSolutionFactory iniSolutionFactory = new InitialSolutionBestInsertion(serviceProviderFactory);
		ruinAndRecreateAlgo.setInitialSolutionFactory(iniSolutionFactory);
		ruinAndRecreateAlgo.setIterations(iterations);
		ruinAndRecreateAlgo.setWarmUpIterations(warmup);
		ruinAndRecreateAlgo.setTourAgentFactory(serviceProviderFactory);
		ruinAndRecreateAlgo.setRuinStrategyManager(new RuinStrategyManager());
		
		RecreationBestInsertion bestInsertion = new RecreationBestInsertion();
		bestInsertion.setRandom(random);
		ruinAndRecreateAlgo.setRecreationStrategy(bestInsertion);
		
		RuinRadial radialRuin = new RuinRadial(vrp, getJobDistance(vrp));
		radialRuin.setRuinFraction(0.3);
		radialRuin.setRandom(random);
		
		RuinRandom randomRuin = new RuinRandom(vrp);
		randomRuin.setRuinFraction(0.5);
		randomRuin.setRandom(random);
		
		ruinAndRecreateAlgo.getRuinStrategyManager().addStrategy(radialRuin, 0.5);
		ruinAndRecreateAlgo.getRuinStrategyManager().addStrategy(randomRuin, 0.5);
		
		ruinAndRecreateAlgo.setThresholdFunction(new ThresholdFunctionSchrimpf(0.1));
		
		for(RuinAndRecreateListener l : ruinAndRecreationListeners){
			ruinAndRecreateAlgo.getListeners().add(l);
		}
		
		
		return ruinAndRecreateAlgo;
	}

	private JobDistance getJobDistance(VehicleRoutingProblem vrp) {
		if(jobDistance.equals("vrpCost")){
			return new JobDistanceAvgCosts(vrp.getCosts());
		}
		else{
			throw new IllegalStateException("jobDistance " + jobDistance + " not supported");
		}
	}
	
	@Override
	public final RuinAndRecreate createAlgorithm(TourPlan initialSolution, VehicleRoutingProblem vrp) {
		RuinAndRecreateSolution rrSolution = makeRRSolution(vrp,initialSolution);
		RuinAndRecreate ruinAndRecreateAlgo = new RuinAndRecreate(vrp,rrSolution);
		ruinAndRecreateAlgo.setWarmUpIterations(warmup);
		ruinAndRecreateAlgo.setIterations(iterations);
		ruinAndRecreateAlgo.setTourAgentFactory(serviceProviderFactory);
		ruinAndRecreateAlgo.setRuinStrategyManager(new RuinStrategyManager());
		
		RecreationBestInsertion recreationStrategy = new RecreationBestInsertion();
		recreationStrategy.setRandom(random);
		
		ruinAndRecreateAlgo.setRecreationStrategy(recreationStrategy);
		
		RuinRadial radialRuin = new RuinRadial(vrp, getJobDistance(vrp));
		radialRuin.setRuinFraction(0.3);
		radialRuin.setRandom(random);
		
		RuinRandom randomRuin = new RuinRandom(vrp);
		randomRuin.setRuinFraction(0.5);
		randomRuin.setRandom(random);
		
		ruinAndRecreateAlgo.getRuinStrategyManager().addStrategy(radialRuin, 0.5);
		ruinAndRecreateAlgo.getRuinStrategyManager().addStrategy(randomRuin, 0.5);
		
		ruinAndRecreateAlgo.setThresholdFunction(new ThresholdFunctionSchrimpf(0.1));
		
		for(RuinAndRecreateListener l : ruinAndRecreationListeners){
			ruinAndRecreateAlgo.getListeners().add(l);
		}
		
		return ruinAndRecreateAlgo;
	}

	private RuinAndRecreateSolution makeRRSolution(VehicleRoutingProblem vrp, TourPlan initialSolution) {
			List<ServiceProviderAgent> agents = new ArrayList<ServiceProviderAgent>();
			LinkedList<Vehicle> vehicles = new LinkedList<Vehicle>(vrp.getVehicles());
			for(VehicleRoute r : initialSolution.getVehicleRoutes()){
				ServiceProviderAgent agent = serviceProviderFactory.createAgent(r.getVehicle(), new Driver(){}, r.getTour()); 
				agents.add(agent);
				vehicles.remove(r.getVehicle());
			}
			//empty vehicles
			for(Vehicle v : vehicles){
				DriverImpl driver = new DriverImpl("driver");
				driver.setEarliestStart(v.getEarliestDeparture());
				driver.setLatestEnd(v.getLatestArrival());
				driver.setHomeLocation(v.getLocationId());
				ServiceProviderAgent agent = serviceProviderFactory.createAgent(v, driver);
				agents.add(agent);
			}
			RuinAndRecreateSolution rrSolution = new RuinAndRecreateSolution(agents);
			rrSolution.setScore((-1)*initialSolution.getScore());
			return rrSolution;
	
	}
	
}
