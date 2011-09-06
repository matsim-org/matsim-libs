/*******************************************************************************
 * Copyright (C) 2011 Stefan Schršder.
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
package vrp.algorithms.ruinAndRecreate;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.math.stat.descriptive.moment.StandardDeviation;
import org.apache.log4j.Logger;

import vrp.algorithms.ruinAndRecreate.api.RecreationStrategy;
import vrp.algorithms.ruinAndRecreate.api.RuinAndRecreateListener;
import vrp.algorithms.ruinAndRecreate.api.RuinStrategy;
import vrp.algorithms.ruinAndRecreate.api.ServiceProvider;
import vrp.algorithms.ruinAndRecreate.api.ThresholdFunction;
import vrp.algorithms.ruinAndRecreate.api.TourAgent;
import vrp.algorithms.ruinAndRecreate.api.TourAgentFactory;
import vrp.algorithms.ruinAndRecreate.basics.Solution;
import vrp.api.VRP;
import vrp.basics.Tour;
import vrp.basics.VrpSolution;
import vrp.basics.VrpUtils;


/**
 * This algorithm is basically the implementation of 
 * Schrimpf G., J. Schneider, Hermann Stamm-Wilbrandt and Gunter Dueck (2000): Record Breaking Optimization Results Using 
 * the Ruin and Recreate Principle, Journal of Computational Physics 159, 139-171 (2000).
 * 
 * @author stefan schroeder
 *
 */

public class RuinAndRecreate {
	
	public static class Offer {
		
		private ServiceProvider agent;
		
		private double cost;

		public Offer(ServiceProvider agent, double cost) {
			super();
			this.agent = agent;
			this.cost = cost;
		}

		public ServiceProvider getServiceProvider() {
			return agent;
		}

		public double getPrice() {
			return cost;
		}
		
		@Override
		public String toString() {
			return "currentTour=" + agent + "; marginalInsertionCosts=" + cost;
		}
		
	}

	private static Logger logger = Logger.getLogger(RuinAndRecreate.class);
	
	private RuinStrategyManager ruinStrategyManager; 

	private RecreationStrategy recreationStrategy;
	
	private VRP vrp;
	
	private int nOfMutations = 100;
	
	private int warmUpIterations = 10;
	
	private int currentMutation = 0;
	
	private Solution currentSolution;
	
	private ThresholdFunction thresholdFunction;
	
	private double currentThreshold;
	
	private Double initialThreshold = 0.0;
	
	private TourAgentFactory tourAgentFactory;

	private Collection<RuinAndRecreateListener> listeners = new ArrayList<RuinAndRecreateListener>();
	
	private VrpSolution vrpSolution;
	
	private int lastPrint = 1;
	
	public RuinAndRecreate(VRP vrp, Solution iniSolution, int nOfMutations) {
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
		logger.info("#customer: " + vrp.getCustomers().values().size());
		randomWalk(warmUpIterations);
		logger.info("run mutations");
		resetIterations();
		while(currentMutation < nOfMutations){
			Solution tentativeSolution = VrpUtils.copySolution(currentSolution, vrp, tourAgentFactory);
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
		vrpSolution = new VrpSolution(getSolution());
		vrpSolution.setTransportCosts(currentSolution.getResult());
		informFinish();
	}
	
	private void verify() {
		if(currentSolution.getTourAgents().isEmpty() || currentSolution.getTourAgents().size() < 1){
			return;
		}
		
	}

	public VrpSolution getVrpSolution() {
		return vrpSolution;
	}

	private void init() {
		thresholdFunction.setNofIterations(nOfMutations);
		thresholdFunction.setInitialThreshold(initialThreshold);
	}

	private void ruinAndRecreate(Solution solution) {
		RuinStrategy ruinStrategy = ruinStrategyManager.getRandomStrategy();
		logger.debug("stratClass=" + ruinStrategy.getClass());
		ruinStrategy.run(solution);
		recreationStrategy.run(solution,ruinStrategy.getShipmentsWithoutService());
	}

	private void randomWalk(int nOfIterations) {
		if(nOfIterations == 0){
			return;
		}
		logger.info("random walk for threshold determination");
		Solution initialSolution = VrpUtils.copySolution(currentSolution, vrp, tourAgentFactory);
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
		for(RuinAndRecreateListener l : listeners){
			l.inform(event);
		}
	}

	public Collection<RuinAndRecreateListener> getListeners() {
		return listeners;
	}

	public Solution getTourAgentSolution(){
		return currentSolution;
	}
	
	public Collection<Tour> getSolution(){
		Collection<Tour> tours = new ArrayList<Tour>();
		for(TourAgent tA : currentSolution.getTourAgents()){
			tours.add(tA.getTour());
		}
		return tours;
	}
}
