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

package playground.ciarif.retailers.RetailerGA;

import java.util.ArrayList;
import java.util.Collections;

import org.matsim.demandmodeling.primloc.CumulativeDistribution;

public class RetailerGA {
	private final int populationSize;
	private int genomeLength;
	private ArrayList<RetailerGenome> generation;
	private MyPermutator permutator = new MyPermutator();
	private final MyFitnessFunction fitnessFunction;
	private final RetailerGenome initialSolution;
	private RetailerGenome incumbent;
	private CumulativeDistribution cdf;
	private double best;
	private double average;
	private double worst;
	private final int numberOfMutationsToMutant = 5;
	private final double diversityThreshold = 0.25;
	private final double diversityMutationFraction = 0.25;
	

	public RetailerGA(	int populationSize, 
						int genomeLength, 
						MyFitnessFunction fitnessFunction, 
						ArrayList<Integer> initialSolution) {
		
		this.populationSize = populationSize;
		this.genomeLength = genomeLength;
		this.fitnessFunction = fitnessFunction;
		double firstFitness = fitnessFunction.evaluate(initialSolution);
		this.initialSolution = new RetailerGenome(firstFitness, 
											this.fitnessFunction.isMax(),
											initialSolution);
		this.incumbent = this.initialSolution;
		generation = new ArrayList<RetailerGenome>(populationSize);
		this.generateFirstGeneration();
	}
	
	public void generateFirstGeneration(){
		if(this.generation.size() > 0){
			System.err.println("Trying to overwrite an existing generation!!");
			System.exit(0);
		} else{
			this.generation.add(this.initialSolution);

			for(int i = 0; i < this.populationSize-1; i++){
				/*
				 * To ensure diversity, each member of the population is at least 5
				 * swaps from the original genome. This might be too many for short
				 * genomes, but too FEW for large genomes.
				 */
				ArrayList<Integer> newSolution = this.initialSolution.clone();
				for(int j = 0; j < numberOfMutationsToMutant; j++){
					newSolution = this.mutate(newSolution);
				}
				double newSolutionFitness = this.fitnessFunction.evaluate(newSolution);
				RetailerGenome newGenome = new RetailerGenome(newSolutionFitness, 
											this.fitnessFunction.isMax(),
											newSolution);
				this.generation.add(newGenome);
				
				// Check if the new solution is better than the incumbent.
				checkIncumbent(newGenome);
			}
		}
		Collections.sort(this.generation);
		if(this.fitnessFunction.isMax()){
			setBest(Double.NEGATIVE_INFINITY);
			setAverage(Double.NEGATIVE_INFINITY);
			setWorst(Double.NEGATIVE_INFINITY);
		} else{
			setBest(Double.POSITIVE_INFINITY);
			setAverage(Double.POSITIVE_INFINITY);
			setWorst(Double.POSITIVE_INFINITY);
		}
		calculateStats();
		buildCDF();
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
		Collections.swap(result, pos1, pos2);
		
		return result;
	}

	public int getPopulationSize() {
		return populationSize;
	}
	

	private void checkIncumbent(RetailerGenome genome){
		if(this.fitnessFunction.isMax()){
			if(genome.getFitness() > this.incumbent.getFitness()){
				this.incumbent = genome;
			}
		} else{
			if(genome.getFitness() < this.incumbent.getFitness()){
				this.incumbent = genome;
			}			
		}
	}
	
	private void checkDiversity() {
		ArrayList<RetailerGenome> incumbentList = new ArrayList<RetailerGenome>();
		RetailerGenome thisGenome = null;
		
		int pos = 0;
		boolean end = false;
		while(!end && pos < populationSize){
			thisGenome = this.generation.get(pos);
			if(thisGenome.getFitness() == this.incumbent.getFitness()){
				incumbentList.add(thisGenome);
				pos++;
			} else{
				end = true;
			}
		}
		if( ((double)incumbentList.size())/((double)populationSize) > diversityThreshold){
			for(int i = 0; i < incumbentList.size()*diversityMutationFraction; i++){
				thisGenome = incumbentList.get(i);
				if(thisGenome != this.incumbent){
					ArrayList<Integer> newGenome = this.mutate(thisGenome.getGenome());
					for(int j = 0; j < genomeLength; j++){
						thisGenome.getGenome().set(j, newGenome.get(j));
					}
				}
			}
			Collections.sort(this.generation);
		}		
	}

