/* *********************************************************************** *
 * project: org.matsim.*
 * NaiveGraphGenerator.java
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
package playground.johannes.socialnetworks.graph.spatial.generators;

import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.gis.CRSUtils;
import org.matsim.contrib.sna.graph.analysis.GraphAnalyzer;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseGraphBuilder;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseVertex;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.graph.spatial.io.KMLVertexDescriptor;
import org.matsim.contrib.sna.graph.spatial.io.SpatialGraphKMLWriter;

import playground.johannes.socialnetworks.gis.BeelineCostFunction;
import playground.johannes.socialnetworks.gis.CartesianDistanceCalculator;
import playground.johannes.socialnetworks.gis.DistanceCalculator;
import playground.johannes.socialnetworks.graph.spatial.analysis.Accessability;
import playground.johannes.socialnetworks.graph.spatial.analysis.StandardAnalyzerTask;
import playground.johannes.socialnetworks.graph.spatial.io.Population2SpatialGraph;
import playground.johannes.socialnetworks.statistics.Discretizer;
import playground.johannes.socialnetworks.statistics.LinearDiscretizer;

import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public class NaiveGraphGenerator {

	private static final Logger logger = Logger.getLogger(NaiveGraphGenerator.class);
	
	private DistanceCalculator distanceCalculator = new CartesianDistanceCalculator();
	
	private Discretizer discretizer = new LinearDiscretizer(1000);
	
	private final double scaleConst = 0.1;
	
	private final Random random = new Random(0);
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String popFile = "/Users/jillenberger/Work/work/socialnets/data/schweiz/complete/plans/plans.0.001.xml";
		String graphFile = "/Users/jillenberger/Work/work/socialnets/mcmc/output/graph.kmz";
		
		Population2SpatialGraph reader = new Population2SpatialGraph(CRSUtils.getCRS(21781));
		SpatialSparseGraph graph = reader.read(popFile);
		
		NaiveGraphGenerator generator = new NaiveGraphGenerator();
		TObjectDoubleHashMap<SpatialVertex> gammas = generator.calculateGammas(graph, 1.0, 1.6);
		logger.info("Connecting vertices...");
		generator.connect(graph, new SpatialSparseGraphBuilder(CRSUtils.getCRS(21781)), gammas);
		logger.info("Done.");
		
		SpatialGraphKMLWriter writer = new SpatialGraphKMLWriter();
		writer.setDrawEdges(false);
		writer.setKmlVertexDetail(new KMLVertexDescriptor(graph));
		writer.write(graph, graphFile);
		
		StandardAnalyzerTask task = new StandardAnalyzerTask();
		task.setOutputDirectoy("/Users/jillenberger/Work/work/socialnets/mcmc/output/");
		GraphAnalyzer.analyze(graph,task);
		
		
		
	}

	public TObjectDoubleHashMap<SpatialVertex> calculateGammas(SpatialSparseGraph graph, double gammaMin, double gammaMax) {
		logger.info("Calculating accessability...");
		Set<Point> opportunities = new HashSet<Point>();
		
		for(SpatialSparseVertex vertex : graph.getVertices()) {
			opportunities.add(vertex.getPoint());
		}
		
		Accessability access = new Accessability();
		BeelineCostFunction cFunc = new BeelineCostFunction();
		cFunc.setDistanceCalculator(new CartesianDistanceCalculator());
		TObjectDoubleHashMap<SpatialVertex> values = access.values(graph.getVertices(), cFunc, opportunities);
		logger.info("Done.");
		
		double values2[] = values.getValues();
		double aMin = 83;//StatUtils.min(values2);
		double aMax = 130;//StatUtils.max(values2);
		double delta = (gammaMin - gammaMax) / (aMax - aMin);
		double b = gammaMax - (delta * aMin);

		TObjectDoubleHashMap<SpatialVertex> gammas = new TObjectDoubleHashMap<SpatialVertex>();
		TObjectDoubleIterator<SpatialVertex> it = values.iterator();
		Discretizer discretizer = new LinearDiscretizer(0.1);
		for(int i = 0; i < values.size(); i++) {
			it.advance();
			double gamma = discretizer.discretize(delta * it.value() + b) * 0.1;
			gammas.put(it.key(), gamma);
		}
		
		return gammas;
	}
	
	public void connect(SpatialSparseGraph graph, SpatialSparseGraphBuilder builder, TObjectDoubleHashMap<SpatialVertex> gammas) {
		LinkedList<SpatialSparseVertex> pending = new LinkedList<SpatialSparseVertex>();
		pending.addAll(graph.getVertices());
		
		SpatialSparseVertex v1;
		while ((v1 = pending.poll()) != null) {
			for (SpatialSparseVertex v2 : pending) {
				double p = probability(v1, v2, gammas.get(v1));
				if (random.nextDouble() <= p) {
					builder.addEdge(graph, v1, v2);
				}
			}
		}
	}
	
	private double probability(SpatialVertex v1, SpatialVertex v2, double gamma) {
		double d = discretizer.discretize(distanceCalculator.distance(v1.getPoint(), v2.getPoint()));
		d = Math.max(1.0, d);
		return scaleConst * Math.pow(d, -gamma);
	}
}
