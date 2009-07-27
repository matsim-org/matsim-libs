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
	
	private int maximumTriesPerIteration = 1000;
	private double initialStepSize = 0.001;
	private double currentStepSize; 
	private final static Logger log = Logger.getLogger(RetailerSA.class);

	
	public RetailerSA(	double initialTemperature, 
						double temperatureReductionFrequency, 
						double temperatureReductionFactor, 
						int iterationLimit,
						ArrayList<Double> initialSolution,
						MySaFitnessFunction fitnessFunction,
						boolean isMax){
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
		
		int iteration = 0;
		while(iteration < this.iterationLimit){
			int solutionSize = currentSolution.size();
			
			
			Double[] progress = {incumbentObjective, currentObjective};
			solutionProgress.add(progress);
			iteration++;
			if(iteration%temperatureReductionFrequency == 0){
				currentTemperature *= temperatureReductionFactor;
			}
		}		
	}
	
	@SuppressWarnings("unchecked")
	private ArrayList<Double> performNeighbourhoodMove(){
		ArrayList<Double> result = new ArrayList<Double>(this.currentSolution.size());
		int tryInnerMove = 0;
		int tryOuterMove = 0;
		boolean moveFound = false;
		
		ArrayList<Double> copyOfSolution;
		while(tryOuterMove < maximumTriesPerIteration/5 && !moveFound){
			while(tryInnerMove < maximumTriesPerIteration && !moveFound){
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
					}
					index++;
				}		
				
				tryInnerMove++;
			}
			tryOuterMove++;
			if(result.size() == 0){
				log.warn("Could not find an acceptable neighborhood move. Double the step-size.");
				currentStepSize *= 2;
			}
		}
		if(result.size() == 0){
			log.warn("Could not find an acceptable neighborhood move. Return to original stepsize.");
			currentStepSize = initialStepSize;
		}
		
		return result;
	}
	
	private boolean acceptSolution(ArrayList<Double> solution){

		double thisObjective = fitnessFunction.evaluate(solution);
		double difference = this.isMax ? this.currentObjective - thisObjective : thisObjective - this.currentObjective;
		if(difference > 0){
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