	private void buildCDF(){
		double [] x = new double[populationSize+1];
		double [] y = new double[populationSize+1];
		x[0] = -0.5;
		y[0] = 0.0;
		x[populationSize] = populationSize - 1;
		y[populationSize] = 1.0;
		
		ArrayList<Double> copy = new ArrayList<Double>(populationSize);
		if(this.fitnessFunction.isMax()){
			double minValue = 0.95*this.generation.get(populationSize - 1).getFitness();
			for (RetailerGenome gn : this.generation) {
				copy.add(Double.valueOf(gn.getFitness()) - minValue);
			}
		} else{
			double maxValue = 1.05*this.generation.get(populationSize - 1).getFitness();
			for (RetailerGenome gn : this.generation) {
				copy.add(maxValue - Double.valueOf(gn.getFitness()));
			}
		}
		double total = 0;
		for (Double d : copy) {
			total += d;
		}
		for(int i = 0; i < populationSize-1; i++){
			x[i+1] = Double.valueOf(i) + 0.5;
			y[i+1] = y[i] + (copy.get(i) / total);
		}
		
		this.cdf = new CumulativeDistribution(x, y);
	}

	@Override
	public String toString(){
		Collections.sort(this.generation);
		String result = new String();
		for(int i = 0; i < populationSize; i++){
			result += String.valueOf(i+1) + ":\t";
			for (int j = 0; j < genomeLength-1; j++) {
				result += String.valueOf(generation.get(i).getGenome().get(j)) + " -> ";
			}
			result += String.valueOf(generation.get(i).getGenome().get(genomeLength-1)) + 
			"\t\t" + String.valueOf(generation.get(i).getFitness());
			if(this.incumbent == this.generation.get(i)){
				result += "*";
			}
			result += "\n";
		}		
		return result;
	}

	public void evolve(double elites, double mutants, int crossoverType, ArrayList<Integer> precedenceVector) {
		int numElites = (int) Math.max(1, Math.round(elites*populationSize));
		int numMutants = (int) Math.max(1, Math.round(mutants*populationSize));
		int numCrossovers = populationSize - numElites - numMutants;
		
		ArrayList<RetailerGenome> newGeneration = new ArrayList<RetailerGenome>(populationSize);
		
		/* 
		 * Add all the elites
		 */
		newGeneration.add(this.incumbent);
		for(int i = 1; i < numElites; i++){
			newGeneration.add(this.generation.get(i));
		}
		
		/* 
		 * Add all the mutants. An individual is randomly selected (weighted) from
		 * the current generation, and 5 random swaps are performed.
		 */
		for(int i = 0; i < numMutants; i++){
			int pos = (int) this.cdf.sampleFromCDF();
			ArrayList<Integer> al = generation.get(pos).clone();
			for(int j = 0; j < numberOfMutationsToMutant; j++){
				ArrayList<Integer> newAl = this.mutate(al);
				for(int k = 0; k < genomeLength; k++){
					al.set(k, newAl.get(k));
				}
			}
			RetailerGenome newGn = new RetailerGenome(this.fitnessFunction.evaluate(al), 
													  this.fitnessFunction.isMax(), 
													  al);
			newGeneration.add(newGn);			
		}
		
		/*
		 * Fill the rest of the new generation with offspring. 
		 */
		
		/*
		 * Perform crossover based on mechanism selected.
		 */
		switch (crossoverType) {
		case 1: 
			/* Enhanced Edge Recombination (EER) from Michalewicz (1992) produces
			 * a single offspring from two parents. 
			 */
			
		 break;
			
			
		case 2: 
			/* Merged crossover (MX) first introduced by Blanton & 
			 * Wainwright (1993) produces a single offspring from two parents. 
			 */
			if(precedenceVector != null){
				int i = 0;
				while(i < numCrossovers){
					ArrayList<Integer> P1 = this.generation.get((int) this.cdf.sampleFromCDF()).clone();
					ArrayList<Integer> P2 = this.generation.get((int) this.cdf.sampleFromCDF()).clone();

					RetailerGenome offspring = this.performMX(P1, P2, precedenceVector);
					newGeneration.add(offspring);
					i++;				
				}
			} else{
				System.err.println("Trying to perform Merged Crossover without a precedence vector!");
				System.exit(0);
			}
			
			break;
			
		case 3: 
			/* Partially Matched Crossover (PMX) from Goldberg & Lingle (1985)
			 * produces two offspreing from two parents.
			 */
			int i = 0;
			while(i < numCrossovers){

				ArrayList<Integer> P1 = this.generation.get((int) this.cdf.sampleFromCDF()).clone();
				ArrayList<Integer> P2 = this.generation.get((int) this.cdf.sampleFromCDF()).clone();
				
				ArrayList<RetailerGenome> offspring = this.performPMX(P1, P2);				
				
				if(newGeneration.size() < this.populationSize - 1){
					newGeneration.addAll(offspring);
					i+= 2;
				} else{
					newGeneration.add(offspring.get(0));
					i++;
				}
			}
			if(newGeneration.size() != this.populationSize){
				System.err.printf("After PMX the new generation is of size %d, and not %d", newGeneration.size(), this.populationSize);
				System.exit(0);
			}
			break;
			
		default:
			System.err.printf("Crossover type %d not implemented!", crossoverType);
			break;
		}		

		/*
		 * Set the newly generated population as the current generation.
		 */
		for(int j = 0; j < populationSize; j++){
			this.generation.set(j, newGeneration.get(j));
			checkIncumbent(newGeneration.get(j));
		}
		Collections.sort(this.generation);
		/*
		 * Check for diversity. This is done by checking how many solutions share the same
		 * fitness as the incumbent. If the threshold is exceeded, mutate half of them.
		 */
		checkDiversity();
		calculateStats();
		buildCDF();
	}
	
