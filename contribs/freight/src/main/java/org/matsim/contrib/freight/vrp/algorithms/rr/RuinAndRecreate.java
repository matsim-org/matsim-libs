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
package org.matsim.contrib.freight.vrp.algorithms.rr;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.math.stat.descriptive.moment.StandardDeviation;
import org.apache.log4j.Logger;
import org.matsim.contrib.freight.vrp.algorithms.rr.recreation.RecreationStrategy;
import org.matsim.contrib.freight.vrp.algorithms.rr.ruin.RuinStrategy;
import org.matsim.contrib.freight.vrp.algorithms.rr.thresholdFunctions.ThresholdFunction;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.TourAgent;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.TourAgentFactory;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblem;
import org.matsim.contrib.freight.vrp.basics.Tour;
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
	
	private TourAgentFactory tourAgentFactory;

	private Collection<RuinAndRecreateListener> listeners = new ArrayList<RuinAndRecreateListener>();
	
	private int lastPrint = 1;
	
	public RuinAndRecreate(VehicleRoutingProblem vrp, RRSolution iniSolution, int nOfMutations) {
		this.vrp = vrp;
		this.currentSolution = iniSolution;
		this.nOfMutations = nOfMutations;
	}

	public void setRecreationStrategy(RecreationStrategy recreationStrategy) {
		this.recreationStrategy = recreationStrategy;
	}

	public void setTourAgentFactory(TourAgentFactory tourAgentFactory) {
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
		verify();
		init();
		logger.info("#jobs: " + vrp.getJobs().values().size());
		randomWalk(warmUpIterations);
		logger.info("run mutations");
		resetIterations();
		while(currentMutation < nOfMutations){
			RRSolution tentativeSolution = VrpUtils.copySolution(currentSolution, vrp, tourAgentFactory);
			ruinAndRecreate(tentativeSolution);
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
	
	private void verify() {
		if(currentSolution.getTourAgents().isEmpty() || currentSolution.getTourAgents().size() < 1){
			throw new IllegalStateException("initial solution is empty. this cannot be. check vehicle-routing-problem setup (VRP)");
		}
		
	}

	private void init() {
		thresholdFunction.setNofIterations(nOfMutations);
		thresholdFunction.setInitialThreshold(initialThreshold);
	}

	private void ruinAndRecreate(RRSolution solution) {
		RuinStrategy ruinStrategy = ruinStrategyManager.getRandomStrategy();
		logger.debug("stratClass=" + ruinStrategy.getClass());
		ruinStrategy.run(solution);
		recreationStrategy.run(solution,ruinStrategy.getUnassignedJobs());
	}

	private void randomWalk(int nOfIterations) {
		if(nOfIterations == 0){
			return;
		}
		logger.info("random walk for threshold determination");
		RRSolution initialSolution = VrpUtils.copySolution(currentSolution, vrp, tourAgentFactory);
		resetIterations();
		double[] results = new double[nOfIterations];
		for(int i=0;i<nOfIterations;i++){
			printNoIteration(i);
			ruinAndRecreate(initialSolution);
			double result = initialSolution.getResult();
			results[i]=result;
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
	
	public Collection<Tour> getSolution(){
		Collection<Tour> tours = new ArrayList<Tour>();
		for(TourAgent tA : currentSolution.getTourAgents()){
			tours.add(tA.getTour());
		}
		return tours;
	}
}
