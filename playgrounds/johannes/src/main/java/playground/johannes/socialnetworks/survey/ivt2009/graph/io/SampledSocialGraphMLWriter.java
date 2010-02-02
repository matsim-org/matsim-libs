/* *********************************************************************** *
 * project: org.matsim.*
 * SampledSocialGraphMLWriter.java
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
package playground.johannes.socialnetworks.survey.ivt2009.graph.io;

import java.io.IOException;
import java.util.List;

import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.snowball.SampledVertex;
import org.matsim.contrib.sna.snowball.io.SampledGraphML;
import org.matsim.core.utils.collections.Tuple;

import playground.johannes.socialnetworks.graph.social.io.SocialGraphMLWriter;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SampledSocialGraph;

/**
 * @author illenberger
 *
 */
public class SampledSocialGraphMLWriter extends SocialGraphMLWriter {

	@Override
	public void write(Graph graph, String filename) throws IOException {
		if(graph instanceof SampledSocialGraph)
			super.write(graph, filename);
		else
			throw new IllegalArgumentException("This is not a SampledSocialGraph!");
	}

	@Override
	protected List<Tuple<String, String>> getVertexAttributes(Vertex v) {
		List<Tuple<String, String>> attrs = super.getVertexAttributes(v);
		
		SampledGraphML.addSnowballAttributesData((SampledVertex) v, attrs);
		
		return attrs;
	}

}
