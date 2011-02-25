/* *********************************************************************** *
 * project: org.matsim.*
 * EdgeProperty.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.graph.analysis;

import gnu.trove.TObjectDoubleHashMap;

import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.sna.graph.Edge;

/**
 * @author illenberger
 *
 */
public interface EdgeProperty {

	public TObjectDoubleHashMap<Edge> values(Set<? extends Edge> edges);
		
	public DescriptiveStatistics statistics(Set<? extends Edge> edges);
	
}
