/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.wrashid.parkingSearch.ppSim.jdepSim.analysis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.Matrix;

import playground.wrashid.lib.obj.TwoHashMapsConcatenated;


public class StrategyScoresAnalysis {

	/**
	 * @param args
	 */
	
	//static String outputFolder="C:/data/parkingSearch/psim/zurich/output/run19/output/";
	static String outputFolder="H:/data/experiments/parkingSearchOct2013/runs/run104/output/";
	public static void main(String[] args) {
		
		
		int startIteration=0;
		int endIteration=399;
		int iterationStep=1;
		
		int referenceIteration=endIteration;
		
		TwoHashMapsConcatenated<String, Integer, LinkedList<StrategyScoreLog>> refernceScores = getScores(referenceIteration,false);
		
		System.out.println("==========");
		for (int i=startIteration;i<endIteration;i+=iterationStep){
			TwoHashMapsConcatenated<String, Integer, LinkedList<StrategyScoreLog>> curScores = getScores(i,false);
			
			System.out.println(i + "\t" + getPercentageOfRelaxedStrategiesUnique(curScores,refernceScores) + "\t" + getPercentageOfRelaxedStrategiesAll(curScores,refernceScores) + "\t" + getPercentageOfMultipleBestStrategies(curScores));
			
		}
		System.out.println("==========");
		
		/*
		TwoHashMapsConcatenated<String, Integer, LinkedList<StrategyScoreLog>> curScoresWithPrivateParking = getScores(startIteration,true);
		TwoHashMapsConcatenated<String, Integer, LinkedList<StrategyScoreLog>> nexterIterScoresWithPrivateParking = getScores(startIteration+1,true);
		
		for (int i=startIteration+1;i<endIteration-1;i+=iterationStep){
			System.out.println(i + "\t" + getPercentageOfBestScoreChangePerIteration(curScoresWithPrivateParking,nexterIterScoresWithPrivateParking));
			curScoresWithPrivateParking = nexterIterScoresWithPrivateParking;
			nexterIterScoresWithPrivateParking = getScores(i+1,true);
		}
		*/
		
		
		// % is best score strategy same as in final?
		
		// % of agents who have multiple strategies with same score (best score)
		
		
	}



	
	private static double getPercentageOfBestScoreChangePerIteration(
			TwoHashMapsConcatenated<String, Integer, LinkedList<StrategyScoreLog>> curScoresWithPrivateParking,
			TwoHashMapsConcatenated<String, Integer, LinkedList<StrategyScoreLog>> nexterIterScoresWithPrivateParking) {
		double numberOfParkingOperations=0;
		double numberOfParkingOperationsWhereStrategyRelaxed=0;
		for (String personId:nexterIterScoresWithPrivateParking.getKeySet1()){
			for (Integer legIndex:nexterIterScoresWithPrivateParking.getKeySet2(personId)){
				
				if (curScoresWithPrivateParking.get(personId, legIndex)==null){
					continue;
				}
				
				double bestCurScore = getBestScore(curScoresWithPrivateParking.get(personId, legIndex));
				double bestNextScore = getBestScore(nexterIterScoresWithPrivateParking.get(personId, legIndex));
				
				numberOfParkingOperations++;
				
				if (bestCurScore!= bestNextScore){
					numberOfParkingOperationsWhereStrategyRelaxed++;
				}
			}
		}
		
		return numberOfParkingOperationsWhereStrategyRelaxed/numberOfParkingOperations;
	}




