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
import java.util.List;

import org.apache.commons.math.stat.descriptive.moment.StandardDeviation;
import org.apache.log4j.Logger;
import org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators.RouteAgent;
import org.matsim.contrib.freight.vrp.algorithms.rr.iniSolution.InitialSolutionFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.listener.AlgorithmEndsListener;
import org.matsim.contrib.freight.vrp.algorithms.rr.listener.AlgorithmStartsListener;
import org.matsim.contrib.freight.vrp.algorithms.rr.listener.IterationEndsListener;
import org.matsim.contrib.freight.vrp.algorithms.rr.listener.IterationStartListener;
import org.matsim.contrib.freight.vrp.algorithms.rr.listener.MainRunStartsListener;
import org.matsim.contrib.freight.vrp.algorithms.rr.listener.RecreationEndsListener;
import org.matsim.contrib.freight.vrp.algorithms.rr.listener.RecreationStartsListener;
import org.matsim.contrib.freight.vrp.algorithms.rr.listener.RuinAndRecreateListener;
import org.matsim.contrib.freight.vrp.algorithms.rr.listener.RuinEndsListener;
import org.matsim.contrib.freight.vrp.algorithms.rr.listener.RuinStartsListener;
import org.matsim.contrib.freight.vrp.algorithms.rr.listener.WarmupStartsListener;
import org.matsim.contrib.freight.vrp.algorithms.rr.recreate.RecreationStrategy;
import org.matsim.contrib.freight.vrp.algorithms.rr.ruin.RuinStrategy;
import org.matsim.contrib.freight.vrp.basics.Job;
import org.matsim.contrib.freight.vrp.basics.VehicleRoute;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblem;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblemSolution;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblemSolver;
import org.matsim.core.utils.collections.Tuple;

/**
 * This algorithm is basically the implementation of Schrimpf G., J. Schneider,
 * Hermann Stamm-Wilbrandt and Gunter Dueck (2000): Record Breaking Optimization
 * Results Using the Ruin and Recreate Principle, Journal of Computational
 * Physics 159, 139-171 (2000).
 * 
 * @author stefan schroeder
 * 
 */

public final class RuinAndRecreate implements VehicleRoutingProblemSolver {

	private static Logger logger = Logger.getLogger(RuinAndRecreate.class);

	private RuinStrategyManager ruinStrategyManager;

	private RecreationStrategy recreationStrategy;

	private VehicleRoutingProblem vrp;

	private int nOfIterations = 100;

	private int warmUpIterations = 10;

	private int currentIteration = 0;

//	private RuinAndRecreateSolution currentSolution;
	
	private VehicleRoutingProblemSolution currentSolution;

	private ThresholdFunction thresholdFunction;

	private Double initialThreshold = 0.0;

	private InitialSolutionFactory initialSolutionFactory;

	private Collection<RuinAndRecreateListener> controlerListeners = new ArrayList<RuinAndRecreateListener>();

	private int lastPrint = 1;

	public RuinAndRecreate(VehicleRoutingProblem vrp) {
		this.vrp = vrp;
	}

	public void setInitialSolutionFactory(InitialSolutionFactory initialSolutionFactory) {
		this.initialSolutionFactory = initialSolutionFactory;
	}

	public void setRecreationStrategy(RecreationStrategy recreationStrategy) {
		this.recreationStrategy = recreationStrategy;
	}

	public void setThresholdFunction(ThresholdFunction thresholdFunction) {
		this.thresholdFunction = thresholdFunction;
	}

	public void setWarmUpIterations(int warmUpIterations) {
		this.warmUpIterations = warmUpIterations;
	}

	public RuinStrategyManager getRuinStrategyManager() {
		return ruinStrategyManager;
	}

	public void setRuinStrategyManager(RuinStrategyManager ruinStrategyManager) {
		this.ruinStrategyManager = ruinStrategyManager;
	}

	@Override
	public VehicleRoutingProblemSolution solve() {
		run();
		Collection<VehicleRoute> routes = new ArrayList<VehicleRoute>();
		routes.addAll(currentSolution.getRoutes());
		return new VehicleRoutingProblemSolution(routes, currentSolution.getTotalCost());
	}

