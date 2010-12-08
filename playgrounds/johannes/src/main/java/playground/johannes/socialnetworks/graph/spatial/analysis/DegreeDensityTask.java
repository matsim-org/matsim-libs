/* *********************************************************************** *
 * project: org.matsim.*
 * PopDensityTask.java
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
package playground.johannes.socialnetworks.graph.spatial.analysis;

import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;

import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.gis.Zone;
import org.matsim.contrib.sna.gis.ZoneLayer;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.Degree;
import org.matsim.contrib.sna.graph.analysis.ModuleAnalyzerTask;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;

import playground.johannes.socialnetworks.statistics.Correlations;

/**
 * @author illenberger
 *
 */
public class DegreeDensityTask extends ModuleAnalyzerTask<Degree> {

	private static final Logger logger = Logger.getLogger(DegreeDensityTask.class);
	
	private ZoneLayer zones;
	
	public DegreeDensityTask(ZoneLayer zones) {
		setModule(Degree.getInstance());
		this.zones = zones;
	}

	@Override
	public void analyze(Graph graph, Map<String, Double> stats) {
		if(getOutputDirectory() != null) {
			TObjectDoubleHashMap<Vertex> kMap = module.values(graph.getVertices());
			
			double[] rhoValues = new double[kMap.size()];
			double[] kValues = new double[kMap.size()];
			
			int noZone = 0;
			
			TObjectDoubleIterator<Vertex> it = kMap.iterator();
			for (int i = 0; i < kMap.size(); i++) {
				it.advance();
				SpatialVertex v = (SpatialVertex) it.key();
				if(v.getPoint() != null) {
				Zone zone = zones.getZone(v.getPoint());
				if (zone != null) {
					rhoValues[i] = zone.getPopulationDensity();
					kValues[i] = it.value();
				} else {
					noZone++;
				}
				}
			}
		
			if(noZone > 0)
				logger.warn(String.format("No zone found for %1$s vertices out of %2$s.", noZone, kMap.size()));
			
			try {
				Correlations.writeToFile(Correlations.mean(rhoValues, kValues, 3000), String.format("%1$s/k_rho.txt", getOutputDirectory()), "rho", "k_mean");
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			logger.warn("No output directory specified!");
		}
	}
}
