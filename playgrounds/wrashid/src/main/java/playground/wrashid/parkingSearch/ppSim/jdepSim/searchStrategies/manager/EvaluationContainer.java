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
	
	private LinkedList<StrategyEvaluation> evaluations;

	private Random random;
	
	public EvaluationContainer(LinkedList<ParkingSearchStrategy> allStrategies){
		random = MatsimRandom.getLocalInstance();
		setEvaluations(new LinkedList<StrategyEvaluation>());
		createAndStorePermutation(allStrategies);
	}
	
	public void createAndStorePermutation(LinkedList<ParkingSearchStrategy> allStrategies){
		ArrayList<ParkingSearchStrategy> strategies=new ArrayList<ParkingSearchStrategy>(allStrategies);
	
		while (strategies.size()!=0){
			int randomIndex = random.nextInt(strategies.size());
			getEvaluations().add(new StrategyEvaluation(strategies.remove(randomIndex)));
		}
	}
	
	public StrategyEvaluation getCurrentSelectedStrategy(){
		return getEvaluations().getFirst();
	}
	
	public void selectBestStrategyForExecution(){
		StrategyEvaluation best=null;
		for (StrategyEvaluation se:getEvaluations()){
			if (best==null || se.score>best.score){
				best=se;
			}
		}
		getEvaluations().remove(best);
		getEvaluations().addFirst(best);
		
		if (best.score<0){
			DebugLib.emptyFunctionForSettingBreakPoint();
		}
	}
	
	
	public void selectNextStrategyAccordingToMNLExp(){
		double exponentialSum=0;
		double maxScore=getBestStrategyScore();
		double[] selectionProbabilities=new double[getEvaluations().size()];
		for (int i=0;i<getEvaluations().size();i++){
			exponentialSum+=Math.exp(getEvaluations().get(i).score - maxScore);
		}
		
		for (int i=0;i<getEvaluations().size();i++){
			selectionProbabilities[i]=Math.exp( getEvaluations().get(i).score - maxScore)/exponentialSum;
		}
		
		double r=random.nextDouble();
		int index=0;
		double sum=0;
		
		while (sum+selectionProbabilities[index]<r){
			sum+=selectionProbabilities[index];
			index++;
		}
		
		getEvaluations().addFirst(getEvaluations().remove(index));
	}
	
	public void selectNextStrategyAccordingToProbability(){
		double exponentialSum=0;
		double[] selectionProbabilities=new double[getEvaluations().size()];
		for (int i=0;i<getEvaluations().size();i++){
			exponentialSum+=Math.abs(getEvaluations().get(i).score);
		}
		
		for (int i=0;i<getEvaluations().size();i++){
			selectionProbabilities[i]=Math.abs( getEvaluations().get(i).score)/exponentialSum;
		}
		
		double r=random.nextDouble();
		int index=0;
		double sum=0;
		
		while (sum+selectionProbabilities[index]<r){
			sum+=selectionProbabilities[index];
			index++;
		}
		
		getEvaluations().addFirst(getEvaluations().remove(index));
	}
	
	public void selectStrategyAccordingToFixedProbabilityForBestStrategy(){
		if (random.nextDouble() < ZHScenarioGlobal.loadDoubleParam("convergance.fixedPropbabilityBestStrategy.probabilityBestStrategy")) {
			selectBestStrategyForExecution();
		} else {
			selectLongestNonExecutedStrategyForExecution();
		}
	}
	
	public void trimStrategySet(int maxNumberOfStrategies){
		while (getEvaluations().size()>maxNumberOfStrategies){
			dropWostStrategy();
		}
	}
	
	public void dropWostStrategy(){
		StrategyEvaluation worstStrategy=getEvaluations().getFirst();
		for (StrategyEvaluation se:getEvaluations()){
			if (worstStrategy.score>se.score){
				worstStrategy=se;
			}
		}
		getEvaluations().remove(worstStrategy);
	}
	
	public int getNumberOfStrategies(){
		return getEvaluations().size();
	}
	
	
	public boolean allStrategiesHaveBeenExecutedOnce(){
		for (StrategyEvaluation se:getEvaluations()){
			if (Double.isInfinite(se.score)){
				return false;
			}
		}
		return true;
	}
	
	// precondition: such a strategy exists (allStrategiesHaveBeenExecutedOnce=> true)
	public void selectStrategyNotExecutedTillNow(){
		StrategyEvaluation strategyToExecuteNext=null;
		for (StrategyEvaluation se:getEvaluations()){
			if (Double.isInfinite(se.score)){
				strategyToExecuteNext=se;
				break;
			}
		}
		getEvaluations().remove(strategyToExecuteNext);
		getEvaluations().addFirst(strategyToExecuteNext);
	}
	
	public void selectLongestNonExecutedStrategyForExecution(){
		StrategyEvaluation last=getEvaluations().getLast();
		getEvaluations().remove(last);
		getEvaluations().addFirst(last);
	}
	
	public void updateScoreOfSelectedStrategy(double score){
		if (score<-100000000){
			DebugLib.emptyFunctionForSettingBreakPoint();
		}
		
		getEvaluations().get(0).score=score;
	}
	
	public void printAllScores(){
		for (StrategyEvaluation strategyEvaluation:getEvaluations()){
			log.info(strategyEvaluation.strategy.getName() + "-> score: " + strategyEvaluation.score);
		}
	}
	
	public double getSelectedStrategyScore(){
		return getCurrentSelectedStrategy().score;
	}
	
	public double getBestStrategyScore(){
		double bestScore=Double.NEGATIVE_INFINITY;
		for (StrategyEvaluation se:getEvaluations()){
			if (se.score>Double.NEGATIVE_INFINITY && bestScore<se.score){
				bestScore=se.score;
			}
		}
		return bestScore;
	}
	
	public double getWorstStrategyScore(){
		double worstScore=Double.POSITIVE_INFINITY;
		for (StrategyEvaluation se:getEvaluations()){
			if (se.score>Double.NEGATIVE_INFINITY && worstScore>se.score){
				worstScore=se.score;
			}
		}
		return worstScore;
	}
	
	public double getAverageStrategyScore(){
		double average=0;
		int sampleSize=0;
		for (StrategyEvaluation se:getEvaluations()){
			if (se.score>Double.NEGATIVE_INFINITY){
				average+=se.score;
				sampleSize++;
			}
		}
		return average/sampleSize;
	}

	public LinkedList<StrategyEvaluation> getEvaluations() {
		return evaluations;
	}

	public void setEvaluations(LinkedList<StrategyEvaluation> evaluations) {
		this.evaluations = evaluations;
	}

}

