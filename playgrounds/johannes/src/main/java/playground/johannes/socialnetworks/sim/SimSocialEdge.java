/* *********************************************************************** *
 * project: org.matsim.*
 * SocialTie.java
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
package playground.johannes.socialnetworks.sim;

import gnu.trove.TIntArrayList;

import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseEdge;
import org.matsim.core.utils.collections.Tuple;

import playground.johannes.socialnetworks.graph.social.SocialEdge;


/**
 * @author illenberger
 *
 */
public class SimSocialEdge extends SpatialSparseEdge implements SocialEdge {

	private int created;
	
	private TIntArrayList usage;
	
	private int lastUsed;
	
	protected SimSocialEdge(int created) {
		this.created = created;
		usage = new TIntArrayList();
		usage.add(created);
		lastUsed = created;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Tuple<? extends SimSocialVertex, ? extends SimSocialVertex> getVertices() {
		return (Tuple<? extends SimSocialVertex, ? extends SimSocialVertex>) super.getVertices();
	}

	@Override
	public SimSocialVertex getOpposite(Vertex v) {
		return (SimSocialVertex) super.getOpposite(v);
	}

	public int getCreated() {
		return created;
	}
	
	public int getLastUsed() {
		return lastUsed;
	}
	
	public TIntArrayList getUsage() {
		return usage;
	}

	public void use(int iteration) {
		usage.add(iteration);
		lastUsed = iteration;
	}
}
