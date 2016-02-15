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
package org.matsim.contrib.socnetgen.sna.graph.social.generators;


import org.matsim.contrib.socnetgen.sna.graph.Vertex;
import org.matsim.contrib.socnetgen.sna.graph.matrix.AdjacencyMatrix;
import org.matsim.contrib.socnetgen.sna.graph.mcmc.ErgmTerm;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialVertex;
import org.matsim.contrib.socnetgen.sna.graph.social.analysis.Gender;
import org.matsim.core.population.PersonUtils;

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
		
		if(Gender.MALE.equalsIgnoreCase(PersonUtils.getSex(((SocialVertex) y.getVertex(i)).getPerson().getPerson())))
			gender_i = 1;
		
		if(Gender.MALE.equalsIgnoreCase(PersonUtils.getSex(((SocialVertex) y.getVertex(j)).getPerson().getPerson())))
			gender_j = 1;
		
		return Math.exp(- getTheta() * Math.abs(gender_i - gender_j));
	}

}
