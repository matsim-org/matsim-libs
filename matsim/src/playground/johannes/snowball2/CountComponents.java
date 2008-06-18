/* *********************************************************************** *
 * project: org.matsim.*
 * CountComponents.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.johannes.snowball2;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.matsim.utils.io.IOUtils;

import playground.johannes.snowball2.Centrality.CentralityGraph;
import playground.johannes.snowball2.Centrality.CentralityGraphDecorator;
import playground.johannes.snowball2.Centrality.CentralityVertex;
import edu.uci.ics.jung.graph.Graph;

/**
 * @author illenberger
 *
 */
public class CountComponents implements GraphStatistic {
	
	private Collection<Collection<CentralityVertex>> cSet;

	public double run(Graph g) {
		cSet = new HashSet<Collection<CentralityVertex>>();
		CentralityGraphDecorator graphDecorator = new CentralityGraphDecorator(g);
		UnweightedDijkstra dijkstra = new UnweightedDijkstra((CentralityGraph) graphDecorator.getSparseGraph());
		Queue<CentralityVertex> vertices = new LinkedList<CentralityVertex>((Collection<? extends CentralityVertex>) graphDecorator.getSparseGraph().getVertices());
		CentralityVertex source;
		while((source = vertices.poll()) != null) {
			List<CentralityVertex> reached = dijkstra.run(source);
			reached.add(source);
			cSet.add(reached);
			vertices.removeAll(reached);
		}
		
		return cSet.size();
	}

	public Collection<Collection<CentralityVertex>> getClusterSet() {
		return cSet;
	}
	
	public void dumpComponentSummary(String filename) {
		if(cSet != null) {
			Map<Integer, Integer> clusters = new HashMap<Integer, Integer>();
			for(Collection<CentralityVertex> cluster : cSet) {
				int size = cluster.size();
				Integer count = clusters.get(size);
				if(count == null)
					count = 0;
				count++;
				clusters.put(size, count);
			}
			
			try {
			BufferedWriter writer = IOUtils.getBufferedWriter(filename);
			for(Integer size : clusters.keySet()) {
				writer.write(String.valueOf(clusters.get(size)));
				writer.write(" x size ");
				writer.write(String.valueOf(size));
				writer.newLine();
			}
			writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
