/* *********************************************************************** *
 * project: org.matsim.*
 * FloodingInfoExtractor.java
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
package playground.gregor.flooding;


import org.matsim.contrib.evacuation.flooding.FloodingInfo;
import org.matsim.contrib.evacuation.flooding.FloodingReader;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * @author laemmel
 *
 */
public class FloodingInfoExtractor {
	
	
//	private static final double EASTING = 650220;
//	private static final double NORTHING = 9897595;
	private static final double EASTING = 650745;
	private static final double NORTHING = 9895438;
	private static final Coordinate TARGET = new Coordinate(EASTING, NORTHING);
	private int count = 0;
	
	private void run() {


		int count = 8;
		if (count <= 0) {
			return;
		}
		
		double offsetEast = 632968.461027224;
		double offsetNorth = 9880201.726;
		for (int i = 0; i < count; i++) {
			String netcdf = "/home/laemmel/arbeit/svn/shared-svn/studies/countries/id/padang/inundation/20100201_sz_pc_2b_tide_subsidence/SZ_hilman_2b_subsidence_more_points_P" + i + "_8.sww";
			FloodingReader fr = new FloodingReader(netcdf);
			fr.setReadTriangles(true);
			fr.setReadFloodingSeries(true);
			fr.setOffset(offsetEast, offsetNorth);
			if (coordinateFound(fr)) {
				break;
			}
		
		}
		System.out.println("fis:" + this.count);
		
		
	}

	/**
	 * @param fr
	 * @return
	 */
	private boolean coordinateFound(FloodingReader fr) {

		for (FloodingInfo fi : fr.getFloodingInfos()) {
			if (fi.getCoordinate().distance(TARGET) < 5){
				System.out.println(fi.getFloodingSeries());
				return true;
			}
			this.count++;
		
		}
		return false;
	}

	public static void main (String [] args) {
		new FloodingInfoExtractor().run();
	}

}
