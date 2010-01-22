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
package playground.johannes.socialnetworks.graph.social;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.contrib.sna.gis.CRSUtils;
import org.matsim.contrib.sna.graph.SparseVertex;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseGraph;


/**
 * @author illenberger
 *
 */
public class SocialNetwork extends SpatialSparseGraph {
	
	private Map<SocialPerson, Ego> personEgoMapping = new HashMap<SocialPerson, Ego>();
	
	/**
	 * @deprecated
	 */
	public SocialNetwork() {
		super(CRSUtils.getCRS(21781)); //FIXME
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public Set<? extends Ego> getVertices() {
		return (Set<? extends Ego>) super.getVertices();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Set<? extends SocialTie> getEdges() {
		return (Set<? extends SocialTie>) super.getEdges();
	}

	public Ego getEgo(SocialPerson p) {
		return personEgoMapping.get(p);
	}

	@Override
	public SocialTie getEdge(SparseVertex v1, SparseVertex v2) {
		return (SocialTie) super.getEdge(v1, v2);
	}

}
