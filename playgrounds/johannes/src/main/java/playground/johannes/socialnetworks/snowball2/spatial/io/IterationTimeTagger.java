/* *********************************************************************** *
 * project: org.matsim.*
 * TimeTagger.java
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
package playground.johannes.socialnetworks.snowball2.spatial.io;

import java.util.HashMap;
import java.util.Map;

import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.snowball.SampledEdgeDecorator;
import org.matsim.contrib.sna.snowball.SampledVertexDecorator;
import org.matsim.contrib.sna.snowball.sim.Sampler;
import org.matsim.contrib.sna.snowball.sim.SamplerListener;


/**
 * @author illenberger
 *
 */
public class IterationTimeTagger implements SamplerListener {
	
	private Map<Object, String> timeTags = new HashMap<Object, String>();

//	private int timeCode = 1900;
	
	public Map<?, String> getTimeTags() {
		return timeTags;
	}
	
	@Override
	public boolean afterSampling(Sampler<?, ?, ?> sampler, SampledVertexDecorator<?> vertex) {
		if(vertex.isSampled()) {
		String time = String.valueOf(vertex.getIterationSampled());
		for(SampledEdgeDecorator<?> edge : vertex.getEdges()) {
			if(!timeTags.containsKey(edge.getDelegate()))
				timeTags.put(edge.getDelegate(), time);
			
			Vertex v = edge.getOpposite(vertex).getDelegate();
			if(!timeTags.containsKey(v))
				timeTags.put(v, time);
			
//			timeCode++;
		}
		}
		return true;
	}

	@Override
	public boolean beforeSampling(Sampler<?, ?, ?> sampler, SampledVertexDecorator<?> vertex) {
		return true;
	}

	@Override
	public void endSampling(Sampler<?, ?, ?> sampler) {
	}

}
