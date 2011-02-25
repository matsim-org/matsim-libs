/* *********************************************************************** *
 * project: org.matsim.*
 * Ergm.java
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
import org.matsim.contrib.sna.util.Composite;




/**
 * @author illenberger
 *
 */
public class Ergm extends Composite<EnsembleProbability> implements EnsembleProbability {

	public <V extends Vertex> double ratio(AdjacencyMatrix<V> y, int i, int j, boolean y_ij) {
		double prod = 1;
		for(int k = 0; k < components.size(); k++) {
			prod *= components.get(k).ratio(y, i, j, y_ij);
		}
		
		if(Double.isNaN(prod))
			throw new IllegalArgumentException("H(y) must not be NaN!");
		
		return prod;
	}	
}
