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

package playground.christoph.snowball;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class FilterEgos {

	private static String coordinateFile = "../../matsim/mysimulations/snowball/coordinates.txt";
	private static String shapeFile = "../../matsim/mysimulations/snowball/G1K08.shp";
	
	private static String[] germanCantonShortCuts = {"BS", "SO", "BL", "AG", "SH", "TG", "ZH", "LU", "ZG", "OW", "NW", "UR", "SZ", "SG", "AR", "AI", "GL", "BE", "GR"};
	
//	Ticino TI
//	Jura JU
//	Vaud VD
//	Fribourg FR
//	Neuchâtel NE
//	Genève GE
//	Valais VS
	
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
//		BufferedReader reader = new BufferedReader(new FileReader(args[0]));
		BufferedReader reader = new BufferedReader(new FileReader(coordinateFile));
		String line = reader.readLine();
		while((line = reader.readLine()) != null) {
			String[] tokens = line.split("\t");
			String egoId = tokens[0];
			double latidude = Double.parseDouble(tokens[1]);
			double longitude = Double.parseDouble(tokens[2]);
			egos.put(egoId, new Coordinate(longitude, latidude));
		}
		
		/*
		 * Cantons shape file
		 */
		Set<Feature> otherCantons = new HashSet<Feature>();
		Set<Feature> germanCantons = new HashSet<Feature>();
		FeatureSource featureSource = ShapeFileReader.readDataFile(shapeFile);
		for (Object o : featureSource.getFeatures()) {
			germanCantons.add((Feature) o);
		}
		
		/*
		 * print canton names and shortcuts
		 */
		for (Feature canton : germanCantons)
		{
			System.out.println(canton.getAttribute(2) + " " + canton.getAttribute(3));
		}
		
		/*
		 * filter cantons
		 */
		Iterator<Feature> iter = germanCantons.iterator();
		while (iter.hasNext())
		{
			Feature canton = iter.next();
			String cantonShortCut = (String) canton.getAttribute(3);
			boolean germanCanton = false;
			for (String shortCut : germanCantonShortCuts)
			{
				if (shortCut.equalsIgnoreCase(cantonShortCut))
				{
					germanCanton = true;
					break;
				}
			}
			if (!germanCanton)
			{
				iter.remove();
				otherCantons.add(canton);
			}
		}
		System.out.println("Using " + germanCantons.size() + " cantons.");
		
		/*
		 * make coordinate transformation
		 */
		CoordinateReferenceSystem wgs84 = DefaultGeographicCRS.WGS84;
		CoordinateReferenceSystem ch1903 = MGC.getCRS("EPSG:21781");
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
			 * check if point is in a german Canton
			 */
			boolean german = false;
			for (Feature canton : germanCantons) {
				Geometry polygon = canton.getDefaultGeometry();
				if (polygon.contains(point)) {
					validEgos.add(entry.getKey());
					german = true;
					break;
				}
			}
			/*
			 * check if point is in an other Canton
			 */
			boolean other = false;
			for (Feature canton : otherCantons) {
				Geometry polygon = canton.getDefaultGeometry();
				if (polygon.contains(point)) {
					validEgos.add(entry.getKey());
					other = true;
					break;
				}
			}
			
			if (!german)
			{
				if (other) System.out.println("Coordinates in a non german speaking canton! EgoId: " + entry.getKey());
				else System.out.println("Coordinates outside Switzerland! EgoId: " + entry.getKey());
			}
		}
		
		for (String ego : validEgos)
		{
			System.out.println("Valid EgoId: " + ego);
		}
		System.out.println("Total valid Egos: " + validEgos.size());
	}
}
