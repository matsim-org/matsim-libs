/* *********************************************************************** *
 * project: org.matsim.*
 * SnowballMovie.java
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.opengis.kml._2.FolderType;

import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.spatial.SpatialEdge;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.graph.spatial.io.KMLPartitions;
import org.matsim.contrib.sna.graph.spatial.io.SpatialGraphKMLWriter;
import org.matsim.contrib.sna.graph.spatial.io.SpatialGraphMLReader;

import playground.johannes.socialnetworks.snowball2.sim.RandomResponse;
import playground.johannes.socialnetworks.snowball2.sim.RandomSeedGenerator;
import playground.johannes.socialnetworks.snowball2.sim.Sampler;
import playground.johannes.socialnetworks.snowball2.spatial.io.KMLTimeSpan;
import playground.johannes.socialnetworks.snowball2.spatial.io.TimeTagger2;

/**
 * @author illenberger
 *
 */
public class SnowballMovie {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		/*
		 * Load graph
		 */
		SpatialGraphMLReader reader = new SpatialGraphMLReader();
		SpatialGraph graph = reader.readGraph(args[0]);
		/*
		 * Run sampler
		 */
		Sampler<SpatialGraph, SpatialVertex, SpatialEdge> sampler = new Sampler<SpatialGraph, SpatialVertex, SpatialEdge>();
		sampler.setResponseGenerator(new RandomResponse(1, 0));
		sampler.setSeedGenerator(new RandomSeedGenerator(10, 0));
//		final IterationTimeTagger tagger = new IterationTimeTagger();
//		sampler.setListener(tagger);
		
		sampler.run(graph);
		
		/*
		 * Write KML
		 */
		final TimeTagger2 tagger = new TimeTagger2(sampler.getSampledGraph());
		SpatialGraphKMLWriter writer = new SpatialGraphKMLWriter();
		KMLPartitions partition = new KMLPartitions() {
			
			@Override
			public List<Set<? extends SpatialVertex>> getPartitions(SpatialGraph graph) {
				ArrayList list = new ArrayList();
				Set set = new HashSet();
				for(Object obj : tagger.getTimeTags().keySet()) {
					if(obj instanceof Vertex) {
						set.add(obj);
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
		KMLTimeSpan timeSpan = new KMLTimeSpan(tagger.getTimeTags());
		writer.setKmlVertexDetail(timeSpan);
		writer.setKmlEdgeDetail(timeSpan);
//		KMLIconVertexStyle style = new KMLIconVertexStyle(graph);
//		style.setVertexColorizer(new SnowballColorizer(graph.getVertices()));
//		writer.setKmlVertexStyle(style);
//		writer.addKMZWriterListener(style);
//		writer.setDrawEdges(false);
		writer.write(graph, args[1]);
	}

//	private static class TimeSpanVertex implements KMLObjectDetail<SpatialVertex> {
//
//		private ObjectFactory factory = new ObjectFactory();
//		
//		private TObjectIntHashMap<Vertex> timeCodes;
//		
//		@Override
//		public void addDetail(PlacemarkType kmlPlacemark, SpatialVertex object) {
//			TimeSpanType tType = factory.createTimeSpanType();
//			int code = timeCodes.get(object);
//			if(code == 0)
//				code = 10000;
//			String timeStamp = String.valueOf(code);
//			tType.setBegin(timeStamp);
////			tType.setBegin(String.valueOf(((SampledVertex)vertex).getIterationDetected()+2000));
//			kmlPlacemark.setAbstractTimePrimitiveGroup(factory.createTimeSpan(tType));	
//		}
//		
//	}
//	
//	private static class TimeSpanEdge implements KMLObjectDetail<SpatialEdge> {
//
//		private ObjectFactory factory = new ObjectFactory();
//		
//		private TObjectIntHashMap<Edge> timeCodes;
//		
//		@Override
//		public void addDetail(PlacemarkType kmlPlacemark, SpatialEdge object) {
//			TimeSpanType tType = factory.createTimeSpanType();
//			int code = timeCodes.get(object);
//			if(code == 0)
//				code = 10000;
//			String timeStamp = String.valueOf(code);
//			tType.setBegin(timeStamp);
////			tType.setBegin(String.valueOf(((SampledVertex)vertex).getIterationDetected()+2000));
//			kmlPlacemark.setAbstractTimePrimitiveGroup(factory.createTimeSpan(tType));	
//		}
//		
//	}
//	
//	private static class TimeCodeTagger implements SamplerListener {
//		
//		private TObjectIntHashMap<Vertex> timeCodesVertex = new TObjectIntHashMap<Vertex>();
//		
//		private TObjectIntHashMap<Edge> timeCodesEdge = new TObjectIntHashMap<Edge>();
//
//		private int timeCode;
//		
//		@Override
//		public boolean afterSampling(Sampler<?, ?, ?> sampler, SampledVertexDecorator<?> vertex) {
//			SampledEdgeDecorator<?> e;
//			if(vertex.getNeighbours().size() > 0) {
//				e = vertex.getEdges().iterator().next();
//				timeCodesEdge.put(e.getDelegate(), timeCode);
//			}
//			timeCodesVertex.put(vertex.getDelegate(), timeCode);
//			timeCode++;
//			return true;
//		}
//
//		@Override
//		public boolean beforeSampling(Sampler<?, ?, ?> sampler, SampledVertexDecorator<?> vertex) {
//			return true;
//		}
//
//		@Override
//		public void endSampling(Sampler<?, ?, ?> sampler) {
//		}
//		
//	}
//	
//	private static class SeedGenerator implements VertexPartition {
//
//		private Set<? extends SocialSampledVertexDecorator<SocialSparseVertex>> seeds;
//		
//		@Override
//		public <V extends Vertex> Set<V> getPartition(Set<V> vertices) {
////			HashSet<V> delegates = new HashSet<V>();
////			for(SocialSampledVertexDecorator<?> v : seeds)
////				delegates.add((V) v.getDelegate());
////			
//			return (Set<V>) seeds;
//		}
//		
//	}
}
