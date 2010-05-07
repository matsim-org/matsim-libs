/* *********************************************************************** *
 * project: org.matsim.*
 * ErgmAge.java
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
package playground.johannes.socialnetworks.graph.social.mcmc;

import org.matsim.contrib.sna.graph.matrix.AdjacencyMatrix;
import org.matsim.core.population.PersonImpl;

import playground.johannes.socialnetworks.graph.mcmc.ErgmTerm;

/**
 * @author illenberger
 *
 */
public class ErgmAge extends ErgmTerm {

	@Override
	public double changeStatistic(AdjacencyMatrix m, int i, int j, boolean y_ij) {
		int age1 = ((SNAdjacencyMatrix<PersonImpl>)m).getVertex(i).getPerson().getAge();
		int age2 = ((SNAdjacencyMatrix<PersonImpl>)m).getVertex(j).getPerson().getAge();
		
//		if(age1 == age2)
//			return getTheta();
//		else
//			return 0;
		
		return getTheta() * Math.min(age1, age2) / Math.max(age1, age2);
	}


}