	public void run() {
		verify();
		init();
		informAlgoStarts();
		logger.info("run ruin-and-recreate");
		logger.info("recreation:");
		logger.info("strat=" + recreationStrategy.getClass().toString());
		logger.info("ruin:");
		logStrats();
		logger.info("#warmupIterations=" + warmUpIterations + "; #iterations="+ nOfIterations);
		logger.info("#jobs: " + vrp.getJobs().values().size());
		makeInitialSolution();
		logger.info("warmup");
		informWarmupStarts();
		randomWalk(warmUpIterations);
		logger.info("go");
		resetIterations();
		informMainRunStarts();
		while (currentIteration < nOfIterations) {
			informIterationStarts(currentIteration, currentSolution);
			Collection<VehicleRoute> routes = copySolution(currentSolution.getRoutes());
			double result2beat = currentSolution.getTotalCost() + thresholdFunction.getThreshold(currentIteration);
			ruinAndRecreate(routes, result2beat);
			double tentativeResult = getResult(routes);
			double currentResult = currentSolution.getTotalCost();
//			logger.info("COMP: tent: " + tentativeResult + " old: " + currentResult);
			if (tentativeResult < currentResult + thresholdFunction.getThreshold(currentIteration)) {
				logger.info("BING - new: " + tentativeResult + " old: " + currentResult);
				currentSolution = new VehicleRoutingProblemSolution(routes, tentativeResult);
				
			}
			printNoIteration(currentIteration);
			informIterationEnds(currentIteration, currentSolution, new VehicleRoutingProblemSolution(routes, tentativeResult));
			currentIteration++;
		}
		informAlgoEnds(currentSolution);
		logger.info("done");
	}


	private double getResult(Collection<VehicleRoute> routes) {
		double cost = 0.0;
		for(VehicleRoute route : routes){
			cost += route.getCost();
		}
		return cost;
	}

	private void informMainRunStarts() {
		for (RuinAndRecreateListener l : controlerListeners) {
			if (l instanceof MainRunStartsListener) {
//				((MainRunStartsListener) l).informMainRunStarts(currentSolution);
			}
		}
	}

	private void informIterationEnds(int currentIteration, VehicleRoutingProblemSolution awardedSolution, VehicleRoutingProblemSolution rejectedSolution) {
		for (RuinAndRecreateListener l : controlerListeners) {
			if (l instanceof IterationEndsListener) {
				((IterationEndsListener) l).informIterationEnds(currentIteration, awardedSolution, rejectedSolution);
			}
		}
	}

	private void informAlgoEnds(VehicleRoutingProblemSolution currentSolution) {
		for (RuinAndRecreateListener l : controlerListeners) {
			if (l instanceof AlgorithmEndsListener) {
				((AlgorithmEndsListener) l).informAlgorithmEnds(currentSolution);
			}
		}
	}

	private void informIterationStarts(int currentIteration,VehicleRoutingProblemSolution currentSolution) {
		for (RuinAndRecreateListener l : controlerListeners) {
			if (l instanceof IterationStartListener) {
				((IterationStartListener) l).informIterationStarts(currentIteration, currentSolution);
			}
		}
	}

	public Collection<RuinAndRecreateListener> getControlerListeners() {
		return controlerListeners;
	}

	private void informWarmupStarts() {
		for (RuinAndRecreateListener l : controlerListeners) {
			if (l instanceof WarmupStartsListener) {
//				((WarmupStartsListener) l).informWarmupStarts(currentSolution);
			}
		}
	}

	private void informAlgoStarts() {
		for (RuinAndRecreateListener l : controlerListeners) {
			if (l instanceof AlgorithmStartsListener) {
				((AlgorithmStartsListener) l).informAlgorithmStarts(this);
			}
		}
	}

	private void makeInitialSolution() {
		if (currentSolution == null) {
			logger.info("create initial solution with =" + initialSolutionFactory.getClass().toString());
			currentSolution = initialSolutionFactory.createInitialSolution(vrp);
		}
		else{
			logger.info("initial solution already set");
		}
	}
	
	public void setCurrentSolution(VehicleRoutingProblemSolution solution){
		this.currentSolution = solution;
	}

	private void logStrats() {
		for (Tuple<RuinStrategy, Double> t : ruinStrategyManager
				.getStrategies()) {
			logger.info("strat=" + t.getFirst().getClass().toString()
					+ "; prob=" + t.getSecond());
		}
	}

