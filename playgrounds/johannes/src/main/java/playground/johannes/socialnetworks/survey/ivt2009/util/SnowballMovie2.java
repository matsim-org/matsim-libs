/* *********************************************************************** *
 * project: org.matsim.*
 * SnowballMovie2.java
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
package playground.johannes.socialnetworks.survey.ivt2009.util;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.opengis.kml._2.FolderType;

import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.VertexDecorator;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.graph.spatial.io.KMLIconVertexStyle;
import org.matsim.contrib.sna.graph.spatial.io.KMLObjectDetailComposite;
import org.matsim.contrib.sna.graph.spatial.io.KMLPartitions;
import org.matsim.contrib.sna.graph.spatial.io.SpatialGraphKMLWriter;
import org.matsim.contrib.sna.snowball.SampledVertex;

import playground.johannes.socialnetworks.snowball2.SampledGraphProjection;
import playground.johannes.socialnetworks.snowball2.SampledVertexDecorator;
import playground.johannes.socialnetworks.snowball2.io.SampledGraphProjMLReader;
import playground.johannes.socialnetworks.snowball2.io.SeedColorizer;
import playground.johannes.socialnetworks.snowball2.sim.Sampler;
import playground.johannes.socialnetworks.snowball2.sim.SamplerListener;
import playground.johannes.socialnetworks.snowball2.sim.SamplerListenerComposite;
import playground.johannes.socialnetworks.snowball2.sim.VertexPartition;
import playground.johannes.socialnetworks.snowball2.social.SocialSampledEdgeDecorator;
import playground.johannes.socialnetworks.snowball2.social.SocialSampledGraphProjection;
import playground.johannes.socialnetworks.snowball2.social.SocialSampledGraphProjectionBuilder;
import playground.johannes.socialnetworks.snowball2.social.SocialSampledVertexDecorator;
import playground.johannes.socialnetworks.snowball2.spatial.io.KMLTimeSpan;
import playground.johannes.socialnetworks.snowball2.spatial.io.TimeTagger;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseEdge;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraph;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseVertex;
import playground.johannes.socialnetworks.survey.ivt2009.graph.io.KMLNeighbors;
import playground.johannes.socialnetworks.survey.ivt2009.graph.io.SocialSparseGraphMLReader;

/**
 * @author illenberger
 *
 */
public class SnowballMovie2 {

	private static SocialSampledGraphProjection<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> graph ;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		/*
		 * Load graph
		 */
		SampledGraphProjMLReader<SocialSparseGraph,SocialSparseVertex,SocialSparseEdge> reader =
			new SampledGraphProjMLReader<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge>(new SocialSparseGraphMLReader());
		
		reader.setGraphProjectionBuilder(new SocialSampledGraphProjectionBuilder<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge>());
		
		graph = (SocialSampledGraphProjection<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge>) reader.readGraph(args[0]);
		
//		for(SocialVertex v : graph.getVertices()) {
//			if(v.getPerson().getId().toString().equalsIgnoreCase("10434"))
//				System.err.println();
//		}
		/*
		 * Find seed vertices
		 */
		final SocialSampledVertexDecorator<SocialSparseVertex> seed1 = getSeed("207");
		final SocialSampledVertexDecorator<SocialSparseVertex> seed2 = getSeed("845");
		
		VertexPartition seedGenerator = new VertexPartition() {
			
			@Override
			public <V extends Vertex> Set<V> partition(Set<V> vertices) {
				Set<V> seeds = new java.util.HashSet<V>();
				seeds.add((V) seed1);
				seeds.add((V) seed2);
				return seeds;
			}
		};
		/*
		 * Prepare sampler
		 */
		Sampler<SocialSampledGraphProjection<?, ?, ?>, SocialSampledVertexDecorator<?>, SocialSampledEdgeDecorator<?>> sampler =
			new Sampler<SocialSampledGraphProjection<?,?,?>, SocialSampledVertexDecorator<?>, SocialSampledEdgeDecorator<?>>();
		sampler.setSeedGenerator(seedGenerator);
		final TimeTagger timeTagger = new TimeTagger();
		SamplerListenerComposite composite = new SamplerListenerComposite();
		composite.addComponent(timeTagger);
		composite.addComponent(new ConnectionListener());
		sampler.setListener(composite);
		sampler.run(graph);
		/*
		 * Write KML file
		 */
		SpatialGraphKMLWriter writer = new SpatialGraphKMLWriter();
		
