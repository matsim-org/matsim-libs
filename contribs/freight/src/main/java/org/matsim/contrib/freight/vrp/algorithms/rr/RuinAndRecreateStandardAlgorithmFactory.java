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

import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators.RouteAgentFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.iniSolution.InitialSolutionBestInsertion;
import org.matsim.contrib.freight.vrp.algorithms.rr.iniSolution.InitialSolutionFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.recreate.RecreationBestInsertion;
import org.matsim.contrib.freight.vrp.algorithms.rr.ruin.JobDistance;
import org.matsim.contrib.freight.vrp.algorithms.rr.ruin.JobDistanceAvgCosts;
import org.matsim.contrib.freight.vrp.algorithms.rr.ruin.RuinRadial;
import org.matsim.contrib.freight.vrp.algorithms.rr.ruin.RuinRandom;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblem;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblemSolution;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblemSolver;
import org.matsim.contrib.freight.vrp.utils.RandomNumberGeneration;

public class RuinAndRecreateStandardAlgorithmFactory implements RuinAndRecreateFactory {

	private static Logger logger = Logger.getLogger(RuinAndRecreateStandardAlgorithmFactory.class);

	private Random random = RandomNumberGeneration.getRandom();

	private final RouteAgentFactory routeAgentFactory;

	public int warmup = 50;

	public int iterations = 1000;

	public String jobDistance = "vrpCost";

	public RuinAndRecreateStandardAlgorithmFactory(RouteAgentFactory routeAgentFactory) {
		super();
		this.routeAgentFactory = routeAgentFactory;
	}

	public void setRandom(Random random) {
		this.random = random;
	}

	@Override
	public final RuinAndRecreate createAlgorithm(VehicleRoutingProblem vrp) {
		RuinAndRecreate ruinAndRecreateAlgo = new RuinAndRecreate(vrp);
		InitialSolutionFactory iniSolutionFactory = new InitialSolutionBestInsertion(routeAgentFactory);
		ruinAndRecreateAlgo.setInitialSolutionFactory(iniSolutionFactory);
		ruinAndRecreateAlgo.setIterations(iterations);
		ruinAndRecreateAlgo.setWarmUpIterations(warmup);
		ruinAndRecreateAlgo.setRuinStrategyManager(new RuinStrategyManager());

		RecreationBestInsertion bestInsertion = new RecreationBestInsertion(routeAgentFactory);
		bestInsertion.setRandom(random);
		ruinAndRecreateAlgo.setRecreationStrategy(bestInsertion);

		RuinRadial radialRuin = new RuinRadial(vrp, routeAgentFactory, getJobDistance(vrp));
		radialRuin.setRuinFraction(0.3);
		radialRuin.setRandom(random);

		RuinRandom randomRuin = new RuinRandom(vrp, routeAgentFactory);
		randomRuin.setRuinFraction(0.5);
		randomRuin.setRandom(random);

		ruinAndRecreateAlgo.getRuinStrategyManager().addStrategy(radialRuin,0.5);
		ruinAndRecreateAlgo.getRuinStrategyManager().addStrategy(randomRuin,0.5);

		ruinAndRecreateAlgo.setThresholdFunction(new ThresholdFunctionSchrimpf(0.1));

		return ruinAndRecreateAlgo;
	}

	private JobDistance getJobDistance(VehicleRoutingProblem vrp) {
		if (jobDistance.equals("vrpCost")) {
			return new JobDistanceAvgCosts(vrp.getCosts());
		} else {
			throw new IllegalStateException("jobDistance " + jobDistance
					+ " not supported");
		}
	}

	@Override
	public VehicleRoutingProblemSolver createSolver(VehicleRoutingProblem vrp) {
		return createAlgorithm(vrp);
	}

	@Override
	public VehicleRoutingProblemSolver createSolver(VehicleRoutingProblem vrp,VehicleRoutingProblemSolution initialSolution) {
		if(initialSolution == null){
			return createAlgorithm(vrp);
		}
		RuinAndRecreate ruinAndRecreateAlgo = new RuinAndRecreate(vrp);
		ruinAndRecreateAlgo.setCurrentSolution(initialSolution);
		
		ruinAndRecreateAlgo.setIterations(iterations);
		ruinAndRecreateAlgo.setWarmUpIterations(warmup);
		ruinAndRecreateAlgo.setRuinStrategyManager(new RuinStrategyManager());

		RecreationBestInsertion bestInsertion = new RecreationBestInsertion(routeAgentFactory);
		bestInsertion.setRandom(random);
		ruinAndRecreateAlgo.setRecreationStrategy(bestInsertion);

		RuinRadial radialRuin = new RuinRadial(vrp, routeAgentFactory, getJobDistance(vrp));
		radialRuin.setRuinFraction(0.3);
		radialRuin.setRandom(random);

		RuinRandom randomRuin = new RuinRandom(vrp, routeAgentFactory);
		randomRuin.setRuinFraction(0.5);
		randomRuin.setRandom(random);

		ruinAndRecreateAlgo.getRuinStrategyManager().addStrategy(radialRuin,0.5);
		ruinAndRecreateAlgo.getRuinStrategyManager().addStrategy(randomRuin,0.5);

		ruinAndRecreateAlgo.setThresholdFunction(new ThresholdFunctionSchrimpf(0.1));

		return ruinAndRecreateAlgo;

	}

}
