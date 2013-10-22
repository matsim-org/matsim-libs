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
package playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.analysis;

import java.util.ArrayList;
import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.lib.GeneralLib;

import playground.wrashid.parkingSearch.planLevel.init.ParkingRoot;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.manager.EvaluationContainer;
import playground.wrashid.parkingSearch.ppSim.jdepSim.zurich.ZHScenarioGlobal;

public class StrategyScoreStats {

	private ArrayList<Double> averageBestList;
	private ArrayList<Double> averageExecutedList;
	private ArrayList<Double> averageWorstList;
	private ArrayList<Double> averageAverageList;
	
	public StrategyScoreStats(){
		averageBestList = new ArrayList();
		averageExecutedList = new ArrayList();
		averageWorstList = new ArrayList();
		averageAverageList = new ArrayList();
	}
	
	
	public void addIterationData(HashMap<Id, HashMap<Integer, EvaluationContainer>> strategyEvaluations) {
		double averageBest=0;
		double averageExecuted=0;
		double averageWorst=0;
		double averageAverage=0;
		
		for (Id personId : strategyEvaluations.keySet()) {
			for (Integer legIndex : strategyEvaluations.get(personId).keySet()) {
				EvaluationContainer evaluationContainer = strategyEvaluations.get(personId).get(legIndex);
				averageBest+=evaluationContainer.getBestStrategyScore();
				averageExecuted+=evaluationContainer.getSelectedStrategyScore();
				averageWorst+=evaluationContainer.getWorstStrategyScore();
				averageAverage+=evaluationContainer.getAverageStrategyScore();
			}
		}
		
		int numberOfPeople = strategyEvaluations.keySet().size();
		averageBestList.add(averageBest/numberOfPeople);
		averageExecutedList.add(averageExecuted/numberOfPeople);
		averageWorstList.add(averageWorst/numberOfPeople);
		averageAverageList.add(averageAverage/numberOfPeople);
	}


	public void writeDataToFile() {
		String xLabel = "Iteration";
		String yLabel = "score";
		String title="Parking Strategy Score";
		int numberOfXValues = averageBestList.size();
		int numberOfFunctions = 4;
		double[] xValues=new double[numberOfXValues];
		String[] seriesLabels=new String[numberOfFunctions];
		
		seriesLabels[0]="average best";
		seriesLabels[1]="average executed";
		seriesLabels[2]="average worst";
		seriesLabels[3]="average average";
		
		double[][] matrix=new double[numberOfXValues][numberOfFunctions];
		
		for (int i=0;i<numberOfXValues;i++){
			matrix[i][0]=averageBestList.get(i);
			matrix[i][1]=averageExecutedList.get(i);
			matrix[i][2]=averageWorstList.get(i);
			matrix[i][3]=averageAverageList.get(i);
			xValues[i]=i;
		}

		GeneralLib.writeGraphic(ZHScenarioGlobal.outputFolder + "parkingStrategyScores.png", matrix, title, xLabel, yLabel, seriesLabels, xValues);
		
	}
}

