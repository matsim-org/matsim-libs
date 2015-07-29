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

package org.matsim.contrib.evacuation.io;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.matsim.contrib.evacuation.control.Controller;
import org.matsim.contrib.evacuation.control.ShapeFactory;
import org.matsim.contrib.evacuation.model.Constants;
import org.matsim.contrib.evacuation.model.shape.PolygonShape;
import org.matsim.contrib.evacuation.model.shape.ShapeStyle;
import org.matsim.contrib.evacuation.populationselector.CreatePopulationShapeFileFromExistingData;
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
public class ShapeIO {
	public static boolean savePolygon(Controller controller,
			PolygonShape polygonShape, String destinationFile) {
		String description = polygonShape.getDescription();

		if (!destinationFile.endsWith("shp"))
			destinationFile = destinationFile + ".shp";

		CoordinateReferenceSystem targetCRS = MGC.getCRS(controller
				.getSourceCoordinateSystem());

		Polygon polygon = polygonShape.getPolygon();
		Coordinate[] coordinates = polygon.getCoordinates();
		ArrayList<Coordinate> newCoordinatesList = new ArrayList<Coordinate>();

		for (int i = 0; i < coordinates.length; i++) {
			if ((i == coordinates.length - 1)
					|| (!newCoordinatesList.contains(coordinates[i]))) {
				newCoordinatesList.add(coordinates[i]);
				System.out.println("coord:" + coordinates[i].x + "\t "
						+ coordinates[i].y);
			}
		}
		if (coordinates.length > newCoordinatesList.size()) {
			Coordinate[] newCoordinates = new Coordinate[newCoordinatesList
					.size()];

			newCoordinatesList.toArray(newCoordinates);

			System.out.println("finally:");
			for (int i = 0; i < newCoordinates.length; i++) {
				System.out.println("coord:" + newCoordinates[i].x + "\t "
						+ newCoordinates[i].y);
			}

			GeometryFactory geofac = new GeometryFactory();
			LinearRing shell = geofac.createLinearRing(coordinates);
			polygon = geofac.createPolygon(shell, null);
		}

		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setName("EvacuationArea");
		b.setCRS(targetCRS);
		b.add("the_geom", MultiPolygon.class);
		b.add("name", String.class);
		SimpleFeatureType ft = b.buildFeatureType();

		try {
			MultiPolygon mp = new GeometryFactory(new PrecisionModel(2))
					.createMultiPolygon(new Polygon[] { polygon });
			SimpleFeature f = new SimpleFeatureBuilder(ft).buildFeature(
					description, new Object[] { mp, description });
			Collection<SimpleFeature> fts = new ArrayList<SimpleFeature>();
			fts.add(f);
			ShapeFileWriter.writeGeometries(fts, destinationFile);
			polygonShape.setFromFile(true);
			return true;
		} catch (FactoryRegistryException e) {
			e.printStackTrace();
			return false;
		}
	}

	public static boolean savePopulationAreaPolygons(Controller controller,
			ArrayList<PolygonShape> shapes, String destinationFile) {

		CoordinateReferenceSystem targetCRS = MGC.getCRS(controller
				.getSourceCoordinateSystem());

		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setName("EvacuationArea");
		b.setCRS(targetCRS);
		b.add("the_geom", MultiPolygon.class);
		b.add("persons", Long.class);
		SimpleFeatureType ft = b.buildFeatureType();
		SimpleFeatureBuilder builder = new SimpleFeatureBuilder(ft);

		try {
			Collection<SimpleFeature> fts = new ArrayList<SimpleFeature>();

			for (PolygonShape polygonShape : shapes) {
				Polygon currentPolygon = polygonShape.getPolygon();

				int pop = Integer.valueOf(polygonShape
						.getMetaData(Constants.POPULATION));

				MultiPolygon mp = new GeometryFactory(new PrecisionModel(2))
						.createMultiPolygon(new Polygon[] { currentPolygon });
				SimpleFeature f = builder.buildFeature(null, new Object[] { mp,
						pop });
				fts.add(f);
			}

			ShapeFileWriter.writeGeometries(fts, controller
					.getEvacuationConfigModule().getPopulationFileName());

			for (PolygonShape polygonShape : shapes)
				polygonShape.setFromFile(true);

			return true;
		} catch (FactoryRegistryException e) {
			e.printStackTrace();
			return false;
		}
	}

	public static PolygonShape getShapeFromFile(Controller controller,
			String id, String shapeFileString) {
		int layerID = controller.getVisualizer().getPrimaryShapeRenderLayer()
				.getId();
		PolygonShape newPolygon = new PolygonShape(layerID, null);
		newPolygon.setId(id);

		if (!new File(shapeFileString).exists()) {

			GeometryFactory geofac = new GeometryFactory();
			LinearRing lr = geofac.createLinearRing(new Coordinate[] {
					new Coordinate(0, 0), new Coordinate(1, 0),
					new Coordinate(1, 1), new Coordinate(0, 0) });
			Polygon p = geofac.createPolygon(lr, null);
			newPolygon.setPolygon(p);
			return newPolygon;

		}

		ShapeFileReader shapeFileReader = new ShapeFileReader();
		shapeFileReader.readFileAndInitialize(shapeFileString);

		CreatePopulationShapeFileFromExistingData.transformCRS(shapeFileReader);

		ArrayList<Geometry> geometries = new ArrayList<Geometry>();
		for (SimpleFeature ft : shapeFileReader.getFeatureSet()) {
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

	public static ArrayList<PolygonShape> getShapesFromFile(
			Controller controller, String shapeFileString, ShapeStyle style) {

		ArrayList<PolygonShape> shapes = new ArrayList<PolygonShape>();

		int layerID = controller.getVisualizer().getPrimaryShapeRenderLayer()
				.getId();

		ShapeFileReader shapeFileReader = new ShapeFileReader();
		shapeFileReader.readFileAndInitialize(shapeFileString);

		CreatePopulationShapeFileFromExistingData.transformCRS(shapeFileReader);

		for (SimpleFeature ft : shapeFileReader.getFeatureSet()) {
			Geometry geo = (Geometry) ft.getDefaultGeometry();
			Coordinate[] coords = geo.getCoordinates();
			coords[coords.length - 1] = coords[0];
			GeometryFactory geofac = new GeometryFactory();
			LinearRing shell = geofac.createLinearRing(coords);

			PolygonShape newPolygon = new PolygonShape(layerID, null);
			Polygon polygon = geofac.createPolygon(shell, null);

			String persons = ft.getAttribute("persons").toString();
			if (persons != null) {
				newPolygon.putMetaData(Constants.POPULATION, persons);
				ShapeFactory.setPopAreaStyle(newPolygon);
			}

			newPolygon.setPolygon(polygon);
			newPolygon.setStyle(style);
			newPolygon.setFromFile(true);

			shapes.add(newPolygon);
		}

		return shapes;
	}

}
