/* *********************************************************************** *
 * project: org.matsim.*
 * ErgmTravelTime.java
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
package playground.johannes.socialnetworks.graph.spatial.generators;

import gnu.trove.TIntObjectHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;

import playground.johannes.socialnetworks.graph.mcmc.AdjacencyMatrix;
import playground.johannes.socialnetworks.graph.mcmc.ErgmTerm;
import playground.johannes.socialnetworks.graph.spatial.SpatialAdjacencyMatrix;
import playground.johannes.socialnetworks.spatial.TravelTimeMatrix;
import playground.johannes.socialnetworks.spatial.ZoneLegacy;
import playground.johannes.socialnetworks.spatial.ZoneLayerLegacy;

/**
 * @author illenberger
 *
 */
public class ErgmTravelTime extends ErgmTerm {

	private static final Logger logger = Logger.getLogger(ErgmTravelTime.class);
	
	private static final double BETA = -0.0004;
	
	private TravelTimeMatrix matrix;
	
//	private ZoneLayer zones;
	
	private TIntObjectHashMap<ZoneLegacy> zoneMapping;
	
	public ErgmTravelTime(SpatialAdjacencyMatrix y, ZoneLayerLegacy zones, TravelTimeMatrix matrix) {
//		this.zones = zones;
		this.matrix = matrix;
	
		logger.info("Precaching zones...");
		zoneMapping = new TIntObjectHashMap<ZoneLegacy>();
		for(int i = 0; i < y.getVertexCount(); i++) {
			Coord c_i = ((SpatialAdjacencyMatrix)y).getVertex(i).getCoordinate();
			ZoneLegacy z_i = zones.getZone(c_i);
			if(z_i != null)
				zoneMapping.put(i, z_i);
		}
	}
	
	@Override
	public double changeStatistic(AdjacencyMatrix y, int i, int j, boolean yIj) {
//		Coord c_i = ((SpatialAdjacencyMatrix)y).getVertex(i).getCoordinate();
//		Coord c_j = ((SpatialAdjacencyMatrix)y).getVertex(j).getCoordinate();
//		
//		Zone z_i = zones.getZone(c_i);
//		Zone z_j = zones.getZone(c_j);
		
		ZoneLegacy z_i = zoneMapping.get(i);
		ZoneLegacy z_j = zoneMapping.get(j);
		
		if(z_i != null && z_j != null) {
			double tt = matrix.getTravelTime(z_i, z_j);
			tt = Math.max(60, tt);
			return - Math.log(Math.exp(BETA * tt) / tt);
		} else {
//			logger.warn("At least one zone is null!");
			return Double.POSITIVE_INFINITY;
		}
	}
	
//	private double descretize(double tt) {
//		
//	}

}
