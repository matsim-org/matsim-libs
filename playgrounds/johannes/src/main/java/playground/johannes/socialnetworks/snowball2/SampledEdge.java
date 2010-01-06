/* *********************************************************************** *
 * project: org.matsim.*
 * SampledEdge.java
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
package playground.johannes.socialnetworks.snowball2;

import org.matsim.contrib.sna.graph.Edge;

/**
 * @author illenberger
 *
 */
public interface SampledEdge extends Edge {
	
	/*
	 * Due to a bug in Sun's java compiler the return type of this method cannot
	 * be generic, since otherwise multiple inheritance (e.g. in
	 * SampledSpatialGraph) would result in a compile error. See also
	 * http://bugs.sun.com/view_bug.do;jsessionid=3cb252856515e1983e4affdf768e?bug_id=6294779
	 */
//	public Tuple<? extends SampledVertex, ? extends SampledVertex> getVertices();
	
	/*
	 * Due to a bug in Sun's java compiler the return type of this method cannot
	 * be generic, since otherwise multiple inheritance (e.g. in
	 * SampledSpatialGraph) would result in a compile error. See also
	 * http://bugs.sun.com/view_bug.do;jsessionid=3cb252856515e1983e4affdf768e?bug_id=6294779
	 */
//	public SampledVertex getOpposite(Vertex v);

}
