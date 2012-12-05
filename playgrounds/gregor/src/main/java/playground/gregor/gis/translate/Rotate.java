/* *********************************************************************** *
 * project: org.matsim.*
 * Rotate.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.gregor.gis.translate;

import java.util.HashSet;
import java.util.Set;

import org.geotools.feature.Feature;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;

import Jama.Matrix;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

public class Rotate {

	private final static double Phi = 0.35;

	private static final Matrix m = new Matrix(new double[][]{new double[]{Math.cos(Phi),Math.sin(Phi)},new double[]{-Math.sin(Phi),Math.cos(Phi)}});

	
	public static void main(String [] args) {
		String input = "/Users/laemmel/devel/burgdorf2d/raw_input/floorplan.shp";
		String output ="/Users/laemmel/devel/burgdorf2d/raw_input/floorplan_rotated.shp";
		
		ShapeFileReader reader = new ShapeFileReader();
		reader.readFileAndInitialize(input);
		Set<Coordinate> handled = new HashSet<Coordinate>();
		
		Envelope e = reader.getBounds();
		
		double cx = e.getMinX() + e.getMaxX();
		cx /= 2;
		double cy = e.getMinY() + e.getMaxY();
		cy /= 2;
		for (Feature ft : reader.getFeatureSet()) {
			for (Coordinate c : ft.getDefaultGeometry().getCoordinates()) {
				if (handled.contains(c)) {
					System.out.println("hab schon!");
					continue;
				}
				Matrix cm = new Matrix(2,1);
				cm.set(0,0, c.x-cx);
				cm.set(1,0, c.y-cy);
				
				Matrix res = m.times(cm);
				c.x = res.get(0, 0)+cx;
				c.y = res.get(1, 0)+cy;
				handled.add(c);
				
			}
		}
		ShapeFileWriter.writeGeometries(reader.getFeatureSet(), output);
		
	}
	
}
