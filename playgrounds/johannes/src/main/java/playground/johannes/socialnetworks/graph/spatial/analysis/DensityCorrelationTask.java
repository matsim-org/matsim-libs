/* *********************************************************************** *
 * project: org.matsim.*
 * DensityCorrelationTask.java
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

import gnu.trove.TDoubleDoubleHashMap;

import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.gis.ZoneLayer;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.analysis.ModuleAnalyzerTask;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;

import playground.johannes.socialnetworks.statistics.Correlations;

/**
 * @author illenberger
 *
 */
public class DensityCorrelationTask extends ModuleAnalyzerTask<DensityCorrelation> {

	private static final Logger logger = Logger.getLogger(DensityCorrelationTask.class);
	
	public static final String POPDENSITY_PERARSON_CORRELATION = "rho_rho_pearson";
	
	private final ZoneLayer zoneLayer;
	
	private static final double binsize = 1000.0;
	
	public DensityCorrelationTask(ZoneLayer zoneLayer) {
		this.setModule(new DensityCorrelation());
		this.zoneLayer = zoneLayer;
	}
	
	@Override
	public void analyze(Graph graph, Map<String, Double> stats) {
		double pearson = module.pearsonCorrelation((SpatialGraph) graph, zoneLayer);
		stats.put(POPDENSITY_PERARSON_CORRELATION, pearson);
		logger.info(String.format("%1$s\t%2$.4f", POPDENSITY_PERARSON_CORRELATION, pearson));
		
		if(getOutputDirectory() != null) {
			TDoubleDoubleHashMap values = module.densityCorrelation((SpatialGraph) graph, zoneLayer, binsize);
			try {
				Correlations.writeToFile(values, String.format("%1$s/rho_rho.txt", getOutputDirectory()), "rho", "rho");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
