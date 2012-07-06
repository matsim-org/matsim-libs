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
import org.matsim.contrib.freight.vrp.algorithms.rr.serviceProvider.ServiceProviderAgent;
import org.matsim.contrib.freight.vrp.algorithms.rr.serviceProvider.ServiceProviderAgentFactory;
import org.matsim.contrib.freight.vrp.basics.Job;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblem;
import org.matsim.contrib.freight.vrp.basics.VrpType;
import org.matsim.core.utils.collections.Tuple;



/**
 * This algorithm is basically the implementation of 
 * Schrimpf G., J. Schneider, Hermann Stamm-Wilbrandt and Gunter Dueck (2000): Record Breaking Optimization Results Using 
 * the Ruin and Recreate Principle, Journal of Computational Physics 159, 139-171 (2000).
 * 
 * @author stefan schroeder
 *
 */

public final class RuinAndRecreate {
	
	private static Logger logger = Logger.getLogger(RuinAndRecreate.class);
	
	private RuinStrategyManager ruinStrategyManager; 

	private RecreationStrategy recreationStrategy;
	
	private VehicleRoutingProblem vrp;
	
	private int nOfIterations = 100;
	
	private int warmUpIterations = 10;
	
	private int currentIteration = 0;
	
	private RuinAndRecreateSolution currentSolution;
	
	private ThresholdFunction thresholdFunction;
	
	private double currentThreshold;
	
	private Double initialThreshold = 0.0;
	
	private ServiceProviderAgentFactory tourAgentFactory;
	
	private InitialSolutionFactory initialSolutionFactory;

	private Collection<RuinAndRecreateListener> listeners = new ArrayList<RuinAndRecreateListener>();
	
	private Collection<RuinAndRecreateControlerListener> controlerListeners = new ArrayList<RuinAndRecreateControlerListener>();
	
	private int lastPrint = 1;

	private VrpType vrpType;
	
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

	public void run(){
		verify();
		init();
		informAlgoStarts();
		logger.info("run ruin-and-recreate");
		logger.info("initialConstruction=" + initialSolutionFactory.getClass().toString());
		logger.info("recreation:");
		logger.info("strat=" + recreationStrategy.getClass().toString());
		logger.info("ruin:");
		logStrats();
		logger.info("#warmupIterations="+warmUpIterations+ "; #iterations="+nOfIterations);
		logger.info("#jobs: " + vrp.getJobs().values().size());
		logger.info("create initial solution");
		makeInitialSolution();
		logger.info("warmup");
		informWarmupStarts();
		randomWalk(warmUpIterations);
		logger.info("go");
		resetIterations();
		while(currentIteration < nOfIterations){
			informIterationStarts(currentIteration,currentSolution);
			RuinAndRecreateSolution tentativeSolution = copySolution(currentSolution);
			double result2beat = currentSolution.getResult() + thresholdFunction.getThreshold(currentIteration);
			ruinAndRecreate(tentativeSolution, result2beat);
			double tentativeResult = tentativeSolution.getResult();
			double currentResult = currentSolution.getResult();
			boolean isAccepted = false;
			if(tentativeResult < currentResult + thresholdFunction.getThreshold(currentIteration)){
				currentSolution = tentativeSolution;
				isAccepted = true;
			}
			informListener(currentIteration, tentativeResult, currentResult, currentThreshold, isAccepted);
			printNoIteration(currentIteration);
			informIterationEnds(currentIteration);
			currentIteration++;
		}
		informAlgoEnds();
		informFinish();
		logger.info("done");
	}

	private void informAlgoEnds() {
		// TODO Auto-generated method stub
		
	}

	private void informIterationEnds(int currentIteration2) {
		// TODO Auto-generated method stub
		
	}

	private void informIterationStarts(int currentIteration, RuinAndRecreateSolution currentSolution) {
		for(RuinAndRecreateControlerListener l : controlerListeners){
			if(l instanceof IterationStartListener){
				((IterationStartListener) l).informIterationStarts(currentIteration, currentSolution);
			}
		}
		
	}

	public Collection<RuinAndRecreateControlerListener> getControlerListeners() {
		return controlerListeners;
	}

