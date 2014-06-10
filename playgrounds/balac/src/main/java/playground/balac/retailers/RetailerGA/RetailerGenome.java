/* *********************************************************************** *
 * project: org.matsim.*
 * RetailerGenome.java
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

package playground.balac.retailers.RetailerGA;

import java.util.ArrayList;

public class RetailerGenome implements Comparable<RetailerGenome>, Cloneable{
	private double fitness;
	private final boolean isMax;
	private final ArrayList<Integer> genome;
	
	public RetailerGenome(double fitness, boolean isMax, ArrayList<Integer> genome){
		this.fitness = fitness;
		this.isMax = isMax;
		this.genome = genome;
	}

	public double getFitness() {
		return fitness;
	}

	public boolean isMax() {
		return isMax;
	}
	
	public ArrayList<Integer> getGenome() {
		return genome;
	}

	public int compareTo(RetailerGenome gn) {
		if(this.isMax){
			if (this == gn) return 0;
			if (this.getFitness() < gn.getFitness()) return -1;
			if (this.getFitness() == gn.getFitness()) return 0;
			return 1;
			
		} else{
			return (int) (this.getFitness() - gn.getFitness());
		}
	}
	
	@Override
	public ArrayList<Integer> clone(){
		ArrayList<Integer> result = new ArrayList<Integer>(genome.size());
		for (Integer integer : this.genome) {
			result.add(Integer.valueOf(integer));
		}
		return result;
	}
	
}
