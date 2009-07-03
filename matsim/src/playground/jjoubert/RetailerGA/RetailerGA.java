/* *********************************************************************** *
 * project: org.matsim.*
 * RetailerGA.java
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

package playground.jjoubert.RetailerGA;

import java.util.ArrayList;

public class RetailerGA {
	private final int populationSize;
	private int genomeLength;
	private ArrayList<RetailerGenome> generation;
	private MyPermutator permutator = new MyPermutator();
	private final MyFitnessFunction fitnessFunction;
	private final RetailerGenome initialSolution;
	private RetailerGenome incumbent;
	

	public RetailerGA(	int populationSize, 
						int genomeLength, 
						MyFitnessFunction fitnessFunction, 
						ArrayList<Integer> initialSolution) {
		
		this.populationSize = populationSize;
		this.genomeLength = genomeLength;
		this.fitnessFunction = fitnessFunction;
		double firstFitness = fitnessFunction.evaluate(initialSolution);
		this.initialSolution = new RetailerGenome(firstFitness, initialSolution);
		this.incumbent = this.initialSolution;
		generation = new ArrayList<RetailerGenome>(populationSize);
	}
	
	public void generateFirstGeneration(){
		if(this.generation.size() > 0){
			System.err.println("Trying to overwrite an existing generation!!");
			System.exit(0);
		} else{
			this.generation.add(this.initialSolution);

			for(int i = 0; i < this.populationSize-1; i++){
				ArrayList<Integer> newSolution = this.mutate(this.initialSolution.getGenome());
				double newSolutionFitness = this.fitnessFunction.evaluate(newSolution);
				this.generation.add(new RetailerGenome(newSolutionFitness, newSolution));
			}
		}
	}
	
	public ArrayList<Integer> mutate(ArrayList<Integer> genome){
		/*
		 * Create a copy of the original solution.
		 */
		ArrayList<Integer> result = new ArrayList<Integer>(this.genomeLength);
		for (Integer integer : genome) {
			int i = Integer.valueOf(integer);
			result.add(i);
		}
		/*
		 * Perform a random swap of two values.
		 */
		ArrayList<Integer> strip = permutator.permutate(genomeLength);
		int pos1 = strip.get(0)-1;
		int pos2 = strip.get(1)-1;
		result.set(pos1, genome.get(pos2));
		result.set(pos2, genome.get(pos1));		
		
		return result;
	}

	public int getPopulationSize() {
		return populationSize;
	}
	
	public String toString(){
		String result = new String();
		for(int i = 0; i < populationSize; i++){
			result += String.valueOf(i+1) + ":\t";
			for (int j = 0; j < genomeLength-1; j++) {
				result += String.valueOf(generation.get(i).getGenome().get(j)) + " -> ";
			}
			result += String.valueOf(generation.get(i).getGenome().get(genomeLength-1)) + 
					"\t\t" + String.valueOf(generation.get(i).getFitness()) + "\n";
		}		
		return result;
	}

}
