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
package playground.johannes.socialnetworks.survey.ivt2009.graph.io;

import java.io.IOException;

import playground.johannes.sna.graph.Edge;
import playground.johannes.sna.graph.Graph;
import playground.johannes.sna.graph.Vertex;
import playground.johannes.sna.snowball.SampledGraph;
import playground.johannes.sna.snowball.SampledVertex;
import playground.johannes.sna.snowball.SampledVertexDecorator;
import playground.johannes.socialnetworks.graph.social.SocialVertex;
import playground.johannes.socialnetworks.snowball2.social.SocialSampledGraphProjectionBuilder;
import playground.johannes.socialnetworks.survey.ivt2009.analysis.SampledVertexFilter;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseEdge;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraph;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseVertex;

/**
 * @author illenberger
 *
 */
public class PajekWriter extends playground.johannes.socialnetworks.graph.io.PajekWriter<Graph, Vertex, Edge> {

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
