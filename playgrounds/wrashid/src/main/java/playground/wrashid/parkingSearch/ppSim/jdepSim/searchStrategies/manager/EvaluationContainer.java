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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.core.gbl.MatsimRandom;

import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.ParkingSearchStrategy;
import playground.wrashid.parkingSearch.ppSim.jdepSim.zurich.ZHScenarioGlobal;
import playground.wrashid.parkingSearch.withinDay_v_STRC.scoring.ParkingScoreManager;
import playground.wrashid.parkingSearch.withinDay_v_STRC.strategies.FullParkingSearchStrategy;

public class EvaluationContainer {

	private static final Logger log = Logger.getLogger(EvaluationContainer.class);
	
	LinkedList<StrategyEvaluation> evaluations;

	private Random random;
	
	public EvaluationContainer(LinkedList<ParkingSearchStrategy> allStrategies){
		random = MatsimRandom.getLocalInstance();
		evaluations=new LinkedList<StrategyEvaluation>();
		createAndStorePermutation(allStrategies);
	}
	
	public void createAndStorePermutation(LinkedList<ParkingSearchStrategy> allStrategies){
		ArrayList<ParkingSearchStrategy> strategies=new ArrayList<ParkingSearchStrategy>(allStrategies);
	
		while (strategies.size()!=0){
			int randomIndex = random.nextInt(strategies.size());
			evaluations.add(new StrategyEvaluation(strategies.remove(randomIndex)));
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
		
		if (best.score<0){
			DebugLib.emptyFunctionForSettingBreakPoint();
		}
	}
	
	public void selectNextStrategyAccordingToMNL(){
		double exponentialSum=0;
		double[] selectionProbabilities=new double[evaluations.size()];
		for (int i=0;i<evaluations.size();i++){
			exponentialSum+=Math.exp(evaluations.get(i).score);
		}
		
		for (int i=0;i<evaluations.size();i++){
			selectionProbabilities[i]=Math.exp( evaluations.get(i).score)/exponentialSum;
		}
		
		double r=random.nextDouble();
		int index=0;
		double sum=0;
		
		while (sum+selectionProbabilities[index]<r){
			sum+=selectionProbabilities[index];
			index++;
		}
		
		evaluations.addFirst(evaluations.remove(index));
	}
	
	public void selectStrategyAccordingToFixedProbabilityForBestStrategy(){
		if (random.nextDouble() < ZHScenarioGlobal.loadDoubleParam("convergance.fixedPropbabilityBestStrategy.probabilityBestStrategy")) {
			selectBestStrategyForExecution();
		} else {
			selectLongestNonExecutedStrategyForExecution();
		}
	}
	
	public void trimStrategySet(int maxNumberOfStrategies){
		while (evaluations.size()>maxNumberOfStrategies){
			dropWostStrategy();
		}
	}
	
	public void dropWostStrategy(){
		StrategyEvaluation worstStrategy=evaluations.getFirst();
		for (StrategyEvaluation se:evaluations){
			if (worstStrategy.score>se.score){
				worstStrategy=se;
			}
		}
		evaluations.remove(worstStrategy);
	}
	
	public int getNumberOfStrategies(){
		return evaluations.size();
	}
	
	
	public boolean allStrategiesHaveBeenExecutedOnce(){
		for (StrategyEvaluation se:evaluations){
			if (Double.isInfinite(se.score)){
				return false;
			}
		}
		return true;
	}
	
	// precondition: such a strategy exists (allStrategiesHaveBeenExecutedOnce=> true)
	public void selectStrategyNotExecutedTillNow(){
		StrategyEvaluation strategyToExecuteNext=null;
		for (StrategyEvaluation se:evaluations){
			if (Double.isInfinite(se.score)){
				strategyToExecuteNext=se;
				break;
			}
		}
		evaluations.remove(strategyToExecuteNext);
		evaluations.addFirst(strategyToExecuteNext);
	}
	
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

