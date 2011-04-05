/* *********************************************************************** *
 * project: org.matsim.*
 * CachedAccessibility.java
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
package playground.johannes.socialnetworks.graph.spatial.analysis;

import gnu.trove.TObjectDoubleHashMap;

import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.sna.graph.Vertex;

import playground.johannes.socialnetworks.graph.analysis.CachedVertexProperty;

/**
 * @author illenberger
 *
 */
public class CachedAccessibility extends Accessibility {

	private CachedVertexProperty cache;
	
	public CachedAccessibility(Accessibility accessibility) {
		super(null);
		cache = new CachedVertexProperty(accessibility);
	}

	@Override
	public TObjectDoubleHashMap<Vertex> values(Set<? extends Vertex> vertices) {
		return cache.values(vertices);
	}

	@Override
	public DescriptiveStatistics statistics(Set<? extends Vertex> vertices) {
		return cache.statistics(vertices);
	}

}