	private static double getPercentageOfRelaxedStrategiesUnique(
			TwoHashMapsConcatenated<String, Integer, LinkedList<StrategyScoreLog>> currentIteration,
			TwoHashMapsConcatenated<String, Integer, LinkedList<StrategyScoreLog>> referenceIteration) {
		
		double numberOfParkingOperations=0;
		double numberOfParkingOperationsWhereStrategyRelaxed=0;
		for (String personId:referenceIteration.getKeySet1()){
			for (Integer legIndex:referenceIteration.getKeySet2(personId)){
				
				LinkedList<String> highestScoreStrategiesRef = getHighestScoreStrategies(referenceIteration.get(personId, legIndex));
				LinkedList<String> highestScoreStrategiesCur = getHighestScoreStrategies(currentIteration.get(personId, legIndex));
				
				if ((highestScoreStrategiesRef==null) || (highestScoreStrategiesCur==null)){
					if (highestScoreStrategiesRef!=highestScoreStrategiesCur){
						// one is using private parking => count as difference
						numberOfParkingOperations++;
					}
					
					continue;
				}
				numberOfParkingOperations++;
				
				if (highestScoreStrategiesRef.size()==1 && highestScoreStrategiesCur.size()==1 && highestScoreStrategiesRef.get(0).equalsIgnoreCase(highestScoreStrategiesCur.get(0))){
					numberOfParkingOperationsWhereStrategyRelaxed++;
				}
			}
		}
		
		return numberOfParkingOperationsWhereStrategyRelaxed/numberOfParkingOperations;
	}


	public static String getScoresFileName(int iterationNumber) {
		String fileName = outputFolder + "/ITERS/" + iterationNumber +".strategyScores.txt.gz";
		return fileName;
	}
	
	public static String getEventsFileName(int iterationNumber) {
		String fileName = outputFolder + "/ITERS/" + iterationNumber +".parkingEvents.txt.gz";
		return fileName;
	}

	// percentage of total parking operations, where there is not a single best strategy
	// but multiple ones
	public static double getPercentageOfMultipleBestStrategies(TwoHashMapsConcatenated<String, Integer, LinkedList<StrategyScoreLog>> scores){
		double numberOfParkingOperations=0;
		double numberOfParkingOperationsWithMultipleBestScores=0;
		
		for (String personId:scores.getKeySet1()){
			for (Integer legIndex:scores.getKeySet2(personId)){
				numberOfParkingOperations++;
				LinkedList<StrategyScoreLog> scoreList = scores.get(personId, legIndex);
				
				if (hasMultipleBestScoreStrategies(scoreList)){
					numberOfParkingOperationsWithMultipleBestScores++;
				}
			}
		}
		
		return numberOfParkingOperationsWithMultipleBestScores/numberOfParkingOperations;
	}
	

	private static double getBestScore(LinkedList<StrategyScoreLog> scoreList){
		double bestScore=Double.NEGATIVE_INFINITY;
		
		for (StrategyScoreLog scoreLog:scoreList){
			if (!Double.isInfinite(scoreLog.score)){
				if(scoreLog.score>bestScore){
					bestScore=scoreLog.score;
				}
			}
		}
		
		return bestScore;
	}
	
	// number of best strategies in current iteration, which are also best strategies in reference iteration
	public static double getPercentageOfRelaxedStrategiesAll(TwoHashMapsConcatenated<String, Integer, LinkedList<StrategyScoreLog>> currentIteration, TwoHashMapsConcatenated<String, Integer, LinkedList<StrategyScoreLog>> referenceIteration){
		double numberOfParkingOperations=0;
		double numberOfParkingOperationsWhereStrategyRelaxed=0;
		for (String personId:referenceIteration.getKeySet1()){
			for (Integer legIndex:referenceIteration.getKeySet2(personId)){
				
				LinkedList<String> highestScoreStrategiesRef = getHighestScoreStrategies(referenceIteration.get(personId, legIndex));
				LinkedList<String> highestScoreStrategiesCur = getHighestScoreStrategies(currentIteration.get(personId, legIndex));
				
				if ((highestScoreStrategiesRef==null) || (highestScoreStrategiesCur==null)){
					continue;
				}
				numberOfParkingOperations++;
				
				HashSet<String> hs=new HashSet();
				
				hs.addAll(highestScoreStrategiesRef);
				hs.addAll(highestScoreStrategiesCur);
				
				if (hs.size()<highestScoreStrategiesRef.size()+highestScoreStrategiesCur.size()){
					numberOfParkingOperationsWhereStrategyRelaxed++;
				}
			}
		}
		
		return numberOfParkingOperationsWhereStrategyRelaxed/numberOfParkingOperations;
	}
	
	
	
	
	private static LinkedList<String> getHighestScoreStrategies(LinkedList<StrategyScoreLog> scoreList) {
		if (scoreList==null){
			return null;
		}
		
		double bestScore=getBestScore(scoreList);
		LinkedList<String> result=new LinkedList<String>();
		
		for (StrategyScoreLog scoreLog:scoreList){
			if (!Double.isInfinite(scoreLog.score)){
				if(scoreLog.score==bestScore){
					result.add(scoreLog.strategyName);
				}
			}
		}
		
		return result;
	}
	
