/* *********************************************************************** *
 * project: org.matsim.*
 * MATSimNet2ShapeDemo.java
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
package playground.yu.utils;

/**
 * This class can convert a MATSim-network to a QGIS .shp-file
 * 
 * @author ychen
 * 
 */
public class MATSimNet2QGISDemo3 {
	public static String ch1903 = "PROJCS[\"CH1903_LV03\",GEOGCS[\"GCS_CH1903\",DATUM[\"D_CH1903\",SPHEROID[\"Bessel_1841\",6377397.155,299.1528128]],PRIMEM[\"Greenwich\",0],UNIT[\"Degree\",0.017453292519943295]],PROJECTION[\"Hotine_Oblique_Mercator_Azimuth_Center\"],PARAMETER[\"False_Easting\",600000],PARAMETER[\"False_Northing\",200000],PARAMETER[\"Scale_Factor\",1],PARAMETER[\"Azimuth\",90],PARAMETER[\"Longitude_Of_Center\",7.439583333333333],PARAMETER[\"Latitude_Of_Center\",46.95240555555556],UNIT[\"Meter\",1],AUTHORITY[\"EPSG\",\"21781\"]]";

	public static void main(String[] args) {
		MATSimNet2QGIS3 mn2q = new MATSimNet2QGIS3();
		// ///////////////////////////////////////////////////
		// write MATSim-network to Shp-file
		// ///////////////////////////////////////////////////
		mn2q.readNetwork("../schweiz-ivtch/network/ivtch-osm.1.6.xml");
		mn2q.setCrs(ch1903);
		mn2q.writeShapeFile("test/yu/utils/cap.1.6.mpg.shp");
		System.out.println("done.");
	}
}
