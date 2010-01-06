/* *********************************************************************** *
 * project: org.matsim.*
 * KMLWriter.java
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
package playground.johannes.socialnetworks.spatial;

import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TDoubleObjectHashMap;
import gnu.trove.TDoubleObjectIterator;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntIntIterator;
import gnu.trove.TObjectIntHashMap;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.utils.geometry.CoordUtils;
import org.xml.sax.SAXException;

import playground.johannes.socialnetworks.graph.spatial.SpatialGraphStatistics;
import playground.johannes.socialnetworks.graph.spatial.SpatialSparseGraph;
import playground.johannes.socialnetworks.graph.spatial.io.Population2SpatialGraph;
import playground.johannes.socialnetworks.statistics.Distribution;

/**
 * @author illenberger
 *
 */
public class PlotFractalDimension {

	private static final Logger logger = Logger.getLogger(PlotFractalDimension.class);
	
	private static final double normDescretization = 60.0;
	
	private static final double descretization = 60.0;
	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws ParserConfigurationException 
	 * @throws SAXException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException, SAXException, ParserConfigurationException {
		/*
		 * read network file
		 */
		NetworkLayer network = new NetworkLayer();
		NetworkReaderMatsimV1 reader = new NetworkReaderMatsimV1(network);
		reader.parse("/Users/fearonni/vsp-work/shared-svn/studies/schweiz-ivtch/baseCase/network/ivtch-changed-with-GTF.xml");
		/*
		 * read travel time matrix
		 */
		logger.info("Loading travel time matrix...");
		TObjectIntHashMap<Node> node2Idx = new TObjectIntHashMap<Node>();
		
		BufferedReader matrixReader = new BufferedReader(new FileReader("/Users/fearonni/vsp-work/work/socialnets/data/schweiz/complete/ttmatrix.real3.txt"));
		String line = matrixReader.readLine();
		String[] tokens = line.split("\t");
		for(int i = 1; i < tokens.length; i++) {
			String id = tokens[i];
			Node node = network.getNode(id);
			node2Idx.put(node, i-1);
		}
		
		int[][] ttmatrix = new int[node2Idx.size()][node2Idx.size()];
		Set<Node> nodes = new HashSet<Node>();
		while((line = matrixReader.readLine()) != null) {
			tokens = line.split("\t");
			String id = tokens[0];
			Node node = network.getNode(id);
			nodes.add(node);
			int i = node2Idx.get(node);
			for(int k = 1; k < tokens.length; k++) {
				int tt = Integer.parseInt(tokens[k]);
				ttmatrix[k-1][i] = tt;
			}
		}
		/*
		 * read graph
		 */
		SpatialSparseGraph graph = new Population2SpatialGraph().read("/Users/fearonni/vsp-work/work/socialnets/data/schweiz/complete/plans/plans.0.001.xml");
		/*
		 * cache nearest nodes
		 */
		int count = 0;
		Map<Vertex, Node> nearestNodes = new HashMap<Vertex, Node>();
		TObjectIntHashMap<Vertex> vertex2Idx = new TObjectIntHashMap<Vertex>();
		logger.info("Caching nearest nodes...");
		for(SpatialVertex v : graph.getVertices()) {
			Node n = getNearestNode(v.getCoordinate(), nodes);
			nearestNodes.put((Vertex) v, n);
			int i = node2Idx.get(n);
			vertex2Idx.put((Vertex) v, i);
			
			count++;
			if(count % 1000 == 0) {
				logger.info(String.format("Processed %1$s of %2$s vertices. (%3$s)", count, graph.getVertices().size(), count/(float)graph.getVertices().size()));
			}
		}
		logger.info("Calculating correlation function...");
		count = 0;
		Map<Vertex, TIntIntHashMap> numNodes_i = new HashMap<Vertex, TIntIntHashMap>();
		TDoubleDoubleHashMap n_tt_all = new TDoubleDoubleHashMap();
		for(SpatialVertex v : graph.getVertices()) {
			
				TIntIntHashMap n_tt = new TIntIntHashMap();
				
				for (SpatialVertex v_j : graph.getVertices()) {
					int i = vertex2Idx.get(v);
					int j = vertex2Idx.get((Vertex) v_j);

					int tt = ttmatrix[i][j];
					tt = (int) Math.ceil(tt / normDescretization);

					n_tt.adjustOrPutValue(tt, 1, 1);
					n_tt_all.adjustOrPutValue(tt, 1, 1);
				}
				
				numNodes_i.put(v, n_tt);
				
				count++;
				if(count % 10 == 0) {
					logger.info(String.format("Processed %1$s of %2$s vertices. (%3$s)", count, graph.getVertices().size(), count/(float)graph.getVertices().size()));
				}
			
		}
		/*
		 * make histogram
		 */
		Distribution.writeHistogram(n_tt_all,"/Users/fearonni/vsp-work/work/socialnets/data/schweiz/complete/fractalDim.txt");
		
		/*
		 * analyze density partitions
		 */
		ZoneLayer layer = ZoneLayer.createFromShapeFile("/Users/fearonni/vsp-work/work/socialnets/data/schweiz/complete/zones/gg-qg.merged.shp");
		ZoneLayerDouble  zones = ZoneLayerDouble.createFromFile(new HashSet<Zone>(layer.getZones()), "/Users/fearonni/vsp-work/work/socialnets/data/schweiz/complete/popdensity/popdensity.txt");
		
		TDoubleObjectHashMap values = SpatialGraphStatistics.createDensityPartitions(graph.getVertices(), zones, 1000);
		TDoubleObjectIterator it = values.iterator();
		for(int i = 0; i < values.size(); i++) {
			it.advance();
			Set<SpatialVertex> vertices = (Set<SpatialVertex>) it.value();
			
			TDoubleDoubleHashMap n_tt_rho = new TDoubleDoubleHashMap();
			for(SpatialVertex v : vertices) {
				TIntIntHashMap n_tt = numNodes_i.get(v);
				TIntIntIterator it2 = n_tt.iterator();
				for(int k = 0; k < n_tt.size(); k++) {
					it2.advance();
					n_tt_rho.adjustOrPutValue(it2.key(), it2.value(), it2.value());
				}
			}
			
			Distribution.writeHistogram(n_tt_rho, "/Users/fearonni/vsp-work/work/socialnets/data/schweiz/complete/fractalDim."+it.key()+".txt");
		}
		logger.info("Done.");
		
	}
	
	private static Node getNearestNode(Coord c, Set<Node> nodes) {
		Node theNode = null;
		double d_min = Double.MAX_VALUE; 
		for(Node node : nodes) {
			Coord c_n = node.getCoord();
			double d = CoordUtils.calcDistance(c, c_n);
			if(d < d_min) {
				theNode = node;
				d_min = d;
			}
		}
		
		return theNode;
	}

}
