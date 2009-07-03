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

package playground.jjoubert.RetailerGA;

import java.util.ArrayList;

public class RetailerGenome {
	private double fitness;
	private final ArrayList<Integer> genome;
	
	public RetailerGenome(double fitness, ArrayList<Integer> genome){
		this.fitness = fitness;
		this.genome = genome;
	}

	public double getFitness() {
		return fitness;
	}
	
	public ArrayList<Integer> getGenome() {
		return genome;
	}
	
}
