/* *********************************************************************** *
 * project: org.matsim.*
 * DensityCorrelation.java
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

import org.apache.commons.math.stat.correlation.PearsonsCorrelation;
import org.apache.log4j.Logger;
import org.matsim.contrib.sna.gis.Zone;
import org.matsim.contrib.sna.gis.ZoneLayer;
import org.matsim.contrib.sna.graph.spatial.SpatialEdge;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;

import playground.johannes.socialnetworks.statistics.Correlations;
import visad.data.netcdf.UnsupportedOperationException;

/**
 * @author illenberger
 *
 */
public class DensityCorrelation {

	private static final Logger logger = Logger.getLogger(DensityCorrelation.class);
	
	public TDoubleDoubleHashMap densityCorrelation(SpatialGraph graph, ZoneLayer zones, double binsize) {
		double[] rhoValues1 = new double[graph.getEdges().size()*2];
		double[] rhoValues2 = new double[rhoValues1.length];
		
		int i = 0;
		int fails = 0;
		for(SpatialEdge e : graph.getEdges()) {
			SpatialVertex v1 = e.getVertices().getFirst();
			SpatialVertex v2 = e.getVertices().getSecond();
			
			Zone zone1 = zones.getZone(v1.getPoint());
			Zone zone2 = zones.getZone(v2.getPoint());
			if(zone1 != null && zone2 != null) {
				double rho1 = 0;//zone1.getPopulationDensity();
				double rho2 = 0;//zone2.getPopulationDensity();
			
				rhoValues1[i] = rho1;
				rhoValues2[i] = rho2;
				i++;
				rhoValues1[i] = rho2;
				rhoValues2[i] = rho1;
				i++;
				
				throw new UnsupportedOperationException("Fix this!");
			} else
				fails++;
		}
		
		if(fails > 0)
			logger.warn(String.format("Dropped %1$s samples out of %2$s because the zone for one vertex could not be obtained.", fails, i + fails));
		
		return Correlations.mean(rhoValues1, rhoValues2, binsize);
	}
	
	public double pearsonCorrelation(SpatialGraph graph, ZoneLayer zones) {
		TDoubleDoubleHashMap values = densityCorrelation(graph, zones, 1.0);
		double[] xValues = values.keys();
		double[] yValues = values.getValues();
		
		return new PearsonsCorrelation().correlation(xValues, yValues);
	}
}