	private static boolean hasMultipleBestScoreStrategies(LinkedList<StrategyScoreLog> scoreList) {
		double bestScore=getBestScore(scoreList);
		
		double bestScoreFound=0;
		for (StrategyScoreLog scoreLog:scoreList){
			if (!Double.isInfinite(scoreLog.score)){
				if(scoreLog.score==bestScore){
					bestScoreFound++;
					if (bestScoreFound>=2){
						return true;
					}
				}
			}
		}
		
		return false;
	}

	public static TwoHashMapsConcatenated<String, Integer, Boolean> usesPrivateParking(Matrix matrix){
		TwoHashMapsConcatenated<String, Integer, Boolean> usesPrivateParking=new TwoHashMapsConcatenated<String, Integer, Boolean>();
		
		for (int i=1;i<matrix.getNumberOfRows();i++){
			String currentPersonId=matrix.getString(i, 0);
			String parkingFacilityId=matrix.getString(i, 6);
			int curLegIndexId=matrix.getInteger(i, 8);
			
			if (parkingFacilityId.contains("privateParking") || parkingFacilityId.contains("publicPOutside")){
				usesPrivateParking.put(currentPersonId, curLegIndexId, Boolean.TRUE);
			}
		}
		
		return usesPrivateParking;
	}
	
	
	public static TwoHashMapsConcatenated<String, Integer, Boolean> usesPrivateParking(int iterationNumber){
		Matrix matrix = GeneralLib.readStringMatrix(getEventsFileName(iterationNumber));
		return usesPrivateParking(matrix);
	}
	
	public static TwoHashMapsConcatenated<String, Integer, ArrayList<String>> getParkingEvents(String outputPath,int iterationNumber){
		Matrix matrix = GeneralLib.readStringMatrix(outputPath + "/ITERS/" + iterationNumber +".parkingEvents.txt.gz");
		TwoHashMapsConcatenated<String, Integer, ArrayList<String>> result=new TwoHashMapsConcatenated<String, Integer, ArrayList<String>>();
		
		int personIdColIndex=matrix.getColumnIndex("personId");
		int legIndexColIndex=matrix.getColumnIndex("legIndex");
		
		for (int i=1;i< matrix.getNumberOfRows();i++){
			result.put(matrix.getString(i, personIdColIndex), matrix.getInteger(i, legIndexColIndex), matrix.getRow(i));
		}
		
		return result;
	}

	
	
	
	public static  TwoHashMapsConcatenated<String, Integer, LinkedList<StrategyScoreLog>> getAllScores(String outputPath,int iterationNumber){
		TwoHashMapsConcatenated<String, Integer, LinkedList<StrategyScoreLog>> scores=new TwoHashMapsConcatenated<String, Integer, LinkedList<StrategyScoreLog>>();
		
		Matrix scoresMatrix = GeneralLib.readStringMatrix(outputPath + "/ITERS/" + iterationNumber +".strategyScores.txt.gz");
		LinkedList<StrategyScoreLog> tmpList=null;
		for (int i=1;i< scoresMatrix.getNumberOfRows();i++){
			String currentPersonId= scoresMatrix.getString(i, 0);
			int curLegIndexId= scoresMatrix.getInteger(i, 1);
			String strategyName= scoresMatrix.getString(i, 2);
			double score= scoresMatrix.getDouble(i, 3);
			
			if (!scores.containsValue(currentPersonId, curLegIndexId)){
				scores.put(currentPersonId, curLegIndexId, new LinkedList<StrategyScoresAnalysis.StrategyScoreLog>());
			}
			tmpList=scores.get(currentPersonId, curLegIndexId);
			
			tmpList.add(new StrategyScoreLog(strategyName, score));
		}
		return scores;
	}
	
	
	
