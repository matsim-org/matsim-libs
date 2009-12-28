/* *********************************************************************** *
 * project: org.matsim.*
 * NTransitivity.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.ivtsurveys;

import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TIntIntHashMap;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.io.SparseGraphMLReader;
import org.matsim.core.gbl.Gbl;

import playground.johannes.socialnetworks.statistics.Distribution;

/**
 * @author illenberger
 *
 */
public class NTransitivity {

	private static final Logger logger = Logger.getLogger(NTransitivity.class);
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		logger.info("Loading graph " + args[0] + "...");
		Graph g = new SparseGraphMLReader().readGraph(args[0]);
		Gbl.printMemoryUsage();

		TDoubleDoubleHashMap hist = getNTransitivity(g, Integer.parseInt(args[2]));
		Distribution.writeHistogram(hist, args[1]);
		logger.info("Done.");
	}

	public static TDoubleDoubleHashMap getNTransitivity(Graph g, int maxDepth) {
		TIntIntHashMap found = new TIntIntHashMap();
		TIntIntHashMap total = new TIntIntHashMap();
		int count = 0;
		int size = g.getVertices().size();
		for(Vertex v : g.getVertices()) {
			if(v.getEdges().size() > 1) {
				Set<Vertex> n1s = new HashSet<Vertex>(v.getNeighbours());
				Set<Vertex> roots = new HashSet<Vertex>();
				roots.add(v);
				for(Vertex n1 : v.getNeighbours()) {
					countCommonNeighbours(roots, n1s, n1, 2, maxDepth, found, total);
					n1s.remove(n1);
				}
			}
			count++;
			if(count % 1000 == 0) {
				logger.info(String.format(
						"Processed %1$s of %2$s persons. (%3$s )", count,
						size, count	/ (double) size));
			}
		}
		
		TDoubleDoubleHashMap hist = new TDoubleDoubleHashMap();
		for(int key : found.keys()) {
			int nFound = found.get(key);
			int nTotal = total.get(key);
			hist.put(key, nFound/(double)nTotal);
		}
		return hist;
	}
	
	private static void countCommonNeighbours(Set<Vertex> roots, Set<Vertex> rootNeighbours, Vertex v, int depth, int maxDepth, TIntIntHashMap found, TIntIntHashMap total) {
		roots.add(v);
		for(Vertex n1 : v.getNeighbours()) { 
			if(!roots.contains(n1)) {
				addCount(depth, total);
				if(rootNeighbours.contains(n1))
					addCount(depth, found);
				
				if(depth < maxDepth)
					countCommonNeighbours(roots, rootNeighbours, n1, depth + 1, maxDepth, found, total);
			}
		}
	}
	
	private static void addCount(int depth, TIntIntHashMap hist) {
		if(!hist.adjustValue(depth, 1)) {
			hist.put(depth, 1);
		}
	}
}
