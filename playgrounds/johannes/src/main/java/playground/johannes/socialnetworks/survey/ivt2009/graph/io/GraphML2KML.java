/* *********************************************************************** *
 * project: org.matsim.*
 * GraphML2KML.java
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.opengis.kml._2.FolderType;

import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.graph.spatial.io.KMLIconVertexStyle;
import org.matsim.contrib.sna.graph.spatial.io.KMLPartitions;
import org.matsim.contrib.sna.graph.spatial.io.SpatialGraphKMLWriter;
import org.matsim.contrib.sna.graph.spatial.io.VertexDegreeColorizer;
import org.matsim.contrib.sna.snowball.SampledVertex;
import org.matsim.contrib.sna.snowball.analysis.SnowballPartitions;

import playground.johannes.socialnetworks.snowball2.io.SampledGraphProjMLReader;
import playground.johannes.socialnetworks.snowball2.social.SocialSampledGraphProjection;
import playground.johannes.socialnetworks.snowball2.social.SocialSampledGraphProjectionBuilder;
import playground.johannes.socialnetworks.survey.ivt2009.analysis.SampledVertexFilter;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseEdge;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraph;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseVertex;

/**
 * @author illenberger
 *
 */
public class GraphML2KML {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
//		SpatialGraphMLReader reader = new SpatialGraphMLReader();
//		SpatialGraph graph = reader.readGraph(args[0]);
		
		SocialSampledGraphProjectionBuilder<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> builder = new SocialSampledGraphProjectionBuilder<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge>();
		SampledGraphProjMLReader<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> reader = new SampledGraphProjMLReader<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge>(new SocialSparseGraphMLReader());
		reader.setGraphProjectionBuilder(builder);
		SocialSampledGraphProjection<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> graph = (SocialSampledGraphProjection<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge>) reader.readGraph(args[0]);
		
		SampledVertexFilter filter = new SampledVertexFilter(builder);
		filter.apply(graph);
		builder.synchronize(graph);
		
		
		SpatialGraphKMLWriter writer = new SpatialGraphKMLWriter();
//		writer.setKmlPartitition(new SampledPartition());
		writer.setDrawEdges(false);
		KMLIconVertexStyle style = new KMLIconVertexStyle(graph);
		VertexDegreeColorizer colorizer = new VertexDegreeColorizer(graph);
		colorizer.setLogscale(false);
		style.setVertexColorizer(colorizer);
		writer.setKmlVertexStyle(style);
		writer.addKMZWriterListener(style);
		writer.write(graph, args[1]);
	}

	public static class SampledPartition implements KMLPartitions {

		/* (non-Javadoc)
		 * @see org.matsim.contrib.sna.graph.spatial.io.KMLPartitions#addDetail(net.opengis.kml._2.FolderType, java.util.Set)
		 */
		@Override
		public void addDetail(FolderType kmlFolder, Set<? extends SpatialVertex> partition) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see org.matsim.contrib.sna.graph.spatial.io.KMLPartitions#getPartitions(org.matsim.contrib.sna.graph.spatial.SpatialGraph)
		 */
		@Override
		public List<Set<? extends SpatialVertex>> getPartitions(SpatialGraph g) {
			SocialSampledGraphProjection<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> graph = (SocialSampledGraphProjection<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge>) g;
			/*
			 * generics do not work on hudson for some reason...
			 */
			List list = new ArrayList(1);
			Set set = SnowballPartitions.createSampledPartition(graph.getVertices());
			list.add(set);
			return list;
		}
		
	}
}
