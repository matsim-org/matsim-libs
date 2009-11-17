/* *********************************************************************** *
 * project: org.matsim.*
 * SampledSocialNetFactory.java
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
package playground.johannes.socialnetworks.survey.ivt2009;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.sna.graph.GraphFactory;


/**
 * @author illenberger
 *
 */
public class SampledSocialNetFactory<P extends Person> implements GraphFactory<SampledSocialNet<P>, SampledEgo<P>, SampledSocialTie> {

	public SampledSocialTie createEdge() {
		return new SampledSocialTie(0);
	}

	public SampledSocialNet<P> createGraph() {
		return new SampledSocialNet<P>();
	}

	public SampledEgo<P> createVertex() {
		throw new UnsupportedOperationException();
	}
	
	public SampledEgo<P> createVertex(P person) {
		return new SampledEgo<P>(person);
	}

}
