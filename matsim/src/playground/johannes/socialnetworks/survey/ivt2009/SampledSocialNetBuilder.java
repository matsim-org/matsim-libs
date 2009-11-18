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
import org.matsim.contrib.sna.graph.AbstractSparseGraphBuilder;


/**
 * @author illenberger
 *
 */
public class SampledSocialNetBuilder <P extends Person> extends AbstractSparseGraphBuilder<SampledSocialNet<P>, SampledEgo<P>, SampledSocialTie> {

	public SampledSocialNetBuilder() {
		super(new SampledSocialNetFactory<P>());
	}

	@Override
	public SampledEgo<P> addVertex(SampledSocialNet<P> g) {
		throw new UnsupportedOperationException("Don't know what to with that...");
	}
	
	public SampledEgo<P> addVertex(SampledSocialNet<P> g, P person, int iteration) {
		SampledEgo<P> ego = new SampledEgo<P>(person);
		ego.detect(iteration);
		if(insertVertex(g, ego))
			return ego;
		else
			return null;
	}

}
