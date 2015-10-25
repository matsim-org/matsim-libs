/* *********************************************************************** *
 * project: org.matsim.*
 * EgoSelector.java
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
package playground.johannes.coopsim.mental;

import gnu.trove.TIntArrayList;
import org.matsim.contrib.socnetgen.sna.graph.matrix.AdjacencyMatrix;
import org.matsim.contrib.socnetgen.sna.graph.matrix.Dijkstra;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialGraph;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialVertex;
import playground.johannes.coopsim.Profiler;
import playground.johannes.studies.sbsurvey.io.SocialSparseGraphMLReader;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author illenberger
 * @deprecated
 */
public class EgoSelector {

	private final Random random = new Random();
	
	private final AdjacencyMatrix<SocialVertex> y;
	
	private final Dijkstra dijkstra;
	
	private final List<SocialVertex> allEgos;
	
	public EgoSelector(SocialGraph graph) {
		allEgos = new ArrayList<SocialVertex>(graph.getVertices());
		y = new AdjacencyMatrix<SocialVertex>(graph);
		dijkstra = new Dijkstra(y);
	}
	public List<SocialVertex> selectEgos(int n) {
		List<SocialVertex> egos = new ArrayList<SocialVertex>(n);
		egos.add(allEgos.get(random.nextInt(allEgos.size())));
		
		for(int i = 1; i < n; i++) {
			SocialVertex ego = allEgos.get(random.nextInt(allEgos.size()));
			
			int idx_i = y.getIndex(ego);
			dijkstra.run(idx_i, -1);
			
			boolean accept = false;
			for(SocialVertex alter : egos) {
				if(ego != alter) {
					int idx_j = y.getIndex(alter);
					
//					dijkstra.run(idx_i, idx_j);
					
					TIntArrayList path = dijkstra.getPath(idx_i, idx_j);
					if(path != null) {
						if(path.size() > 3)
							accept = true;
						else
							accept = false;
					} else {
						accept = true;
					}
				}
			}
			
			if(accept)
				egos.add(ego);
		}
		
		return egos;
	}
	
	public static void main(String args[]) {
		SocialSparseGraphMLReader reader = new SocialSparseGraphMLReader();
		SocialGraph graph = reader.readGraph("/Users/jillenberger/Work/socialnets/locationChoice/mcmc.backup/run336/output/20000000000/graph.graphml");
		
		int diff = 0;
		EgoSelector selector = new EgoSelector(graph);
		Profiler.start("selecting");
		for(int i = 0; i < 1000; i++) {
			List<SocialVertex> egos = selector.selectEgos(4);
			diff += 4 - egos.size();
		}
		Profiler.stop("selecting",true);
		System.out.println(String.format("Average diff = %1$s.", diff/(double)1000));
	}
}