	private Collection<VehicleRoute> copySolution(Collection<VehicleRoute> routes2copy) {
		List<VehicleRoute> routes = new ArrayList<VehicleRoute>();
		for (VehicleRoute route : routes2copy) {
			VehicleRoute copiedRoute = new VehicleRoute(route.getTour().duplicate(), route.getDriver(), route.getVehicle());
			routes.add(copiedRoute);
		}
		return routes;
	}

	private void verify() {
		if(currentSolution != null){
			return;
		}
		if (initialSolutionFactory == null)
			throw new IllegalStateException("no initialSolutionFactory set");
	}

	private void init() {
		thresholdFunction.setNofIterations(nOfIterations);
		thresholdFunction.setInitialThreshold(initialThreshold);
	}

	private void ruinAndRecreate(Collection<VehicleRoute> vehicleRoutes, double result2beat) {
		RuinStrategy ruinStrategy = ruinStrategyManager.getRandomStrategy();
//		informRuinStarts(ruinStrategy, new RuinAndRecreateSolution(vehicleRoutes, currentSolution.getTotalCost()));
		Collection<Job> unassignedJobs = ruinStrategy.ruin(vehicleRoutes);
		informRuinEnds(vehicleRoutes);
//		informRecreationStarts(vehicleRoutes, unassignedJobs);
		recreationStrategy.recreate(vehicleRoutes, unassignedJobs, result2beat);
//		informRecreationEnds(vehicleRoutes);
	}

	private void informRecreationEnds(Collection<RouteAgent> tourAgents) {
		for (RuinAndRecreateListener l : controlerListeners) {
			if (l instanceof RecreationEndsListener) {
				((RecreationEndsListener) l).informRecreationEnds(tourAgents);
			}
		}
	}

	private void informRecreationStarts(Collection<RouteAgent> tourAgents,
			Collection<Job> unassignedJobs) {
		for (RuinAndRecreateListener l : controlerListeners) {
			if (l instanceof RecreationStartsListener) {
				((RecreationStartsListener) l).informRecreationStarts(tourAgents,unassignedJobs);
			}
		}
	}

	private void informRuinEnds(Collection<VehicleRoute> vehicleRoutes) {
		for (RuinAndRecreateListener l : controlerListeners) {
			if (l instanceof RuinEndsListener) {
				((RuinEndsListener) l).informRuinEnds(vehicleRoutes);
			}
		}
	}

	private void informRuinStarts(RuinStrategy ruinStrategy,RuinAndRecreateSolution solution) {
		for (RuinAndRecreateListener l : controlerListeners) {
			if (l instanceof RuinStartsListener) {
				((RuinStartsListener) l).informRuinStarts(ruinStrategy,solution);
			}
		}
	}

	private void randomWalk(int nOfIterations) {
		if (nOfIterations == 0) {
			return;
		}
//		RuinAndRecreateSolution lastSolution = new RuinAndRecreateSolution(copySolution(currentSolution.getTourAgents()),currentSolution.getResult());
		Collection<VehicleRoute> lastSolution = currentSolution.getRoutes();
		resetIterations();
		double[] results = new double[nOfIterations];
		for (int i = 0; i < nOfIterations; i++) {
			printNoIteration(i);
			Collection<VehicleRoute> routes = copySolution(lastSolution);
//			Collection<RouteAgent> agents = copySolution(lastSolution.getTourAgents());
			ruinAndRecreate(routes, Double.MAX_VALUE);
			double result = getResult(routes);
			results[i] = result;
			lastSolution = routes;
		}
		StandardDeviation dev = new StandardDeviation();
		double standardDeviation = dev.evaluate(results);
		initialThreshold = standardDeviation / 2;
		thresholdFunction.setInitialThreshold(initialThreshold);
		logger.info("iniThreshold=" + initialThreshold);
	}

	private void printNoIteration(int currentIteration) {
		if (currentIteration == 0) {
			logger.info(currentIteration + " iterations");
		} else if ((currentIteration % lastPrint) == 0) {
			logger.info(currentIteration + " iterations");
			lastPrint = currentIteration;
		}
	}

	private void resetIterations() {
		lastPrint = 1;
	}

//	public RuinAndRecreateSolution getSolution() {
//		return currentSolution;
//	}

	public void setIterations(int iterations) {
		nOfIterations = iterations;
	}

}
