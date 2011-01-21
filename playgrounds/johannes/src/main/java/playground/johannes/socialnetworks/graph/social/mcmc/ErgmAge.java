/* *********************************************************************** *
 * project: org.matsim.*
 * AgeCorrelation.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.graph.social.mcmc;

import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.matrix.AdjacencyMatrix;

import playground.johannes.socialnetworks.graph.mcmc.ErgmTerm;
import playground.johannes.socialnetworks.graph.social.SocialVertex;

/**
 * @author illenberger
 *
 */
public class ErgmAge extends ErgmTerm {

	public ErgmAge(double theta) {
		setTheta(theta);
	}
	
	@Override
	public <V extends Vertex> double ratio(AdjacencyMatrix<V> y, int i, int j, boolean yIj) {
		int a1 = ((SocialVertex) y.getVertex(i)).getPerson().getAge();
		int a2 = ((SocialVertex) y.getVertex(j)).getPerson().getAge();
		
		if(a1 > 0)
			return Math.exp(- getTheta() * Math.abs((a2 - a1)/(double)a1));
		else
			return 0;
	}
}