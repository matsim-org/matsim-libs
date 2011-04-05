/* *********************************************************************** *
 * project: org.matsim.*
 * CachedVertexProperty.java
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
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.VertexProperty;

/**
 * @author illenberger
 *
 */
public class CachedVertexProperty implements VertexProperty {

	private VertexProperty delegate;
	
	private TObjectDoubleHashMap<Vertex> values;
	
	private DescriptiveStatistics statistics;
	
	public CachedVertexProperty(VertexProperty delegate) {
		this.delegate = delegate;
	}
	
	@Override
	public TObjectDoubleHashMap<Vertex> values(Set<? extends Vertex> vertices) {
		if(values == null)
			values = delegate.values(vertices);
		
		return values;
	}

	@Override
	public DescriptiveStatistics statistics(Set<? extends Vertex> vertices) {
		if(statistics == null)
			statistics = delegate.statistics(vertices);
		
		return statistics;
	}

}
