/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractErdosRenyiGenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.johannes.socialnetworks.graph.spatial.generators;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.graph.GraphBuilder;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.transformations.CH1903LV03toWGS84;
import org.xml.sax.SAXException;

import playground.johannes.socialnetworks.graph.io.PajekClusteringColorizer;
import playground.johannes.socialnetworks.graph.io.PajekDegreeColorizer;
import playground.johannes.socialnetworks.graph.spatial.SpatialGraphAnalyzer;
import playground.johannes.socialnetworks.graph.spatial.SpatialSparseEdge;
import playground.johannes.socialnetworks.graph.spatial.SpatialSparseGraph;
import playground.johannes.socialnetworks.graph.spatial.SpatialSparseGraphBuilder;
import playground.johannes.socialnetworks.graph.spatial.SpatialSparseVertex;
import playground.johannes.socialnetworks.graph.spatial.io.KMLDegreeStyle;
import playground.johannes.socialnetworks.graph.spatial.io.KMLVertexDescriptor;
import playground.johannes.socialnetworks.graph.spatial.io.KMLWriter;
import playground.johannes.socialnetworks.graph.spatial.io.PajekDistanceColorizer;
import playground.johannes.socialnetworks.graph.spatial.io.Population2SpatialGraph;
import playground.johannes.socialnetworks.graph.spatial.io.SpatialGraphMLReader;
import playground.johannes.socialnetworks.graph.spatial.io.SpatialGraphMLWriter;
import playground.johannes.socialnetworks.graph.spatial.io.SpatialPajekWriter;
import playground.johannes.socialnetworks.spatial.TravelTimeMatrix;
import playground.johannes.socialnetworks.spatial.Zone;
import playground.johannes.socialnetworks.spatial.ZoneLayer;
import playground.johannes.socialnetworks.spatial.ZoneLayerDouble;

/**
 * @author illenberger
 *
 */
public class SpatialRandomGraphGenerator<G extends SpatialSparseGraph, V extends SpatialSparseVertex, E extends SpatialSparseEdge> {

	private final static Logger logger = Logger.getLogger(SpatialRandomGraphGenerator.class);
	
	private GraphBuilder<G, V, E> builder;
	
	public SpatialRandomGraphGenerator(GraphBuilder<G, V, E> factory) {
		this.builder = factory;
	}

	public G generate(int numVertices, double gamma, double k_mean, long randomSeed) {
		G g = builder.createGraph();
		for (int i = 0; i < numVertices; i++)
			builder.addVertex(g);

		return generate(g, gamma, k_mean, randomSeed);
	}
	
	@SuppressWarnings("unchecked")
	public G generate(G graph, double gamma, double k_mean, long randomSeed) {
		logger.info("Calculating norm constant...");
		double z = getNormConstant(graph, gamma, k_mean);
		logger.warn("Norm constant="+z);
		LinkedList<V> pending = new LinkedList<V>();
		pending.addAll((Collection<? extends V>) graph.getVertices());
		
		int count = 0;
		int total = graph.getVertices().size(); 
		Random random = new Random(randomSeed);
		V v1;
		while ((v1 = pending.poll()) != null) {
			for (V v2 : pending) {
				double P = z * calcProba(v1, v2, gamma);
				if(P < 0 || P > 1 || Double.isNaN(P) || Double.isInfinite(P))
					logger.warn(String.format("P = %1$s!", P));
				
				if (random.nextDouble() <= P) {
					builder.addEdge(graph, v1, v2);
				}
			}
			count++;
			if(count % 1000 == 0)
				logger.info(String.format("Processed %1$s of %2$s vertices (%3$s)", count, total, count/(float)total));
		}

		return graph;

	}
	
	private double getNormConstant(G graph, double gamma, double k_mean) {
		LinkedList<V> pending = new LinkedList<V>();
		pending.addAll((Collection<? extends V>) graph.getVertices());
	
		int count = 0;
		int total = graph.getVertices().size();
		
		double sum = 0;
		V v1;
		while ((v1 = pending.poll()) != null) {
			for (V v2 : pending) {
				sum += calcProba(v1, v2, gamma);
			}
			count++;
			if(count % 1000 == 0)
				logger.info(String.format("Processed %1$s of %2$s vertices (%3$s)", count, total, count/(float)total));
		}
		
		return k_mean * graph.getVertices().size()/sum * 0.5;
	}
	