		KMLPartitions partition = new KMLPartitions() {
			
			@Override
			public List<Set<? extends SpatialVertex>> getPartitions(SpatialGraph graph) {
				ArrayList list = new ArrayList();
				Set set = new HashSet();
				for(Object obj : timeTagger.getTimeTags().keySet()) {
					if(obj instanceof Vertex) {
						set.add(obj);
//						if(((SocialVertex) obj).getPerson().getId().toString().equalsIgnoreCase("10434"))
//							System.err.println();
					}
				}
				list.add(set);
				return list;
			}
			
			@Override
			public void addDetail(FolderType kmlFolder, Set<? extends SpatialVertex> partition) {
				// TODO Auto-generated method stub
				
			}
		};
		
		writer.setKmlPartitition(partition);
		
		KMLObjectDetailComposite kmlComp = new KMLObjectDetailComposite();
		timeTagger.getTimeTags().put(seed1, "0");
		timeTagger.getTimeTags().put(seed2, "0");
		KMLTimeSpan timeSpan = new KMLTimeSpan(timeTagger.getTimeTags());
		kmlComp.addObjectDetail(timeSpan);
		
		
//		kmlComp.addObjectDetail(new KMLVertexId());
		kmlComp.addObjectDetail(new KMLNeighbors());
		
		writer.setKmlVertexDetail(kmlComp);
		writer.setKmlEdgeDetail(timeSpan);
		
		
		
		KMLIconVertexStyle style = new KMLIconVertexStyle(graph);
		SampledVertex seedProj1 = (SampledVertex) sampler.getSampledGraph().getVertex(seed1);
		SampledVertex seedProj2 = (SampledVertex) sampler.getSampledGraph().getVertex(seed2);
		Set seedProjections = new java.util.HashSet();
		seedProjections.add(seedProj1);
		seedProjections.add(seedProj2);
		
		style.setVertexColorizer(new SeedColorizerWrapper(sampler.getSampledGraph(), seedProjections));
		writer.setKmlVertexStyle(style);
		writer.addKMZWriterListener(style);
		
		writer.write(graph, args[1]);
	}

	private static SocialSampledVertexDecorator<SocialSparseVertex> getSeed(String id) {
		for(SocialSampledVertexDecorator<SocialSparseVertex> vertex : graph.getVertices()) {
			if(vertex.getPerson().getId().toString().equalsIgnoreCase(id)) {
				return vertex;
			}
		}
		
		return null;
	}
	
	private static class SeedColorizerWrapper<V extends SampledVertex> extends SeedColorizer {

		private SampledGraphProjection<?, V, ?> graph;
		
		public SeedColorizerWrapper(SampledGraphProjection<?, ?, ?> graph, Set seeds) {
			super(seeds);
			this.graph = (SampledGraphProjection<?, V, ?>) graph;
		}

		@Override
		public Color getColor(Object object) {
			VertexDecorator<V> v = graph.getVertex((V) object);
			if(v != null)
				return super.getColor(v);
			else
				return Color.BLACK;
		}
		
	}
	
	private static class ConnectionListener implements SamplerListener {

		/* (non-Javadoc)
		 * @see playground.johannes.socialnetworks.snowball2.sim.SamplerListener#afterSampling(playground.johannes.socialnetworks.snowball2.sim.Sampler, playground.johannes.socialnetworks.snowball2.SampledVertexDecorator)
		 */
		@Override
		public boolean afterSampling(Sampler<?, ?, ?> sampler, SampledVertexDecorator<?> vertex) {
			for(Vertex neighbor : vertex.getNeighbours()) {
				if(((SampledVertex)neighbor).getSeed() != vertex.getSeed()) {
					return false;
				}
			}
			return true;
		}

		/* (non-Javadoc)
		 * @see playground.johannes.socialnetworks.snowball2.sim.SamplerListener#beforeSampling(playground.johannes.socialnetworks.snowball2.sim.Sampler, playground.johannes.socialnetworks.snowball2.SampledVertexDecorator)
		 */
		@Override
		public boolean beforeSampling(Sampler<?, ?, ?> sampler, SampledVertexDecorator<?> vertex) {
			// TODO Auto-generated method stub
			return true;
		}

		/* (non-Javadoc)
		 * @see playground.johannes.socialnetworks.snowball2.sim.SamplerListener#endSampling(playground.johannes.socialnetworks.snowball2.sim.Sampler)
		 */
		@Override
		public void endSampling(Sampler<?, ?, ?> sampler) {
			// TODO Auto-generated method stub
			
		}
		
	}
}
