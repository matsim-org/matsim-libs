/* *********************************************************************** *
 * project: org.matsim.*
 * SampledVertex.java
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

import org.matsim.contrib.sna.graph.Vertex;


/**
 * @author illenberger
 *
 */
public interface SampledVertex extends Vertex {

	/*
	 * Due to a bug in Sun's java compiler the return type of this method cannot
	 * be generic, since otherwise multiple inheritance (e.g. in
	 * SampledSpatialGraph) would result in a compile error. See also
	 * http://bugs.sun.com/view_bug.do;jsessionid=3cb252856515e1983e4affdf768e?bug_id=6294779
	 */
//	public List getEdges();
	
	/*
	 * Due to a bug in Sun's java compiler the return type of this method cannot
	 * be generic, since otherwise multiple inheritance (e.g. in
	 * SampledSpatialGraph) would result in a compile error. See also
	 * http://bugs.sun.com/view_bug.do;jsessionid=3cb252856515e1983e4affdf768e?bug_id=6294779
	 */
//	public List getNeighbours();
	
	public void detect(int iteration);
	
	public int getIterationDetected();
	
	public boolean isDetected();
	
	public void sample(int iteration);
	
	public int getIterationSampled();
	
	public boolean isSampled();
	
}
