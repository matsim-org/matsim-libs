/* *********************************************************************** *
 * project: org.matsim.*
 * ErgmTerm.java
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

import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.matrix.AdjacencyMatrix;


/**
 * @author illenberger
 *
 */
public abstract class ErgmTerm implements EnsembleProbability {

	private double theta;
	
	public void setTheta(double theta) {
		this.theta = theta;
	}
	
	public double getTheta() {
		return theta;
	}
	
	abstract public <V extends Vertex> double ratio(AdjacencyMatrix<V> y, int i, int j, boolean y_ij);
	
}
