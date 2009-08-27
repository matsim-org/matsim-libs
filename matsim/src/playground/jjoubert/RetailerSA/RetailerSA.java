/* *********************************************************************** *
 * project: org.matsim.*
 * RetailerSA.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.jjoubert.RetailerSA;

import java.util.ArrayList;
import java.util.Collections;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.MatsimRandom;

public class RetailerSA {
	private double temperatureReductionFrequency;
	private double temperatureReductionFactor;
	private double currentTemperature;
	private int iterationLimit;
	private ArrayList<Double> currentSolution;
	private double currentObjective;
	private ArrayList<Double> incumbentSolution;
	private double incumbentObjective;
	private MySaFitnessFunction fitnessFunction;
	private boolean isMax;
	private ArrayList<Double[]> solutionProgress;
	private int maximumTriesPerIteration;
	private double initialStepSize = 0.02;
	private double currentStepSize; 
	private final static Logger log = Logger.getLogger(RetailerSA.class);


	public RetailerSA(	double initialTemperature, 
			double temperatureReductionFrequency, 
			double temperatureReductionFactor, 
			int iterationLimit,
			ArrayList<Double> initialSolution,
			MySaFitnessFunction fitnessFunction,
			boolean isMax){
		maximumTriesPerIteration = (int) Math.max(5, ((double) iterationLimit)/((double)10));
		this.currentTemperature = initialTemperature;
		this.temperatureReductionFrequency = temperatureReductionFrequency;
		this.temperatureReductionFactor = temperatureReductionFactor;
		this.iterationLimit = iterationLimit;
		this.fitnessFunction = fitnessFunction;
		this.isMax = isMax;

		this.incumbentSolution = initialSolution;
		this.incumbentObjective = fitnessFunction.evaluate(initialSolution);

		this.currentSolution = this.incumbentSolution;
		this.currentObjective = this.incumbentObjective;

		currentTemperature = initialTemperature;
		currentStepSize = initialStepSize;
		solutionProgress = new ArrayList<Double[]>(iterationLimit);
		Double[] initial = {incumbentObjective, currentObjective};
		solutionProgress.add(initial);
	}

	public void estimateParameters(){

		int iterationCounter = 0;
		int iterationMultiplier = 1;
		while(iterationCounter < this.iterationLimit){

			performNeighbourhoodMove();

			Double[] progress = {incumbentObjective, currentObjective};
			solutionProgress.add(progress);
			iterationCounter++;
			if(iterationCounter%temperatureReductionFrequency == 0){
				currentTemperature *= temperatureReductionFactor;
			}
			// Report progress
			if(iterationCounter == iterationMultiplier){
				log.info("   Iteration: " + iterationCounter);
				iterationMultiplier *= 2;
			}
		}
		log.info("   Iteration: " + iterationCounter + " (Done)");
	}

	private void performNeighbourhoodMove(){
		int tryInnerMove = 0;
		int tryOuterMove = 0;
		boolean moveFound = false;

		while(tryOuterMove < maximumTriesPerIteration/5 && !moveFound){
			while(tryInnerMove < maximumTriesPerIteration && !moveFound){
				/*
				 * If you only want to select a random one of the four possible 
				 * neighbourhood steps.
				 */
				moveFound = stepSingle(moveFound);

				/*
				 * If you want to check ALL the neighbours. The problem here is that in
				 * the next iteration you WILL then find an improving step, returning
				 * to a previously visited solution.
				 */
//				moveFound = stepAll(moveFound);
				
				tryInnerMove++;
			}
			tryOuterMove++;
			if(!moveFound){
				log.warn("   Could not find an acceptable neighborhood move. Double the step-size.");
				currentStepSize *= 2;
			} 
//			else{
//				currentStepSize *= 0.5;
//			}
		}
		if(!moveFound){
			log.warn("   Could not find an acceptable neighborhood move. Return to original stepsize.");
			currentStepSize = initialStepSize;
		}
	}

	@SuppressWarnings("unchecked")
	private boolean stepAll(boolean moveFound) {
		ArrayList<Double> result = new ArrayList<Double>(this.currentSolution.size());
		ArrayList<Double> copyOfSolution;

		ArrayList<NeighbourMin> neighbourhood = new ArrayList<NeighbourMin>();
		for(int i = 0; i < this.currentSolution.size(); i++){
			copyOfSolution = (ArrayList<Double>) currentSolution.clone();
			copyOfSolution.set(i, copyOfSolution.get(i) + currentStepSize);
			neighbourhood.add(new NeighbourMin(copyOfSolution, fitnessFunction.evaluate(copyOfSolution)));
			copyOfSolution = (ArrayList<Double>) currentSolution.clone();
			copyOfSolution.set(i, copyOfSolution.get(i) - currentStepSize);
			neighbourhood.add(new NeighbourMin(copyOfSolution, fitnessFunction.evaluate(copyOfSolution)));
		}
		Collections.sort(neighbourhood);

		int index = 0;
		while(index < neighbourhood.size() && !moveFound){
			if(this.acceptSolution(neighbourhood.get(index).getSolution())){
				moveFound = true;
				result = neighbourhood.get(index).getSolution();
				currentSolution = result;
				currentObjective = fitnessFunction.evaluate(currentSolution);
				testIncumbent(currentSolution);
			}
			index++;
		}	
		return moveFound;
	}

	@SuppressWarnings("unchecked")
	private boolean stepSingle(boolean moveFound) {
		ArrayList<Double> copyOfSolution;
		copyOfSolution = (ArrayList<Double>) currentSolution.clone();
		int choice = MatsimRandom.getRandom().nextInt(4);
		switch (choice) {
		case 0:
			copyOfSolution.set(0, copyOfSolution.get(0) + currentStepSize);
			break;
		case 1:
			copyOfSolution.set(0, copyOfSolution.get(0) - currentStepSize);
		case 2:
			copyOfSolution.set(1, copyOfSolution.get(1) + currentStepSize);
			break;
		case 3:
			copyOfSolution.set(1, copyOfSolution.get(1) - currentStepSize);
		default:
			break;
		}
		if(this.acceptSolution(copyOfSolution)){
			moveFound = true;
			currentSolution = copyOfSolution;
			currentObjective = fitnessFunction.evaluate(currentSolution);
			testIncumbent(currentSolution);
		}
		return moveFound;
	}


	private void testIncumbent(ArrayList<Double> result){
		double resultObjective = fitnessFunction.evaluate(result);
		if(isMax){
			if(resultObjective > incumbentObjective){
				incumbentSolution = result;
				incumbentObjective = resultObjective;
			}			
		} else{
			if(resultObjective < incumbentObjective){
				incumbentSolution = result;
				incumbentObjective = resultObjective;
			}
		}
	}


	private boolean acceptSolution(ArrayList<Double> solution){

		double thisObjective = fitnessFunction.evaluate(solution);
		double difference = this.isMax ? this.currentObjective - thisObjective : thisObjective - this.currentObjective;
		if(difference < 0){
			return true;
		} else{
			double randomValue = MatsimRandom.getRandom().nextDouble();
			if(Math.exp(-difference / currentTemperature) > randomValue){
				return true;
			} else{
				return false;
			}
		}		

	}




	public ArrayList<Double> getIncumbentSolution() {
		return incumbentSolution;
	}

	public double getIncumbentObjective() {
		return incumbentObjective;
	}



	public ArrayList<Double[]> getSolutionProgress() {
		return solutionProgress;
	}


	public void setIterationLimit(int iterationLimit) {
		this.iterationLimit = iterationLimit;
	}
	public int getIterationLimit() {
		return iterationLimit;
	}

}