	private void informWarmupStarts() {
		// TODO Auto-generated method stub
		
	}

	private void informAlgoStarts() {
		// TODO Auto-generated method stub
		
	}

	private void makeInitialSolution(){
		if(currentSolution == null){
			currentSolution = initialSolutionFactory.createInitialSolution(vrp);
		}
	}

	private void logStrats() {
		for(Tuple<RuinStrategy,Double> t : ruinStrategyManager.getStrategies()){
			logger.info("strat="+t.getFirst().getClass().toString() + "; prob="+t.getSecond());
		}
	}

	private RuinAndRecreateSolution copySolution(RuinAndRecreateSolution currentSolution) {
		List<ServiceProviderAgent> agents = new ArrayList<ServiceProviderAgent>();
		for(ServiceProviderAgent agent : currentSolution.getTourAgents()){
			ServiceProviderAgent newTourAgent = tourAgentFactory.createAgent(agent);
			agents.add(newTourAgent);
		}
		return new RuinAndRecreateSolution(agents);
	}

	private void verify() {
		if(initialSolutionFactory == null) throw new IllegalStateException("no initialsolutionFactory set");	
	}

	private void init() {
		thresholdFunction.setNofIterations(nOfIterations);
		thresholdFunction.setInitialThreshold(initialThreshold);	
	}

	private void ruinAndRecreate(RuinAndRecreateSolution solution, double upperBound) {
		informRuinStarts();
		RuinStrategy ruinStrategy = ruinStrategyManager.getRandomStrategy();
		Collection<Job> unassignedJobs = ruinStrategy.ruin(solution.getTourAgents());
		informRuinEnds();
		informRecreationStarts();
		recreationStrategy.recreate(solution.getTourAgents(),unassignedJobs);
		informRecreationEnds();
	}

	private void informRecreationEnds() {
		// TODO Auto-generated method stub
		
	}

	private void informRecreationStarts() {
		// TODO Auto-generated method stub
		
	}

	private void informRuinEnds() {
		for(RuinAndRecreateControlerListener l : controlerListeners){
			if(l instanceof RuinEndsListener){
				((RuinEndsListener)l).informRuinEnds();
			}
		}
		
		
	}

	private void informRuinStarts() {
		
	}

	private void randomWalk(int nOfIterations) {
		if(nOfIterations == 0){
			return;
		}
		RuinAndRecreateSolution lastSolution = copySolution(currentSolution);
		resetIterations();
		double[] results = new double[nOfIterations];
		for(int i=0;i<nOfIterations;i++){
			printNoIteration(i);
			RuinAndRecreateSolution currentSolution = copySolution(lastSolution);
			ruinAndRecreate(currentSolution, Double.MAX_VALUE);
			double result = currentSolution.getResult();
			results[i]=result;
			lastSolution = currentSolution;
		}
		StandardDeviation dev = new StandardDeviation();
		double standardDeviation = dev.evaluate(results);
		initialThreshold = standardDeviation / 2;
		thresholdFunction.setInitialThreshold(initialThreshold);
		logger.info("iniThreshold="+initialThreshold);
	}

	private void printNoIteration(int currentIteration) {
		if(currentIteration == 0){
			logger.info(currentIteration + " iterations");
		}
		else if((currentIteration%lastPrint) == 0){
			logger.info(currentIteration + " iterations");
			lastPrint = currentIteration;
		}
	}

	private void resetIterations() {
		lastPrint = 1;
	}

	public void informFinish(){
		for(RuinAndRecreateListener l : listeners){
			l.finish();
		}
	}

	private void informListener(int currentMutation, double tentativeResult,double currentResult, double currentThreshold, boolean isAccepted) {
		RuinAndRecreateEvent event = new RuinAndRecreateEvent(currentMutation, tentativeResult, currentResult, currentThreshold, isAccepted);
		event.setCurrentSolution(getSolution());
		for(RuinAndRecreateListener l : listeners){
			l.inform(event);
		}
	}

	public Collection<RuinAndRecreateListener> getListeners() {
		return listeners;
	}
	
	public RuinAndRecreateSolution getSolution(){
		return currentSolution;
	}

	public void setIterations(int iterations) {
		nOfIterations = iterations;
	}

	
}
