/* *********************************************************************** *
 * project: org.matsim.*
 * ConditionalDistribution.java
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

/**
 * 
 */
package playground.johannes.socialnetworks.graph.mcmc;


/**
 * @author illenberger
 *
 */
public interface ConditionalDistribution {

	public double changeStatistic(AdjacencyMatrix y, int i, int j, boolean y_ij);
	
//	public double getNormConstant(int i);
	public void addEdge(AdjacencyMatrix y, int i, int j);
	
	public void removeEdge(AdjacencyMatrix y, int i, int j);
}
