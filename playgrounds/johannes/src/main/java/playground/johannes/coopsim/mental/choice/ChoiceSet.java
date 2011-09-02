/* *********************************************************************** *
 * project: org.matsim.*
 * ChoiceSet.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.coopsim.mental.choice;

import gnu.trove.TDoubleArrayList;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author illenberger
 *
 */
public class ChoiceSet<T> {

	private final List<T> options;
	
	private final TDoubleArrayList weights;
	
	private final Random random;
	
	private double weightSum;
	
	public ChoiceSet(Random random) {
		this.random = random;
		
		this.options = new ArrayList<T>();
		this.weights = new TDoubleArrayList();
		
		weightSum = 0;
	}
	
	public void addChoice(T choice) {
		addChoice(choice, 1.0);
	}
	
	public void addChoice(T choice, double weight) {
		options.add(choice);
		weights.add(weight);
		
		weightSum += weight;
	}
	
	public T randomChoice() {
		return options.get(random.nextInt(options.size()));
	}
	
	public T randomWeightedChoice() {
		double weight = random.nextDouble() * weightSum;
		double sum = 0;
		for(int i = 0; i < options.size(); i++) {
			sum += weights.get(i);
			if(weight <= sum)
				return options.get(i);
		}
		
		return null;
	}
}
