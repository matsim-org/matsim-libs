/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.wrashid.parkingSearch.withinDay_v_STRC.strategies.manager;

import java.util.LinkedList;

import playground.wrashid.parkingSearch.withinDay_v_STRC.strategies.FullParkingSearchStrategy;

public class EvaluationContainer {

	LinkedList<StrategyEvaluation> evaluations;
	
	// this is super important, because if a parking container is not used in an iteration
	// it should be flushed/ reset.
	private int lastIterationContainerUsed=-1;

	public int getLastIterationContainerUsed() {
		return lastIterationContainerUsed;
	}

	public void setLastIterationContainerUsed(int lastIterationContainerUsed) {
		this.lastIterationContainerUsed = lastIterationContainerUsed;
	}
	
	public EvaluationContainer(LinkedList<FullParkingSearchStrategy> allStrategies){
		evaluations=new LinkedList<StrategyEvaluation>();
		
		for (FullParkingSearchStrategy fullStrat:allStrategies){
			evaluations.add(new StrategyEvaluation(fullStrat));
		}
	}
	
	public StrategyEvaluation getCurrentSelectedStrategy(){
		return evaluations.getFirst();
	}
	
	public void selectBestStrategyForExecution(){
		StrategyEvaluation best=null;
		for (StrategyEvaluation se:evaluations){
			if (best==null || se.score>best.score){
				best=se;
			}
		}
		evaluations.remove(best);
		evaluations.addFirst(best);
	}
	
	public void selectLongestNonExecutedStrategyForExecution(){
		StrategyEvaluation last=evaluations.getLast();
		evaluations.remove(last);
		evaluations.addFirst(last);
	}

}

