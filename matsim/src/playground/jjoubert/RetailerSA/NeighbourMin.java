/* *********************************************************************** *
 * project: org.matsim.*
 * Neighbour.java
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

public class NeighbourMin implements Comparable<NeighbourMin>{

	private ArrayList<Double> solution;
	private Double solutionObjective;
	
	public NeighbourMin(ArrayList<Double> solution, Double objective){
		this.solution = solution;
		this.solutionObjective = objective;
	}
	
	public int compareTo(NeighbourMin o) {
		return (int) Math.signum(this.solutionObjective - o.solutionObjective);
	}

	public ArrayList<Double> getSolution() {
		return solution;
	}

	public Double getSolutionObjective() {
		return solutionObjective;
	}

}
