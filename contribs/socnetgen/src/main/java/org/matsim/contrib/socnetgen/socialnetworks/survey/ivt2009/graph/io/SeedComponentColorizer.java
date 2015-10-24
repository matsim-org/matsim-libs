/* *********************************************************************** *
 * project: org.matsim.*
 * SeedComponentColorizer.java
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
package org.matsim.contrib.socnetgen.socialnetworks.survey.ivt2009.graph.io;

import gnu.trove.TObjectDoubleHashMap;
import org.matsim.contrib.socnetgen.sna.graph.io.PajekColorizer;
import org.matsim.contrib.socnetgen.sna.graph.spatial.io.ColorUtils;
import org.matsim.contrib.socnetgen.sna.snowball.SampledEdge;
import org.matsim.contrib.socnetgen.sna.snowball.SampledGraph;
import org.matsim.contrib.socnetgen.sna.snowball.SampledVertex;
import org.matsim.contrib.socnetgen.sna.snowball.analysis.SnowballPartitions;

import java.awt.*;
import java.util.Set;

/**
 * @author illenberger
 *
 */
public class SeedComponentColorizer extends PajekColorizer<SampledVertex, SampledEdge> {

	private final TObjectDoubleHashMap<SampledVertex> values;
	
	public SeedComponentColorizer(SampledGraph graph) {
		Set<? extends SampledVertex> seeds = SnowballPartitions.createSampledPartition(graph.getVertices(), 0);
		
		values = new TObjectDoubleHashMap<SampledVertex>();
		double step = 1/(double)(seeds.size()+1);
		double val = step;
		
		for(SampledVertex v : seeds) {
			values.put(v, val);
			val += step;
		}
	}
	
	@Override
	public String getVertexFillColor(SampledVertex v) {
		if(!values.containsKey(v.getSeed()))
			System.err.println("seed not found");
		
		Color c = ColorUtils.getGRBColor(values.get(v.getSeed()));
		return "#" + Integer.toHexString(c.getRGB() & 0x00ffffff);
	}

	@Override
	public String getEdgeColor(SampledEdge e) {
		return getColor(-1);
	}

}
