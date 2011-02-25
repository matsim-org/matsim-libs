/* *********************************************************************** *
 * project: org.matsim.*
 * ErgmGender.java
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
package playground.johannes.socialnetworks.graph.social.generators;

import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.matrix.AdjacencyMatrix;

import playground.johannes.socialnetworks.graph.mcmc.ErgmTerm;
import playground.johannes.socialnetworks.graph.social.SocialVertex;
import playground.johannes.socialnetworks.graph.social.analysis.Gender;

/**
 * @author illenberger
 *
 */
public class ErgmGender extends ErgmTerm {

	public ErgmGender(double theta) {
		setTheta(theta);
	}

	@Override
	public <V extends Vertex> double ratio(AdjacencyMatrix<V> y, int i, int j, boolean y_ij) {
		int gender_i = 0;
		int gender_j = 0;
		
		if(Gender.MALE.equalsIgnoreCase(((SocialVertex)y.getVertex(i)).getPerson().getPerson().getSex()))
			gender_i = 1;
		
		if(Gender.MALE.equalsIgnoreCase(((SocialVertex)y.getVertex(j)).getPerson().getPerson().getSex()))
			gender_j = 1;
		
		return Math.exp(- getTheta() * Math.abs(gender_i - gender_j));
	}

}