	public static TwoHashMapsConcatenated<String, Integer, LinkedList<StrategyScoreLog>> getScores(Matrix scoresMatrix,Matrix eventsMatrix,boolean removePrivateParking) {
TwoHashMapsConcatenated<String, Integer, LinkedList<StrategyScoreLog>> scores=new TwoHashMapsConcatenated<String, Integer, LinkedList<StrategyScoreLog>>();
		
		TwoHashMapsConcatenated<String, Integer, Boolean> usesPrivateParking = usesPrivateParking(eventsMatrix);
		
		String prevPersonId= scoresMatrix.getString(1, 0);
		int prevLegIndexId= scoresMatrix.getInteger(1, 1);
		LinkedList<StrategyScoreLog> tmpList=new LinkedList<StrategyScoresAnalysis.StrategyScoreLog>();
		for (int i=1;i< scoresMatrix.getNumberOfRows();i++){
			
			String currentPersonId= scoresMatrix.getString(i, 0);
			int curLegIndexId= scoresMatrix.getInteger(i, 1);
			String strategyName= scoresMatrix.getString(i, 2);
			double score= scoresMatrix.getDouble(i, 3);
			
			if (!prevPersonId.equalsIgnoreCase(currentPersonId) || prevLegIndexId!=curLegIndexId){
				if (!removePrivateParking || usesPrivateParking.get(currentPersonId, curLegIndexId)==null){
					scores.put(prevPersonId, prevLegIndexId, tmpList);
				}
				
				tmpList=new LinkedList<StrategyScoresAnalysis.StrategyScoreLog>();
				
				if (!prevPersonId.equalsIgnoreCase(currentPersonId)){
					prevPersonId=currentPersonId;
				}
				
				if (prevLegIndexId!=curLegIndexId){
					prevLegIndexId=curLegIndexId;
				}
			}
			tmpList.add(new StrategyScoreLog(strategyName, score));
		}
		return scores;
	}
	
	public static TwoHashMapsConcatenated<String, Integer, LinkedList<StrategyScoreLog>> getScores(String outputPath,int iterationNumber, boolean removePrivateParking) {
		Matrix scoresMatrix = GeneralLib.readStringMatrix(outputPath + "/ITERS/" + iterationNumber +".strategyScores.txt.gz");
		Matrix eventsMatrix = GeneralLib.readStringMatrix(outputPath + "/ITERS/" + iterationNumber +".parkingEvents.txt.gz");		
		return getScores(scoresMatrix,eventsMatrix,removePrivateParking);
	}
	
	public static TwoHashMapsConcatenated<String, Integer, LinkedList<StrategyScoreLog>> getScores(int iterationNumber, boolean removePrivateParking) {
		Matrix scoresMatrix = GeneralLib.readStringMatrix(getScoresFileName(iterationNumber));
		Matrix eventsMatrix = GeneralLib.readStringMatrix(getEventsFileName(iterationNumber));		
		return getScores(scoresMatrix,eventsMatrix,removePrivateParking);
	}
	
	public static class StrategyScoreLog{
		public StrategyScoreLog(String strategyName, double score) {
			super();
			this.strategyName = strategyName;
			this.score = score;
		}
		String strategyName;
		double score;
		
		public void print(){
			System.out.println(strategyName + ":" + score);
		}
	}

}