	private double calcProba(V v_i, V v_j, double gamma) {
		double d = CoordUtils.calcDistance(v_i.getCoordinate(), v_j.getCoordinate());
		d = Math.ceil(d/1000.0);
		d = Math.max(d, 1.0);
		return Math.pow(d, gamma);
	}
	
	private static final String MODULE_NAME = "gravityGenerator";
	
	public static void main(String[] args) throws FileNotFoundException, IOException, SAXException, ParserConfigurationException {
		Config config = new Config();
		MatsimConfigReader creader = new MatsimConfigReader(config);
		creader.parse(args[0]);
		
		SpatialSparseGraph graph = null;
		String graphFile = config.findParam(MODULE_NAME, "graphfile");
		
		if(graphFile == null) {
			Population2SpatialGraph reader = new Population2SpatialGraph(21781);
			graph = reader.read(config.findParam("plans", "inputPlansFile"));
		} else {
			SpatialGraphMLReader reader = new SpatialGraphMLReader(21781);
			graph = reader.readGraph(graphFile);
		}

		
		long randomSeed = Long.parseLong(config.getParam("global", "randomSeed"));
		
		double k_mean = Double.parseDouble(config.getParam(MODULE_NAME, "meanDegree"));
		
		String outputDir = config.getParam(MODULE_NAME, "output");
		
		String zonesFile = config.findParam(MODULE_NAME, "zonesFile");
		String densityFile = config.findParam(MODULE_NAME, "densityFile");
		String ttmatrixFile = config.findParam(MODULE_NAME, "ttmatrixFile");
		
		ZoneLayerDouble zones = null;
		ZoneLayer layer = null;
		TravelTimeMatrix ttmatrix = null;
		if(zonesFile != null && densityFile != null) {
			layer = ZoneLayer.createFromShapeFile(zonesFile);
			zones = ZoneLayerDouble.createFromFile(new HashSet<Zone>(layer.getZones()), densityFile);
			
			ttmatrix = TravelTimeMatrix.createFromFile(new HashSet<Zone>(layer.getZones()), ttmatrixFile);
		}
	
		new File(outputDir).mkdirs();
		
		SpatialSparseGraphBuilder builder = new SpatialSparseGraphBuilder();
		SpatialRandomGraphGenerator<SpatialSparseGraph, SpatialSparseVertex, SpatialSparseEdge> generator = new SpatialRandomGraphGenerator<SpatialSparseGraph, SpatialSparseVertex, SpatialSparseEdge>(builder);
		
		graph = generator.generate(graph, -1.6, k_mean, randomSeed);

		try {
			/*
			 * make directories
			 */
			String currentOutputDir = outputDir;//String.format("%1$s%2$s/", outputDir, iteration);
			File file = new File(currentOutputDir);
			file.mkdirs();
			/*
			 * graph analysis
			 */
			SpatialGraphAnalyzer.analyze(graph, currentOutputDir, false, zones, ttmatrix, null);
			/*
			 * graph output
			 * 
			 * graphML
			 */
			SpatialGraphMLWriter writer = new SpatialGraphMLWriter();
			writer.write(graph, String.format("%1$sgraph.graphml", currentOutputDir));
			/*
			 * KML
			 */
			KMLWriter kmlWriter = new KMLWriter();
			kmlWriter.setVertexStyle(new KMLDegreeStyle(kmlWriter.getVertexIconLink()));
			kmlWriter.setVertexDescriptor(new KMLVertexDescriptor(graph));
			kmlWriter.setDrawEdges(false);
			kmlWriter.setCoordinateTransformation(new CH1903LV03toWGS84());
			kmlWriter.write(graph, String.format("%1$sgraph.k.kml", currentOutputDir));
			/*
			 * Pajek
			 */
			PajekDegreeColorizer<SpatialSparseVertex, SpatialSparseEdge> colorizer1 = new PajekDegreeColorizer<SpatialSparseVertex, SpatialSparseEdge>(graph, true);
			PajekClusteringColorizer<SpatialSparseVertex, SpatialSparseEdge> colorizer2 = new PajekClusteringColorizer<SpatialSparseVertex, SpatialSparseEdge>(graph);
			PajekDistanceColorizer colorizer3 = new PajekDistanceColorizer(graph, false);
			SpatialPajekWriter pwriter = new SpatialPajekWriter();
			pwriter.write(graph, colorizer1, currentOutputDir + "graph.degree.net");
			pwriter.write(graph, colorizer2, currentOutputDir+ "graph.clustering.net");
			pwriter.write(graph, colorizer3, currentOutputDir + "graph.distance.net");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
