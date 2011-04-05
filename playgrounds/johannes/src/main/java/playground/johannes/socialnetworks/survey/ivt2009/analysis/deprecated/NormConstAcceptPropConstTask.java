/* *********************************************************************** *
 * project: org.matsim.*
 * NormConstAcceptPropConstTask.java
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
package playground.johannes.socialnetworks.survey.ivt2009.analysis.deprecated;

import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TObjectDoubleHashMap;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.GraphAnalyzer;
import org.matsim.contrib.sna.graph.analysis.ModuleAnalyzerTask;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.graph.spatial.io.KMLIconVertexStyle;
import org.matsim.contrib.sna.graph.spatial.io.SpatialGraphKMLWriter;
import org.matsim.contrib.sna.graph.spatial.io.SpatialGraphMLReader;
import org.matsim.contrib.sna.math.DummyDiscretizer;
import org.matsim.contrib.sna.util.TXTWriter;

import playground.johannes.socialnetworks.gis.CartesianDistanceCalculator;
import playground.johannes.socialnetworks.gis.GravityCostFunction;
import playground.johannes.socialnetworks.graph.analysis.VertexPropertyCorrelation;
import playground.johannes.socialnetworks.graph.spatial.analysis.Accessibility;
import playground.johannes.socialnetworks.graph.spatial.analysis.LogAccessibility;
import playground.johannes.socialnetworks.graph.spatial.io.NumericAttributeColorizer;
import playground.johannes.socialnetworks.survey.ivt2009.analysis.AcceptPropConst;
import playground.johannes.socialnetworks.survey.ivt2009.analysis.KMLEgoPartition;

import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public class NormConstAcceptPropConstTask extends ModuleAnalyzerTask<Accessibility> {

	private Set<Point> destinations;
	
	public void setDestinations(Set<Point> destinations) {
		this.destinations = destinations;
	}
	
	@Override
	public void analyze(Graph g, Map<String, DescriptiveStatistics> statsMap) {
		if(outputDirectoryNotNull()) {
			SpatialGraph graph = (SpatialGraph) g;
			Accessibility norm = new Accessibility(new GravityCostFunction(1.6, 0));
			norm.setTargets(destinations);
			TObjectDoubleHashMap<Vertex> yVals = norm.values(g.getVertices());
			
			AcceptPropConst acceptProp = new AcceptPropConst();
//			MeanDistanceAll meanDist = new ObservedMeanDistanceAll();
//			meanDist.setDestinations(destinations);
//			ObservedAccessibility acces = new ObservedAccessibility();
			LogAccessibility acces = new LogAccessibility();
			TObjectDoubleHashMap<SpatialVertex> values = acces.values(graph.getVertices(), new GravityCostFunction(1.6, 0, new CartesianDistanceCalculator()), destinations);
			acceptProp.setPartitionAttributes(yVals);
			acceptProp.setDestinations(destinations);
			TObjectDoubleHashMap<Vertex> xVals = acceptProp.values(g.getVertices());
			
			TDoubleDoubleHashMap cor = VertexPropertyCorrelation.mean(values, xVals, new DummyDiscretizer());
			try {
				TXTWriter.writeMap(cor, "c_i", "norm", getOutputDirectory() + "norm_acceptProp.txt");
				
				SpatialGraphKMLWriter writer = new SpatialGraphKMLWriter();
				
				NumericAttributeColorizer colorizer = new NumericAttributeColorizer(xVals);
				colorizer.setLogscale(false);
				KMLIconVertexStyle style = new KMLIconVertexStyle(graph);
				style.setVertexColorizer(colorizer);
				writer.setKmlVertexStyle(style);
				writer.setDrawEdges(false);
				writer.addKMZWriterListener(style);
				writer.setKmlPartitition(new KMLEgoPartition());
				writer.write(graph, getOutputDirectory() + "graph.aPropConst.kmz");
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	public static void main(String args[]) throws IOException {
		SpatialGraphMLReader reader = new SpatialGraphMLReader();
		SpatialGraph graph = reader.readGraph("/Volumes/cluster.math.tu-berlin.de/net/ils/jillenberger/socialnets/mcmc/runs/run298/output/15000000000/graph.graphml");
		
		Set<Point> destinations = new HashSet<Point>();
		for(SpatialVertex v : graph.getVertices()) {
			destinations.add(v.getPoint());
		}
		NormConstAcceptPropConstTask task = new NormConstAcceptPropConstTask();
		task.destinations = destinations;
		GraphAnalyzer.analyze(graph, task, "/Volumes/cluster.math.tu-berlin.de/net/ils/jillenberger/socialnets/mcmc/runs/run298/output/15000000000/spatial/");
	}

}
