/* *********************************************************************** *
 * project: org.matsim.*
 * SnowballSample.java
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
package playground.johannes.studies.mcmc;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.geotools.feature.Feature;
import org.matsim.contrib.sna.graph.analysis.DegreeTask;
import org.matsim.contrib.sna.graph.analysis.FixedSizeRandomPartition;
import org.matsim.contrib.sna.graph.analysis.GraphAnalyzer;
import org.matsim.contrib.sna.graph.analysis.RandomPartition;
import org.matsim.contrib.sna.graph.analysis.VertexFilter;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseEdge;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseVertex;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.graph.spatial.io.SpatialGraphKMLWriter;
import org.matsim.contrib.sna.graph.spatial.io.SpatialGraphMLReader;
import org.matsim.contrib.sna.snowball.SampledVertexDecorator;
import org.matsim.contrib.sna.snowball.analysis.EstimatedDegree;
import org.matsim.contrib.sna.snowball.analysis.SimplePiEstimator;
import org.matsim.contrib.sna.snowball.sim.Sampler;
import org.matsim.contrib.sna.snowball.sim.SamplerListener;

import playground.johannes.socialnetworks.gis.CartesianDistanceCalculator;
import playground.johannes.socialnetworks.gis.GravityCostFunction;
import playground.johannes.socialnetworks.gis.io.FeatureSHP;
import playground.johannes.socialnetworks.graph.analysis.AnalyzerTaskArray;
import playground.johannes.socialnetworks.graph.analysis.AnalyzerTaskComposite;
import playground.johannes.socialnetworks.graph.analysis.TopologyAnalyzerTask;
import playground.johannes.socialnetworks.graph.spatial.analysis.AcceptanceProbabilityTask;
import playground.johannes.socialnetworks.graph.spatial.analysis.AcceptancePropaCategoryTask;
import playground.johannes.socialnetworks.graph.spatial.analysis.Accessibility;
import playground.johannes.socialnetworks.graph.spatial.analysis.CachedAccessibility;
import playground.johannes.socialnetworks.graph.spatial.analysis.DegreeAccessibilityTask;
import playground.johannes.socialnetworks.graph.spatial.analysis.EdgeLengthAccessibilityTask;
import playground.johannes.socialnetworks.graph.spatial.analysis.SpatialAnalyzerTask;
import playground.johannes.socialnetworks.graph.spatial.analysis.TransitivityAccessibilityTask;
import playground.johannes.socialnetworks.snowball2.analysis.WSMStatsFactory;
import playground.johannes.socialnetworks.snowball2.analysis.WaveSizeTask;
import playground.johannes.socialnetworks.snowball2.spatial.SpatialSampledGraphProjectionBuilder;
import playground.johannes.socialnetworks.snowball2.spatial.analysis.ObservedAccessibility;
import playground.johannes.socialnetworks.survey.ivt2009.analysis.ObservedAcceptanceProbability;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public class SnowballSample {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		SpatialGraphMLReader reader = new SpatialGraphMLReader();
		SpatialSparseGraph graph = reader.readGraph("/Users/jillenberger/Work/socialnets/mcmc/snowball/graph.363332.graphml");
		
		Set<Feature> features = FeatureSHP.readFeatures("/Users/jillenberger/Work/socialnets/data/schweiz/complete/zones/Kanton.shp");
		Geometry geometry = features.iterator().next().getDefaultGeometry();
		
		Sampler<SpatialSparseGraph, SpatialSparseVertex, SpatialSparseEdge> sampler = new Sampler<SpatialSparseGraph, SpatialSparseVertex, SpatialSparseEdge>();
		
		sampler.setBuilder(new SpatialSampledGraphProjectionBuilder<SpatialSparseGraph, SpatialSparseVertex, SpatialSparseEdge>());
		sampler.setSeedGenerator(new SeedGenerator(geometry));
		sampler.setResponseGenerator(new RandomPartition<SpatialSparseVertex>(0.2));
		sampler.setListener(new SampleSizeListener());
		
		sampler.run(graph);
		
		AnalyzerTaskArray array = new AnalyzerTaskArray();
		array.addAnalyzerTask(new TopologyAnalyzerTask(), "topo");
		AnalyzerTaskComposite spatialTask = new AnalyzerTaskComposite();
		spatialTask.addTask(new SpatialAnalyzerTask());
		
		Set<Point> choiceSet = new HashSet<Point>();
//		SpatialSparseGraph graph2 = new Population2SpatialGraph(CRSUtils.getCRS(21781)).read("");
		for(SpatialVertex v : graph.getVertices()) {	
			choiceSet.add(v.getPoint());
		}
//		AcceptanceProbabilityTask accTask = new AcceptanceProbabilityTask(choiceSet);
//		accTask.setModule(new ObservedAcceptanceProbability());
//		spatialTask.addTask(accTask);
		Accessibility access = new ObservedAccessibility(new GravityCostFunction(1.4, 0, new CartesianDistanceCalculator()));
		access.setTargets(choiceSet);
		CachedAccessibility cachedAccess = new CachedAccessibility(access);
		spatialTask.addTask(new DegreeAccessibilityTask(cachedAccess));
		spatialTask.addTask(new EdgeLengthAccessibilityTask(cachedAccess));
		spatialTask.addTask(new TransitivityAccessibilityTask(cachedAccess));
		
		AcceptancePropaCategoryTask t = new AcceptancePropaCategoryTask(cachedAccess);
//		t.setBoundary(boundary);
		t.setDestinations(choiceSet);
		spatialTask.addTask(t);
		array.addAnalyzerTask(spatialTask, "spatial");
		
		AnalyzerTaskComposite composite = new AnalyzerTaskComposite();
		DegreeTask kTask = new DegreeTask();
		SimplePiEstimator estim = new SimplePiEstimator(graph.getVertices().size());
		kTask.setModule(new EstimatedDegree(estim, new WSMStatsFactory()));
		composite.addTask(kTask);
		composite.addTask(new WaveSizeTask());
		array.addAnalyzerTask(composite, "snowball");
		estim.update(sampler.getSampledGraph());
		GraphAnalyzer.analyze(sampler.getSampledGraph(), array, "/Users/jillenberger/Work/socialnets/mcmc/snowball/");
		
		SpatialGraphKMLWriter writer = new SpatialGraphKMLWriter();
		writer.setDrawEdges(false);
		writer.write((SpatialGraph) sampler.getSampledGraph(), "/Users/jillenberger/Work/socialnets/mcmc/snowball/graph.kmz");
	}

	private static class SeedGenerator implements VertexFilter<SpatialSparseVertex> {

		private Geometry geometry;
		
		public SeedGenerator(Geometry geometry) {
			this.geometry = geometry;
		}
		
		@Override
		public Set<SpatialSparseVertex> apply(Set<SpatialSparseVertex> vertices) {
			Set<SpatialSparseVertex> zrh = new HashSet<SpatialSparseVertex>();
			
			for(SpatialSparseVertex vertex : vertices) {
				if(geometry.contains(vertex.getPoint())) {
					zrh.add(vertex);
				}
			}
			
			VertexFilter<SpatialSparseVertex> filter = new FixedSizeRandomPartition<SpatialSparseVertex>(40);
			
			return filter.apply(zrh);
		}
		
	}
	
	private static class SampleSizeListener implements SamplerListener {

		@Override
		public boolean beforeSampling(Sampler<?, ?, ?> sampler, SampledVertexDecorator<?> vertex) {
			if(sampler.getSampledGraph().getVertices().size() > 7000)
//			if(sampler.getNumSampledVertices() > 200)
				return false;
			else
				return true;
		}

		@Override
		public boolean afterSampling(Sampler<?, ?, ?> sampler, SampledVertexDecorator<?> vertex) {
			return true;
		}

		@Override
		public void endSampling(Sampler<?, ?, ?> sampler) {
		}
		
	}
}
