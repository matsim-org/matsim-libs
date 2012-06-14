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

import org.apache.commons.math.stat.descriptive.moment.StandardDeviation;
import org.apache.log4j.Logger;
import org.matsim.contrib.freight.vrp.algorithms.rr.recreation.RecreationStrategy;
import org.matsim.contrib.freight.vrp.algorithms.rr.ruin.RuinStrategy;
import org.matsim.contrib.freight.vrp.algorithms.rr.thresholdFunctions.ThresholdFunction;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.RRTourAgentFactory;
import org.matsim.contrib.freight.vrp.basics.TourPlan;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblem;
import org.matsim.contrib.freight.vrp.basics.VrpUtils;



/**
 * This algorithm is basically the implementation of 
 * Schrimpf G., J. Schneider, Hermann Stamm-Wilbrandt and Gunter Dueck (2000): Record Breaking Optimization Results Using 
 * the Ruin and Recreate Principle, Journal of Computational Physics 159, 139-171 (2000).
 * 
 * @author stefan schroeder
 *
 */

public class RuinAndRecreate {
	
	private static Logger logger = Logger.getLogger(RuinAndRecreate.class);
	
	private RuinStrategyManager ruinStrategyManager; 

	private RecreationStrategy recreationStrategy;
	
	private VehicleRoutingProblem vrp;
	
	private int nOfMutations = 100;
	
	private int warmUpIterations = 10;
	
	private int currentMutation = 0;
	
	private RRSolution currentSolution;
	
	private ThresholdFunction thresholdFunction;
	
	private double currentThreshold;
	
	private Double initialThreshold = 0.0;
	
	private RRTourAgentFactory tourAgentFactory;

	private Collection<RuinAndRecreateListener> listeners = new ArrayList<RuinAndRecreateListener>();
	
	private int lastPrint = 1;
	
	private TourPlan iniTourPlan;
	
	public RuinAndRecreate(VehicleRoutingProblem vrp, RRSolution iniSolution, int nOfMutations) {
		this.vrp = vrp;
		this.currentSolution = iniSolution;
		this.nOfMutations = nOfMutations;
	}
	
	public RuinAndRecreate(VehicleRoutingProblem vrp, TourPlan iniTourPlan, int nOfMutations) {
		this.vrp = vrp;
		this.iniTourPlan = iniTourPlan;
		this.nOfMutations = nOfMutations;
	}

	public void setRecreationStrategy(RecreationStrategy recreationStrategy) {
		this.recreationStrategy = recreationStrategy;
	}

	public void setTourAgentFactory(RRTourAgentFactory tourAgentFactory) {
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
		logger.info("run ruin-and-recreate");
		logger.info("#warmupIterations="+warmUpIterations+ "; #iterations="+nOfMutations);
		makeIniSolution();
		verify();
		init();
		logger.info("#jobs: " + vrp.getJobs().values().size());
		randomWalk(warmUpIterations);
		logger.info("run mutations");
		resetIterations();
		while(currentMutation < nOfMutations){
			RRSolution tentativeSolution = VrpUtils.copySolution(currentSolution, vrp, tourAgentFactory);
			double result2beat = currentSolution.getResult() + thresholdFunction.getThreshold(currentMutation);
			ruinAndRecreate(tentativeSolution, result2beat);
			double tentativeResult = tentativeSolution.getResult();
			double currentResult = currentSolution.getResult();
			boolean isAccepted = false;
			if(tentativeResult < currentResult + thresholdFunction.getThreshold(currentMutation)){
				currentSolution = tentativeSolution;
				isAccepted = true;
			}
			informListener(currentMutation,tentativeResult,currentResult, currentThreshold,isAccepted);
			printNoIteration(currentMutation);
			currentMutation++;
		}
		informFinish();
	}
	
	private void makeIniSolution() {
		if(currentSolution == null){
			currentSolution = RRUtils.createSolution(vrp, iniTourPlan, tourAgentFactory);
		}
		
	}

	private void verify() {
		if(currentSolution.getTourAgents().isEmpty() || currentSolution.getTourAgents().size() < 1){
			throw new IllegalStateException("initial solution is empty. this cannot be. check vehicle-routing-problem setup (VRP)");
		}
		
	}

	private void init() {
		thresholdFunction.setNofIterations(nOfMutations);
		thresholdFunction.setInitialThreshold(initialThreshold);
		
		
	}

	private void ruinAndRecreate(RRSolution solution, double upperBound) {
		RuinStrategy ruinStrategy = ruinStrategyManager.getRandomStrategy();
		ruinStrategy.run(solution);
		recreationStrategy.run(solution,ruinStrategy.getUnassignedJobs(), upperBound);
	}

	private void randomWalk(int nOfIterations) {
		if(nOfIterations == 0){
			return;
		}
		logger.info("random walk for threshold determination");
		RRSolution lastSolution = VrpUtils.copySolution(currentSolution, vrp, tourAgentFactory);
		resetIterations();
		double[] results = new double[nOfIterations];
		for(int i=0;i<nOfIterations;i++){
			printNoIteration(i);
			RRSolution currentSolution = VrpUtils.copySolution(lastSolution, vrp, tourAgentFactory);
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
		RuinAndRecreateEvent event = new RuinAndRecreateEvent(currentMutation, tentativeResult,currentResult, currentThreshold, isAccepted);
		event.setCurrentSolution(getSolution());
		for(RuinAndRecreateListener l : listeners){
			l.inform(event);
		}
	}

	public Collection<RuinAndRecreateListener> getListeners() {
		return listeners;
	}
	
	public RRSolution getSolution(){
		return currentSolution;
	}
	
	public TourPlan getTourPlan(){
		return RRUtils.createTourPlan(currentSolution);
	}
}
