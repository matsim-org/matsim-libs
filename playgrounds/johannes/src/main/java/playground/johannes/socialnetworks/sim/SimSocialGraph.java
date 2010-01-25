/* *********************************************************************** *
 * project: org.matsim.*
 * SocialNetwork.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.sim;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.contrib.sna.gis.CRSUtils;
import org.matsim.contrib.sna.graph.SparseVertex;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseGraph;

import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.social.SocialPerson;


/**
 * @author illenberger
 *
 */
public class SimSocialGraph extends SpatialSparseGraph implements SocialGraph {
	
	private Map<SocialPerson, SimSocialVertex> personEgoMapping = new HashMap<SocialPerson, SimSocialVertex>();
	
	/**
	 * @deprecated
	 */
	public SimSocialGraph() {
		super(CRSUtils.getCRS(21781)); //FIXME
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public Set<? extends SimSocialVertex> getVertices() {
		return (Set<? extends SimSocialVertex>) super.getVertices();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Set<? extends SimSocialEdge> getEdges() {
		return (Set<? extends SimSocialEdge>) super.getEdges();
	}

	public SimSocialVertex getEgo(SocialPerson p) {
		return personEgoMapping.get(p);
	}

	@Override
	public SimSocialEdge getEdge(SparseVertex v1, SparseVertex v2) {
		return (SimSocialEdge) super.getEdge(v1, v2);
	}

}
