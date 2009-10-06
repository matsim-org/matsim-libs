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

import java.io.IOException;

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
		GeometryLayer layer = GeometryLayer.creatFromShapeFile("/Users/fearonni/vsp-work/work/socialnets/data/schweiz/complete/zones/gg-qg.merged.shp");
		ZoneLayerKMLWriter writer = new ZoneLayerKMLWriter();
		writer.write(layer, "/Users/fearonni/vsp-work/work/socialnets/data/schweiz/complete/zones/gg-qg.merged.kml");

	}

}
