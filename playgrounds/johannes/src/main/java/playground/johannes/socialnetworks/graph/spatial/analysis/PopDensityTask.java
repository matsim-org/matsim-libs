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

import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.gis.Zone;
import org.matsim.contrib.sna.gis.ZoneLayer;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.Degree;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;

import playground.johannes.socialnetworks.graph.analysis.AbstractGraphAnalyzerTask;
import playground.johannes.socialnetworks.graph.analysis.DegreeTask;
import playground.johannes.socialnetworks.statistics.Correlations;

/**
 * @author illenberger
 *
 */
public class PopDensityTask extends AbstractGraphAnalyzerTask {

	private static final Logger logger = Logger.getLogger(PopDensityTask.class);
	
	public static final String POPDENSITY_PERARSON_CORRELATION = "rho_rho_pearson";
	
	private ZoneLayer zones;
	
	public PopDensityTask(String output, ZoneLayer zones) {
		super(output);
		this.zones = zones;
	}

	@Override
	public void analyze(Graph graph, Map<String, Object> analyzers,	Map<String, Double> stats) {
		if(getOutputDirectory() != null) {
			try {
			Set<? extends SpatialVertex> vertices = (Set<? extends SpatialVertex>) graph.getVertices();
			TObjectDoubleHashMap<SpatialVertex> densityValues = new TObjectDoubleHashMap<SpatialVertex>();

			for (SpatialVertex v : vertices) {
				Zone zone = zones.getZone(v.getPoint());
				if (zone != null) {
					double rho = zone.getPopulationDensity();
					densityValues.put(v, rho);
				}
			}
		
			Object obj;
			
			Degree degree;
			obj = analyzers.get(DegreeTask.class.getCanonicalName());
			if(obj == null)
				degree = new Degree();
			else {
				degree = (Degree)obj;
			}
			Correlations.writeToFile(getCorrelation(degree.values(vertices), densityValues), String.format("%1$s/k_rho.txt", getOutputDirectory()), "rho", "k_mean");
			
			DensityCorrelation rhoCorrelation;
			obj = analyzers.get(DensityCorrelation.class.getCanonicalName());
			if(obj == null)
				rhoCorrelation = new DensityCorrelation();
			else {
				rhoCorrelation = (DensityCorrelation)obj;
			}
			Correlations.writeToFile(rhoCorrelation.densityCorrelation((SpatialGraph) graph, zones, 2000.0), String.format("%1$s/rho_rho.txt", getOutputDirectory()), "rho", "rho");
			
			double pearson = rhoCorrelation.pearsonCorrelation((SpatialGraph) graph, zones);
			stats.put(POPDENSITY_PERARSON_CORRELATION, pearson);
			logger.info(String.format("%1$s\t%2$.4f", POPDENSITY_PERARSON_CORRELATION, pearson));
				
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private TDoubleDoubleHashMap getCorrelation(TObjectDoubleHashMap<Vertex> vValues, TObjectDoubleHashMap<SpatialVertex> dValues) {
		double[] rhoValues = new double[dValues.size()];
		double[] values = new double[rhoValues.length];
		
		TObjectDoubleIterator<SpatialVertex> it = dValues.iterator();
		for(int i = 0; i < rhoValues.length; i++) {
			it.advance();
			rhoValues[i] = vValues.get(it.key());
			values[i] = it.value();
		}
		
		return Correlations.correlationMean(rhoValues, values);
	}
}
