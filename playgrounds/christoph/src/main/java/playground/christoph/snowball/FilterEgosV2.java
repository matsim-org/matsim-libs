/* *********************************************************************** *
 * project: org.matsim.*
 * FilterEgosV2.java
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

import java.io.IOException;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

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

/**
 * @author illenberger
 * @author cdobler
 *
 */
public class FilterEgosV2 {

//	private static String shapeFile = "../../matsim/mysimulations/snowball/G1K08.shp";
	private String shapeFile;
	
	private static String[] germanCantonShortCuts = {"BS", "SO", "BL", "AG", "SH", "TG", "ZH", "LU", "ZG", "OW", "NW", "UR", "SZ", "SG", "AR", "AI", "GL", "BE", "GR"};
	
//	Ticino TI
//	Jura JU
//	Vaud VD
//	Fribourg FR
//	Neuchâtel NE
//	Genève GE
//	Valais VS
	
	private MathTransform transform;
	private GeometryFactory factory;
	private Set<Feature> germanCantons;
	private Set<Feature> otherCantons;
		
	public FilterEgosV2(String shapeFile)
	{
		this.shapeFile = shapeFile;
		
		try
		{
			init();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		catch (FactoryException e)
		{
			e.printStackTrace();
		} 
		catch (TransformException e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * @param coord
	 * @return "-1" if invalid coordinates, "1" if german canton, "2" if other canton, "3" if outside switzerland
	 */
	public int checkEgo(String x, String y)
	{
		if (x.equals("") || y.equals("")) return -1;
		/*
		 * transform coordinates
		 */
		double[] points = new double[] {Double.valueOf(x), Double.valueOf(y)};
		try 
		{
			transform.transform(points, 0, points, 0, 1);
		} 
		catch (TransformException e)
		{
			e.printStackTrace();
		}
		
		Point point = factory.createPoint(new Coordinate(points[0], points[1]));
		
		/*
		 * check if point is in a german Canton
		 */
		for (Feature canton : germanCantons) {
			Geometry polygon = canton.getDefaultGeometry();
			if (polygon.contains(point))
			{
				return 1;
			}
		}
		/*
		 * check if point is in an other Canton
		 */
		for (Feature canton : otherCantons) {
			Geometry polygon = canton.getDefaultGeometry();
			if (polygon.contains(point)) {
				return 2;
			}
		}
		
		/*
		 * coordinates are outside switzerland
		 */
		return 3;
	}
	
	private void init() throws IOException, FactoryException, TransformException
	{
		factory = new GeometryFactory();

		/*
		 * Cantons shape file
		 */
		otherCantons = new HashSet<Feature>();
		germanCantons = new HashSet<Feature>();
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
		transform = CRS.findMathTransform(wgs84, ch1903);
	}
}
