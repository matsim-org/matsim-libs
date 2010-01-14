/* *********************************************************************** *
 * project: org.matsim.*
 * Shape2KML.java
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

import gnu.trove.TObjectDoubleHashMap;

import java.io.IOException;
import java.util.HashSet;

import com.vividsolutions.jts.geom.Geometry;

/**
 * @author illenberger
 *
 */
public class Shape2KML {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		ZoneLayerLegacy layer = ZoneLayerLegacy.createFromShapeFile("/Users/fearonni/vsp-work/work/socialnets/data/schweiz/complete/zones/gg-qg.merged.shp");
		ZoneLayerDouble densityLayer = ZoneLayerDouble.createFromFile(new HashSet<ZoneLegacy>(layer.getZones()), "/Users/fearonni/vsp-work/work/socialnets/data/schweiz/complete/popdensity/popdensity.txt");
		ZoneLayerKMLWriter writer = new ZoneLayerKMLWriter();
		
		TObjectDoubleHashMap<Geometry> geoValues = new TObjectDoubleHashMap<Geometry>();
		
		for(ZoneLegacy z_j : densityLayer.getZones()) {
			double tt = densityLayer.getValue(z_j);
			geoValues.put(z_j.getBorder(), tt);
		}
		
		writer.write(layer.getGeometryLayer(), "/Users/fearonni/vsp-work/work/socialnets/data/schweiz/complete/popdensity/popdensity.kml", geoValues);

	}

	
//	public static void main(String[] args) throws IOException {
//		ZoneLayer zoneLayer = ZoneLayer.createFromShapeFile("/Users/fearonni/vsp-work/work/socialnets/data/schweiz/complete/zones/gg-qg.merged.shp");
//		TravelTimeMatrix matrix = TravelTimeMatrix.createFromFile(new HashSet<Zone>(zoneLayer.getZones()), "/Users/fearonni/vsp-work/work/socialnets/data/schweiz/complete/ttmatrix.txt");
//		
//		Zone z_i = matrix.getZones().iterator().next();
//		
//		TObjectDoubleHashMap<Geometry> geoValues = new TObjectDoubleHashMap<Geometry>();
//		
//		for(Zone z_j : matrix.getZones()) {
//			double tt = matrix.getTravelTime(z_i, z_j);
//			geoValues.put(z_j.getBorder(), tt);
//		}
//		
//		ZoneLayerKMLWriter writer = new ZoneLayerKMLWriter();
//		writer.write(zoneLayer.getGeometryLayer(), "/Users/fearonni/vsp-work/work/socialnets/data/schweiz/complete/zones/traveltimes.kml", geoValues);
//	}
}
