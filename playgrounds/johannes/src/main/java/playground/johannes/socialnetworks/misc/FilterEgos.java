/* *********************************************************************** *
 * project: org.matsim.*
 * FilterEgos.java
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
package playground.johannes.socialnetworks.misc;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.geotools.feature.Feature;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.contrib.sna.gis.CRSUtils;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import playground.johannes.socialnetworks.gis.io.FeatureSHP;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public class FilterEgos {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws FactoryException 
	 * @throws TransformException 
	 */
	public static void main(String[] args) throws IOException, FactoryException, TransformException {
		GeometryFactory factory = new GeometryFactory();
		/*
		 * read file with egos and coordinates
		 */
		Map<String, Coordinate> egos = new HashMap<String, Coordinate>();
		BufferedReader reader = new BufferedReader(new FileReader(args[0]));
		String line = reader.readLine();
		while((line = reader.readLine()) != null) {
			String[] tokens = line.split("\t");
			String egoId = tokens[0];
			double longitude = Double.parseDouble(tokens[1]);
			double latidude = Double.parseDouble(tokens[2]);
			egos.put(egoId, new Coordinate(longitude, latidude));
		}
		/*
		 * read shape file
		 */
		Set<Feature> cantons = FeatureSHP.readFeatures(args[1]);
		/*
		 * make coordinate transformation
		 */
		CoordinateReferenceSystem wgs84 = DefaultGeographicCRS.WGS84;
		CoordinateReferenceSystem ch1903 = CRSUtils.getCRS(21781);
		MathTransform transform = CRS.findMathTransform(wgs84, ch1903);
		/*
		 * iterate over all egos
		 */
		Set<String> validEgos = new HashSet<String>();
		for (Entry<String, Coordinate> entry : egos.entrySet()) {
			/*
			 * transform coordinates
			 */
			Coordinate coord = entry.getValue();
			double[] points = new double[] { coord.x, coord.y };
			transform.transform(points, 0, points, 0, 1);
			Point point = factory.createPoint(new Coordinate(points[0], points[1]));
			/*
			 * check if point is at least in one polygon
			 */
			for (Feature canton : cantons) {
				Geometry polygon = canton.getDefaultGeometry();
				if (polygon.contains(point)) {
					validEgos.add(entry.getKey());
					break;
				}
			}
		}
	}
}
