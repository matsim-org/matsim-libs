/* *********************************************************************** *
 * project: org.matsim.*
 * MyMapViewer.java
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


package org.matsim.contrib.grips.io;

import java.util.ArrayList;
import java.util.Collection;

import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.matsim.contrib.grips.control.Controller;
import org.matsim.contrib.grips.model.Constants;
import org.matsim.contrib.grips.model.shape.PolygonShape;
import org.matsim.contrib.grips.model.shape.ShapeStyle;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;

/**
 * all i/o functions concerning shapes
 * 
 * @author wdoering
 *
 */
public class ShapeIO
{
	public static boolean savePolygon(Controller controller, PolygonShape polygonShape, String destinationFile)
	{
		String description = polygonShape.getDescription();

		if (!destinationFile.endsWith("shp"))
			destinationFile = destinationFile + ".shp";

		CoordinateReferenceSystem targetCRS = MGC.getCRS(controller.getSourceCoordinateSystem());

		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setName("EvacuationArea");
		b.setCRS(targetCRS);
		b.add("location", MultiPolygon.class);
		b.add("name", String.class);
		SimpleFeatureType ft = b.buildFeatureType();

		try
		{
			MultiPolygon mp = new GeometryFactory(new PrecisionModel(2)).createMultiPolygon(new Polygon[] { polygonShape.getPolygon() });
			SimpleFeature f = new SimpleFeatureBuilder(ft).buildFeature(description, new Object[] { mp, description });
			Collection<SimpleFeature> fts = new ArrayList<SimpleFeature>();
			fts.add(f);
			ShapeFileWriter.writeGeometries(fts, destinationFile);
			return true;
		}
		catch (FactoryRegistryException e)
		{
			e.printStackTrace();
			return false;
		}
	}

	public static boolean savePopulationAreaPolygons(Controller controller, ArrayList<PolygonShape> shapes, String destinationFile)
	{

		CoordinateReferenceSystem targetCRS = MGC.getCRS(controller.getSourceCoordinateSystem());

		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setName("EvacuationArea");
		b.setCRS(targetCRS);
		b.add("location", MultiPolygon.class);
		b.add("persons", Long.class);
		SimpleFeatureType ft = b.buildFeatureType();
		SimpleFeatureBuilder builder = new SimpleFeatureBuilder(ft);

		try
		{
			Collection<SimpleFeature> fts = new ArrayList<SimpleFeature>();

			for (PolygonShape polygonShape : shapes)
			{
//				String id = polygonShape.getId();
				Polygon currentPolygon = polygonShape.getPolygon();

				int pop = Integer.valueOf(polygonShape.getMetaData(Constants.POPULATION));

				MultiPolygon mp = new GeometryFactory(new PrecisionModel(2)).createMultiPolygon(new Polygon[] { currentPolygon });
				SimpleFeature f = builder.buildFeature(null, new Object[] { mp, pop });
				fts.add(f);
			}

			ShapeFileWriter.writeGeometries(fts, controller.getGripsConfigModule().getPopulationFileName());
			return true;
		}
		catch (FactoryRegistryException e)
		{
			e.printStackTrace();
			return false;
		}
	}

	public static PolygonShape getShapeFromFile(Controller controller, String id, String shapeFileString)
	{
		int layerID = controller.getVisualizer().getPrimaryShapeRenderLayer().getId();
		PolygonShape newPolygon = new PolygonShape(layerID, null);
		newPolygon.setId(id);

		ShapeFileReader shapeFileReader = new ShapeFileReader();
		shapeFileReader.readFileAndInitialize(shapeFileString);

		ArrayList<Geometry> geometries = new ArrayList<Geometry>();
		for (SimpleFeature ft : shapeFileReader.getFeatureSet())
		{
			Geometry geo = (Geometry) ft.getDefaultGeometry();
			geometries.add(geo);
		}

		Coordinate[] coords = geometries.get(0).getCoordinates();
		coords[coords.length - 1] = coords[0];

		GeometryFactory geofac = new GeometryFactory();
		LinearRing shell = geofac.createLinearRing(coords);
		Polygon areaPolygon = geofac.createPolygon(shell, null);
		newPolygon.setPolygon(areaPolygon);

		return newPolygon;

	}
	
	public static ArrayList<PolygonShape> getShapesFromFile(Controller controller, String shapeFileString, ShapeStyle style)
	{
		
		ArrayList<PolygonShape> shapes = new ArrayList<PolygonShape>();
		
		int layerID = controller.getVisualizer().getPrimaryShapeRenderLayer().getId();

		ShapeFileReader shapeFileReader = new ShapeFileReader();
		shapeFileReader.readFileAndInitialize(shapeFileString);

		for (SimpleFeature ft : shapeFileReader.getFeatureSet())
		{
			Geometry geo = (Geometry) ft.getDefaultGeometry();
			Coordinate[] coords = geo.getCoordinates();
			coords[coords.length - 1] = coords[0];
			GeometryFactory geofac = new GeometryFactory();
			LinearRing shell = geofac.createLinearRing(coords);
			
			PolygonShape newPolygon = new PolygonShape(layerID, null);
			Polygon polygon = geofac.createPolygon(shell, null);
			newPolygon.setPolygon(polygon);
			newPolygon.setStyle(style);
			
			shapes.add(newPolygon);
		}
		
		return shapes;
	}	

}
