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
package playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.manager;

import java.util.LinkedList;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.core.gbl.MatsimRandom;

import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.ParkingSearchStrategy;
import playground.wrashid.parkingSearch.withinDay_v_STRC.scoring.ParkingScoreManager;
import playground.wrashid.parkingSearch.withinDay_v_STRC.strategies.FullParkingSearchStrategy;

public class EvaluationContainer {

	private static final Logger log = Logger.getLogger(EvaluationContainer.class);
	
	LinkedList<StrategyEvaluation> evaluations;
	
	public EvaluationContainer(LinkedList<ParkingSearchStrategy> allStrategies){
		evaluations=new LinkedList<StrategyEvaluation>();
		
		for (ParkingSearchStrategy fullStrat:allStrategies){
			evaluations.add(new StrategyEvaluation(fullStrat));
		}
		
		Random rand = MatsimRandom.getLocalInstance();
		StrategyEvaluation randomSelectedEvaluationStrategy = evaluations.remove(rand.nextInt(evaluations.size()));
		evaluations.addFirst(randomSelectedEvaluationStrategy);
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
		
		if (best.score<0){
			DebugLib.emptyFunctionForSettingBreakPoint();
		}
	}
	
	//TODO: add random selection from non selected
	
	//TODO: add logit model
	
	// for this to work, will need to set initial score to finit negative number initially.
	
	
	public void selectLongestNonExecutedStrategyForExecution(){
		StrategyEvaluation last=evaluations.getLast();
		evaluations.remove(last);
		evaluations.addFirst(last);
	}
	
	public void updateScoreOfSelectedStrategy(double score){
		if (score<-100000000){
			DebugLib.emptyFunctionForSettingBreakPoint();
		}
		
		evaluations.get(0).score=score;
	}
	
	public void printAllScores(){
		for (StrategyEvaluation strategyEvaluation:evaluations){
			log.info(strategyEvaluation.strategy.getName() + "-> score: " + strategyEvaluation.score);
		}
	}
	
	public double getSelectedStrategyScore(){
		return getCurrentSelectedStrategy().score;
	}
	
	public double getBestStrategyScore(){
		double bestScore=Double.NEGATIVE_INFINITY;
		for (StrategyEvaluation se:evaluations){
			if (se.score>Double.NEGATIVE_INFINITY && bestScore<se.score){
				bestScore=se.score;
			}
		}
		return bestScore;
	}
	
	public double getWorstStrategyScore(){
		double worstScore=Double.POSITIVE_INFINITY;
		for (StrategyEvaluation se:evaluations){
			if (se.score>Double.NEGATIVE_INFINITY && worstScore>se.score){
				worstScore=se.score;
			}
		}
		return worstScore;
	}
	
	public double getAverageStrategyScore(){
		double average=0;
		int sampleSize=0;
		for (StrategyEvaluation se:evaluations){
			if (se.score>Double.NEGATIVE_INFINITY){
				average+=se.score;
				sampleSize++;
			}
		}
		return average/sampleSize;
	}

}

