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
import org.matsim.contrib.freight.vrp.algorithms.rr.serviceProvider.ServiceProviderAgent;
import org.matsim.contrib.freight.vrp.algorithms.rr.serviceProvider.ServiceProviderAgentFactory;
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

	private RuinAndRecreateSolution currentSolution;

	private ThresholdFunction thresholdFunction;

	private Double initialThreshold = 0.0;

	private ServiceProviderAgentFactory tourAgentFactory;

	private InitialSolutionFactory initialSolutionFactory;

	private Collection<RuinAndRecreateListener> controlerListeners = new ArrayList<RuinAndRecreateListener>();

	private int lastPrint = 1;

	private VehicleRoutingProblemSolution iniVehicleRoutingSolution;

	public RuinAndRecreate(VehicleRoutingProblem vrp) {
		this.vrp = vrp;
	}

	public RuinAndRecreate(VehicleRoutingProblem vrp, RuinAndRecreateSolution initialSolution) {
		this.vrp = vrp;
		this.currentSolution = initialSolution;
	}

	public void setInitialSolutionFactory(InitialSolutionFactory initialSolutionFactory) {
		this.initialSolutionFactory = initialSolutionFactory;
	}

	public void setRecreationStrategy(RecreationStrategy recreationStrategy) {
		this.recreationStrategy = recreationStrategy;
	}

	public void setTourAgentFactory(ServiceProviderAgentFactory tourAgentFactory) {
		this.tourAgentFactory = tourAgentFactory;
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
		return createSolution();
	}

	private VehicleRoutingProblemSolution createSolution() {
		final Collection<VehicleRoute> routes = new ArrayList<VehicleRoute>();
		for (ServiceProviderAgent spa : currentSolution.getTourAgents()) {
			if (spa.isActive())
				routes.add(new VehicleRoute(spa.getTour(), spa.getVehicle()));
		}

		return new VehicleRoutingProblemSolution() {

			@Override
			public Collection<VehicleRoute> getRoutes() {
				return routes;
			}

			@Override
			public double getTotalCost() {
				return currentSolution.getResult();
			}
		};
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
			Collection<ServiceProviderAgent> agents = copySolution(currentSolution.getTourAgents());
			double result2beat = currentSolution.getResult() + thresholdFunction.getThreshold(currentIteration);
			ruinAndRecreate(agents, result2beat);
			double tentativeResult = getResult(agents);
			double currentResult = currentSolution.getResult();
			if (tentativeResult < currentResult + thresholdFunction.getThreshold(currentIteration)) {
				currentSolution = new RuinAndRecreateSolution(agents, tentativeResult);
			}
			printNoIteration(currentIteration);
			informIterationEnds(currentIteration, currentSolution, new RuinAndRecreateSolution(agents, tentativeResult));
			currentIteration++;
		}
		informAlgoEnds(currentSolution);
		logger.info("done");
	}


	private double getResult(Collection<ServiceProviderAgent> agents) {
		double cost = 0.0;
		for(ServiceProviderAgent a : agents){
			cost += a.getTourCost();
		}
		return cost;
	}

	private void informMainRunStarts() {
		for (RuinAndRecreateListener l : controlerListeners) {
			if (l instanceof MainRunStartsListener) {
				((MainRunStartsListener) l).informMainRunStarts(currentSolution);
			}
		}
	}

	private void informIterationEnds(int currentIteration,RuinAndRecreateSolution awardedSolution,RuinAndRecreateSolution rejectedSolution) {
		for (RuinAndRecreateListener l : controlerListeners) {
			if (l instanceof IterationEndsListener) {
				((IterationEndsListener) l).informIterationEnds(
						currentIteration, awardedSolution, rejectedSolution);
			}
		}
	}

	private void informAlgoEnds(RuinAndRecreateSolution currentSolution) {
		for (RuinAndRecreateListener l : controlerListeners) {
			if (l instanceof AlgorithmEndsListener) {
				((AlgorithmEndsListener) l)
						.informAlgorithmEnds(currentSolution);
			}
		}
	}

	private void informIterationStarts(int currentIteration,RuinAndRecreateSolution currentSolution) {
		for (RuinAndRecreateListener l : controlerListeners) {
			if (l instanceof IterationStartListener) {
				((IterationStartListener) l).informIterationStarts(
						currentIteration, currentSolution);
			}
		}
	}

	public Collection<RuinAndRecreateListener> getControlerListeners() {
		return controlerListeners;
	}

	private void informWarmupStarts() {
		for (RuinAndRecreateListener l : controlerListeners) {
			if (l instanceof WarmupStartsListener) {
				((WarmupStartsListener) l).informWarmupStarts(currentSolution);
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
		if(iniVehicleRoutingSolution != null){
			logger.info("initial solution already set");
			currentSolution = makeSolution(iniVehicleRoutingSolution);
		}
		else if (currentSolution == null) {
			logger.info("create initial solution with =" + initialSolutionFactory.getClass().toString());
			currentSolution = initialSolutionFactory.createInitialSolution(vrp);
		}
		else{
			logger.info("initial solution already set");
		}
	}

	public void setCurrentSolution(RuinAndRecreateSolution currentSolution) {
		this.currentSolution = currentSolution;
	}
	
	public void setCurrentSolution(VehicleRoutingProblemSolution solution){
		this.iniVehicleRoutingSolution = solution;
	}

	private RuinAndRecreateSolution makeSolution(VehicleRoutingProblemSolution solution) {
		List<ServiceProviderAgent> agents = new ArrayList<ServiceProviderAgent>();
		for(VehicleRoute route : solution.getRoutes()){
			agents.add(tourAgentFactory.createAgent(route.getVehicle(), route.getDriver(), route.getTour()));
		}
		RuinAndRecreateSolution sol = new RuinAndRecreateSolution(agents, solution.getTotalCost());
		return sol;
	}

	private void logStrats() {
		for (Tuple<RuinStrategy, Double> t : ruinStrategyManager
				.getStrategies()) {
			logger.info("strat=" + t.getFirst().getClass().toString()
					+ "; prob=" + t.getSecond());
		}
	}

	private Collection<ServiceProviderAgent> copySolution(Collection<ServiceProviderAgent> agents2copy) {
		List<ServiceProviderAgent> agents = new ArrayList<ServiceProviderAgent>();
		for (ServiceProviderAgent agent : agents2copy) {
			ServiceProviderAgent newTourAgent = tourAgentFactory.createAgent(agent);
			agents.add(newTourAgent);
		}
		return agents;
	}

	private void verify() {
		if(iniVehicleRoutingSolution != null){
			return;
		}
		if(currentSolution != null){
			return;
		}
		if (initialSolutionFactory == null)
			throw new IllegalStateException("no initialsolutionFactory set");
	}

	private void init() {
		thresholdFunction.setNofIterations(nOfIterations);
		thresholdFunction.setInitialThreshold(initialThreshold);
	}

	private void ruinAndRecreate(Collection<ServiceProviderAgent> tourAgents, double result2beat) {
		RuinStrategy ruinStrategy = ruinStrategyManager.getRandomStrategy();
		informRuinStarts(ruinStrategy, new RuinAndRecreateSolution(tourAgents, currentSolution.getResult()));
		Collection<Job> unassignedJobs = ruinStrategy.ruin(tourAgents);
		informRuinEnds(tourAgents);
		informRecreationStarts(tourAgents, unassignedJobs);
		recreationStrategy.recreate(tourAgents, unassignedJobs,result2beat);
		informRecreationEnds(tourAgents);
	}

	private void informRecreationEnds(Collection<ServiceProviderAgent> tourAgents) {
		for (RuinAndRecreateListener l : controlerListeners) {
			if (l instanceof RecreationEndsListener) {
				((RecreationEndsListener) l).informRecreationEnds(tourAgents);
			}
		}
	}

	private void informRecreationStarts(Collection<ServiceProviderAgent> tourAgents,
			Collection<Job> unassignedJobs) {
		for (RuinAndRecreateListener l : controlerListeners) {
			if (l instanceof RecreationStartsListener) {
				((RecreationStartsListener) l).informRecreationStarts(tourAgents,unassignedJobs);
			}
		}
	}

	private void informRuinEnds(Collection<ServiceProviderAgent> tourAgents) {
		for (RuinAndRecreateListener l : controlerListeners) {
			if (l instanceof RuinEndsListener) {
				((RuinEndsListener) l).informRuinEnds(tourAgents);
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
		RuinAndRecreateSolution lastSolution = new RuinAndRecreateSolution(copySolution(currentSolution.getTourAgents()),currentSolution.getResult());
		resetIterations();
		double[] results = new double[nOfIterations];
		for (int i = 0; i < nOfIterations; i++) {
			printNoIteration(i);
			Collection<ServiceProviderAgent> agents = copySolution(lastSolution.getTourAgents());
			ruinAndRecreate(agents, Double.MAX_VALUE);
			double result = getResult(agents);
			results[i] = result;
			lastSolution = new RuinAndRecreateSolution(agents, result);
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

	public RuinAndRecreateSolution getSolution() {
		return currentSolution;
	}

	public void setIterations(int iterations) {
		nOfIterations = iterations;
	}

}
