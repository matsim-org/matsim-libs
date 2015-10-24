/* *********************************************************************** *
 * project: org.matsim.*
 * PajekWriter.java
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
package org.matsim.contrib.socnetgen.socialnetworks.survey.ivt2009.graph.io;

import org.matsim.contrib.socnetgen.sna.graph.Edge;
import org.matsim.contrib.socnetgen.sna.graph.Graph;
import org.matsim.contrib.socnetgen.sna.graph.Vertex;
import org.matsim.contrib.socnetgen.sna.snowball.SampledGraph;
import org.matsim.contrib.socnetgen.sna.snowball.SampledVertex;
import org.matsim.contrib.socnetgen.sna.snowball.SampledVertexDecorator;
import org.matsim.contrib.socnetgen.socialnetworks.graph.social.SocialVertex;
import org.matsim.contrib.socnetgen.socialnetworks.snowball2.social.SocialSampledGraphProjectionBuilder;
import org.matsim.contrib.socnetgen.socialnetworks.survey.ivt2009.analysis.SampledVertexFilter;
import org.matsim.contrib.socnetgen.socialnetworks.survey.ivt2009.graph.SocialSparseEdge;
import org.matsim.contrib.socnetgen.socialnetworks.survey.ivt2009.graph.SocialSparseGraph;
import org.matsim.contrib.socnetgen.socialnetworks.survey.ivt2009.graph.SocialSparseVertex;

import java.io.IOException;

/**
 * @author illenberger
 *
 */
public class PajekWriter extends org.matsim.contrib.socnetgen.socialnetworks.graph.io.PajekWriter<Graph, Vertex, Edge> {

	@Override
	protected String getVertexLabel(Vertex v) {
		SampledVertexDecorator<SocialVertex> vertex = (SampledVertexDecorator<SocialVertex>) v;
//		return vertex.getDelegate().getPerson().getId().toString();
		return String.valueOf(((SampledVertex)v).getIterationSampled());
	}

	public static void main(String args[]) throws IOException {
		Graph g = GraphReaderFacade.read("/Users/jillenberger/Work/work/socialnets/data/ivt2009/raw/09-2010/graph/noH/graph.graphml");
		PajekWriter writer = new PajekWriter();
		
		SocialSampledGraphProjectionBuilder<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> builder = new SocialSampledGraphProjectionBuilder<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge>();
		SampledVertexFilter filter = new SampledVertexFilter(builder);
		filter.apply((SampledGraph) g);
		writer.write(g, "/Users/jillenberger/Work/work/socialnets/data/ivt2009/raw/09-2010/graph/noH/graph.net");
	}
}
