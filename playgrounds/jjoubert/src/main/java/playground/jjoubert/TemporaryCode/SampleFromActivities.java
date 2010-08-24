/* *********************************************************************** *
 * project: org.matsim.*
 * sampleFromActivities.java
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

package playground.jjoubert.TemporaryCode;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import playground.jjoubert.Utilities.MyActivityReader;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

public class SampleFromActivities {
	private final static Logger log = Logger.getLogger(SampleFromActivities.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		log.info("Filtering activities to a specific window.");
		MultiPolygon window = createCityDeepWindow();
		
		String inputFilename = "/Users/johanwjoubert/MATSim/workspace/MATSimData/Gauteng/Activities/GautengMinorLocations.txt";
		String cityDeepFilename = "/Users/johanwjoubert/MATSim/workspace/MATSimData/Gauteng/Activities/GautengMinorLocations_CityDeepSample.txt";
		
		MyActivityReader mar = new MyActivityReader();
		mar.filterActivity(inputFilename, cityDeepFilename, window);
	}
	
	private static MultiPolygon createCityDeepWindow(){
		GeometryFactory gf = new GeometryFactory();
		// City Deep window
		double minX = 606483;
		double minY = 7097730;
		double maxX = 609162;
		double maxY = 7099431;
		Coordinate p1 = new Coordinate(minX, minY);
		Coordinate p2 = new Coordinate(maxX, minY);
		Coordinate p3 = new Coordinate(maxX, maxY);
		Coordinate p4 = new Coordinate(minX, maxY);
		Coordinate [] ca = {p1, p2, p3, p4, p1};
		ArrayList<Coordinate> al = new ArrayList<Coordinate>(5);
		al.add(p1);
		al.add(p2);
		al.add(p3);
		al.add(p4);
		al.add(p1);
		LinearRing lr = gf.createLinearRing(ca);
		
		Polygon poly = new Polygon(lr, null, gf);
		Polygon [] pa = {poly};
		MultiPolygon mp = gf.createMultiPolygon(pa);
		return mp;
	}

}