	private void calculateStats() {
		double total = 0;
		for (RetailerGenome rg : this.generation) {
			total += rg.getFitness();
		}
		setBest(this.incumbent.getFitness());
		setAverage(total / populationSize);
		setWorst(generation.get(populationSize-1).getFitness());		
	}

	public ArrayList<RetailerGenome> performPMX(ArrayList<Integer> P1,
												ArrayList<Integer> P2){
		
		ArrayList<RetailerGenome> offspring = new ArrayList<RetailerGenome>(2);
		
		// Generate two empty offspring ArrayLists, only containing zeros
		ArrayList<Integer> c1 = new ArrayList<Integer>(this.genomeLength);
		ArrayList<Integer> c2 = new ArrayList<Integer>(this.genomeLength);
		for(int i = 0; i < this.genomeLength; i++){
			c1.add(Integer.MIN_VALUE);
			c2.add(Integer.MIN_VALUE);
		}
		
		/* 
		 * Generate 2 random points, and cross-copy the segments from the parent between
		 * the two points to the alternate offspring. 
		 */		
		ArrayList<Integer> twoPoints = permutator.permutate(genomeLength);
		int pointA = Math.min(twoPoints.get(0), twoPoints.get(1));
		int pointB = Math.max(twoPoints.get(0), twoPoints.get(1));
		for(int j = pointA-1; j <= pointB-1; j++){
			c1.set(j, P2.get(j));
			c2.set(j, P1.get(j));
		}

		// Finish first offspring
		int pos = 0;
		while(pos < this.genomeLength){
			// Find out if C1 contains P1(pos)
			if(c1.contains(P1.get(pos))){
				pos++;
			} else{
				int thePos = findPmxPosition(pos, c1, P1);
				c1.set(thePos, P1.get(pos));
				pos++;
			}
		}
		// Finish second offspring
		pos = 0;
		while(pos < this.genomeLength){
			// Find out if C2 contains P2(pos)
			if(c2.contains(P2.get(pos))){
				pos++;
			} else{
				int thePos = findPmxPosition(pos, c2, P2);
				c2.set(thePos, P2.get(pos));
				pos++;
			}
		}
		RetailerGenome offspring1 = new RetailerGenome(this.fitnessFunction.evaluate(c1), 
													   this.fitnessFunction.isMax(),
													   c1);
		RetailerGenome offspring2 = new RetailerGenome(this.fitnessFunction.evaluate(c2), 
				   									   this.fitnessFunction.isMax(),
				   									   c2);
		offspring.add(offspring1);
		offspring.add(offspring2);
		
		return offspring;		
	}
	
	private RetailerGenome performMX(ArrayList<Integer> p1,
									 ArrayList<Integer> p2,
									 ArrayList<Integer> precedence) {

		ArrayList<Integer> c1 = new ArrayList<Integer>(genomeLength);
		for(int i = 0; i < genomeLength; i++){
			int p1Element = p1.get(i);
			int p2Element = p2.get(i);
			int posP1 = precedence.indexOf(p1Element);
			int posP2 = precedence.indexOf(p2Element);
			if(posP1 < posP2){
				c1.add(p1Element);
				int p2SwapPos = p2.indexOf(p1Element);
				Collections.swap(p2, i, p2SwapPos);
			} else{
				c1.add(p2Element);
				int p1SwapPos = p1.indexOf(p2Element);
				Collections.swap(p1, i, p1SwapPos);
			}
		}
		RetailerGenome gn = new RetailerGenome(this.fitnessFunction.evaluate(c1), 
											   this.fitnessFunction.isMax(),
											   c1);
		return gn;
	}

	private int findPmxPosition(int position, ArrayList<Integer> thisList, ArrayList<Integer> parentList){
		int result = Integer.MIN_VALUE;
		if(thisList.get(position) == Integer.MIN_VALUE){
			result = position;
		} else{
			int filledPoint = thisList.get(position);
			int filledPointPosition = parentList.indexOf(filledPoint);
			result = findPmxPosition(filledPointPosition, thisList, parentList);			
		}
		
		return result;
	}


	
	public RetailerGenome getIncumbent() {
		return incumbent;
	}

	private void setBest(double best) {
		this.best = best;
	}

	private void setAverage(double average) {
		this.average = average;
	}

	private void setWorst(double worst) {
		this.worst = worst;
	}
	
	public ArrayList<Double> getStats(){
		ArrayList<Double> result = new ArrayList<Double>(3);
		result.add(this.best);
		result.add(this.average);
		result.add(this.worst);
		return result;
	}



	
}
