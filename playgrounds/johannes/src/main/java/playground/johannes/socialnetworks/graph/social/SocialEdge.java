/* *********************************************************************** *
 * project: org.matsim.*
 * SocialEdge.java
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
package playground.johannes.socialnetworks.graph.social;

import org.matsim.core.utils.collections.Tuple;

import playground.johannes.sna.graph.Vertex;
import playground.johannes.sna.graph.spatial.SpatialEdge;

/**
 * @author illenberger
 *
 */
public interface SocialEdge extends SpatialEdge {

	public SocialVertex getOpposite(Vertex vertex);
	
	public Tuple<? extends SocialVertex, ? extends SocialVertex> getVertices();
	
	/*
	 * subject to change!
	 */
	public double getFrequency();
	
	public String getType();
}
