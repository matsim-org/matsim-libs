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
package playground.yu.utils.qgis;

/**
 * This class can convert a MATSim-network to a QGIS .shp-file
 * 
 * @author ychen
 * 
 */
public class MATSimNet2QGISDemo implements X2QGIS {

	public static void main(String[] args) {
		MATSimNet2QGIS mn2q = new MATSimNet2QGIS();
		// ///////////////////////////////////////////////////
		// write MATSim-network to Shp-file
		// ///////////////////////////////////////////////////
		// mn2q.readNetwork("../schweiz-ivtch/network/ivtch-osm.xml");
		mn2q.readNetwork("./test/yu/test/equil_net_test.xml");
		mn2q.setCrs(ch1903);
		mn2q.writeShapeFile("./test/yu/test/equil_net_test/equil_net_test.shp");
		System.out.println("done.");
	}
}
