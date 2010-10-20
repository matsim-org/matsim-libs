/* *********************************************************************** *
 * project: org.matsim.*
 * EdgeType.java
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
package playground.johannes.socialnetworks.survey.ivt2009.analysis;

import gnu.trove.TObjectIntHashMap;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.math.Distribution;

import playground.johannes.socialnetworks.graph.social.SocialEdge;
import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.social.analysis.SocioMatrixTask;

import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public class EdgeTypeTask extends SocioMatrixTask {

	private Set<Point> choiceSet;
	
	public EdgeTypeTask(Set<Point> choiceSet) {
		this.choiceSet = choiceSet;
	}
	
	@Override
	public void analyze(Graph g, Map<String, Double> stats) {
		if(getOutputDirectory() != null) {
			TObjectIntHashMap<String> distr = new TObjectIntHashMap<String>();
			
			SocialGraph graph = (SocialGraph) g;
			
			Map<String, Distribution> lenDistr = new HashMap<String, Distribution>();
			
			for(SocialEdge edge : graph.getEdges()) {
				if(edge.getType() != null) {
					distr.adjustOrPutValue(edge.getType(), 1, 1);
					
					Distribution d = lenDistr.get(edge.getType());
					if(d == null) {
						d = new Distribution();
						lenDistr.put(edge.getType(), d);
					}
					double dist = edge.length();
					if(!Double.isNaN(dist))
						d.add(dist);
				}
			}
			
			try {
				writeDistribution(distr, getOutputDirectory() + "/edgeType.txt");
				
				AcceptanceProbabilityEdgeType acc = new AcceptanceProbabilityEdgeType();
				
				for(Entry<String, Distribution> entry : lenDistr.entrySet()) {
					Distribution d = entry.getValue();
					stats.put("d_mean_" + entry.getKey(), d.mean());
					Distribution.writeHistogram(d.absoluteDistribution(1000), String.format("%1$s/d_%2$s.txt", getOutputDirectory(), entry.getKey()));
					Distribution.writeHistogram(d.absoluteDistributionLog2(1000), String.format("%1$s/d_%2$s.log.txt", getOutputDirectory(), entry.getKey()));
					
					Distribution a = acc.distribution(graph.getVertices(), choiceSet, entry.getKey());
					Distribution.writeHistogram(a.absoluteDistribution(1000), String.format("%1$s/p_acc_%2$s.txt", getOutputDirectory(), entry.getKey()));
					Distribution.writeHistogram(a.absoluteDistributionLog2(1000), String.format("%1$s/p_acc_%2$s.log.txt", getOutputDirectory(), entry.getKey()));
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

}
