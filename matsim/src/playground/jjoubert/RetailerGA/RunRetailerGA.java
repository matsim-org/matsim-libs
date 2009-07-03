/* *********************************************************************** *
 * project: org.matsim.*
 * RunRetailerGA.java
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

public class RunRetailerGA {

	
	
	public static void main(String[] args) {
		
		int genomeLength = 10;
		int populationSize = 10;
		int numberOfGenerations = 1;
		
		/*
		 * Just for me to create a fictitious  solution
		 */
		MyPermutator p = new MyPermutator();
		ArrayList<Integer> first = p.permutate(genomeLength);
		
		MyFitnessFunction ff = new MyFitnessFunction(false, genomeLength);
		RetailerGA ga = new RetailerGA(populationSize, genomeLength, ff, first);
		ga.generateFirstGeneration();
		for(int i = 0; i < numberOfGenerations; i++){
			// TODO Evolve
		}
		
		String out = ga.toString();
		System.out.printf(out);
	}	

}
