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

import org.matsim.api.basic.v01.population.BasicPerson;

import playground.johannes.socialnetworks.graph.GraphFactory;

/**
 * @author illenberger
 *
 */
public class SampledSocialNetFactory <P extends BasicPerson<?>> implements GraphFactory<SampledSocialNet<P>, SampledEgo<P>, SampledSocialTie> {

	public SampledSocialTie addEdge(SampledSocialNet<P> g, SampledEgo<P> v1,
			SampledEgo<P> v2) {
		SampledSocialTie e = new SampledSocialTie(v1, v2);
		if(g.insertEdge(e, v1, v2)) {
			return e;
		} else {
			return null;
		}
	}

	public SampledEgo<P> addVertex(SampledSocialNet<P> g) {
		throw new UnsupportedOperationException("Don't know what to with that...");
	}
	
	public SampledEgo<P> addVertex(SampledSocialNet<P> g, P person, int iteration) {
		SampledEgo<P> ego = new SampledEgo<P>(person);
		ego.detect(iteration);
		if(g.insertVertex(ego))
			return ego;
		else
			return null;
	}

	public SampledSocialNet<P> createGraph() {
		return new SampledSocialNet<P>();
	}

}
