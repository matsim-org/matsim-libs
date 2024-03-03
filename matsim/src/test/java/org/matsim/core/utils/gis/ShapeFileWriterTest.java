
/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.core.utils.gis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.matsim.testcases.MatsimTestUtils;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public class ShapeFileWriterTest {

	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testShapeFileWriter() throws IOException{

		String inFile = "src/test/resources/" + utils.getInputDirectory() + "test.shp";

		String outFile = utils.getOutputDirectory() + "/test.shp";
		SimpleFeatureSource s = ShapeFileReader.readDataFile(inFile);
			SimpleFeatureCollection fts = s.getFeatures();
			SimpleFeatureIterator it = fts.features();
			SimpleFeature ft = it.next();
			Geometry g = (Geometry) ft.getDefaultGeometry();
			List<SimpleFeature> fc = new ArrayList<>();
			fc.add(ft);
			ShapeFileWriter.writeGeometries(fc, outFile);

			SimpleFeatureSource s1 = ShapeFileReader.readDataFile(outFile);
			SimpleFeatureCollection fts1 = s1.getFeatures();
			SimpleFeatureIterator it1 = fts1.features();
			SimpleFeature ft1 = it1.next();
			Geometry g1 = (Geometry) ft1.getDefaultGeometry();

			Assertions.assertEquals(g.getCoordinates().length, g1.getCoordinates().length);

	}

	@Test
	void testShapeFileWriterWithSelfCreatedContent() throws IOException {
		String outFile = utils.getOutputDirectory() + "/test.shp";
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setName("EvacuationArea");
		b.setCRS(DefaultGeographicCRS.WGS84);
		b.add("the_geom", MultiPolygon.class);
		b.add("name", String.class);
		SimpleFeatureType ft = b.buildFeatureType();

		GeometryFactory geofac = new GeometryFactory();
		LinearRing lr = geofac.createLinearRing(new Coordinate[]{new Coordinate(0,0),new Coordinate(0,1),new Coordinate(1,1),new Coordinate(0,0)});
		Polygon p = geofac.createPolygon(lr,null);
		MultiPolygon mp = geofac.createMultiPolygon(new Polygon[]{p});
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		features.add(SimpleFeatureBuilder.build(ft, new Object[]{mp,"test_name"},"fid"));


		Geometry g0 = (Geometry) features.iterator().next().getDefaultGeometry();

		ShapeFileWriter.writeGeometries(features, outFile);

		SimpleFeatureSource s1 = ShapeFileReader.readDataFile(outFile);
		SimpleFeatureCollection fts1 = s1.getFeatures();
		SimpleFeatureIterator it1 = fts1.features();
		SimpleFeature ft1 = it1.next();
		Geometry g1 = (Geometry) ft1.getDefaultGeometry();

		Assertions.assertEquals(g0.getCoordinates().length, g1.getCoordinates().length);


	}

	@Test
	void testShapeFileWriterWithSelfCreatedContent_withMatsimFactory_Polygon() throws IOException {
		String outFile = utils.getOutputDirectory() + "test.shp";

		PolygonFeatureFactory ff = new PolygonFeatureFactory.Builder()
				.setName("EvacuationArea")
				.setCrs(DefaultGeographicCRS.WGS84)
				.addAttribute("name", String.class)
				.create();

		Coordinate[] coordinates = new Coordinate[]{new Coordinate(0,0),new Coordinate(0,1),new Coordinate(1,1),new Coordinate(0,0)};
		SimpleFeature f = ff.createPolygon(coordinates);

		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		features.add(f);

		Geometry g0 = (Geometry) f.getDefaultGeometry();

		ShapeFileWriter.writeGeometries(features, outFile);

		SimpleFeatureSource s1 = ShapeFileReader.readDataFile(outFile);
		SimpleFeatureCollection fts1 = s1.getFeatures();
		SimpleFeatureIterator it1 = fts1.features();
		SimpleFeature ft1 = it1.next();
		Geometry g1 = (Geometry) ft1.getDefaultGeometry();

		Assertions.assertEquals(g0.getCoordinates().length, g1.getCoordinates().length);
	}

	@Test
	void testShapeFileWriterWithSelfCreatedContent_withMatsimFactory_Polyline() throws IOException {
		String outFile = utils.getOutputDirectory() + "test.shp";

		PolylineFeatureFactory ff = new PolylineFeatureFactory.Builder()
		.setName("EvacuationArea")
		.setCrs(DefaultGeographicCRS.WGS84)
		.addAttribute("name", String.class)
		.create();

		Coordinate[] coordinates = new Coordinate[]{new Coordinate(0,0),new Coordinate(0,1),new Coordinate(1,1),new Coordinate(0,0)};
		SimpleFeature f = ff.createPolyline(coordinates);

		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		features.add(f);

		Geometry g0 = (Geometry) f.getDefaultGeometry();

		ShapeFileWriter.writeGeometries(features, outFile);

		SimpleFeatureSource s1 = ShapeFileReader.readDataFile(outFile);
		SimpleFeatureCollection fts1 = s1.getFeatures();
		SimpleFeatureIterator it1 = fts1.features();
		SimpleFeature ft1 = it1.next();
		Geometry g1 = (Geometry) ft1.getDefaultGeometry();

		Assertions.assertEquals(g0.getCoordinates().length, g1.getCoordinates().length);
	}

	@Test
	void testShapeFileWriterWithSelfCreatedContent_withMatsimFactory_Point() throws IOException {
		String outFile = utils.getOutputDirectory() + "test.shp";

		PointFeatureFactory ff = new PointFeatureFactory.Builder()
		.setName("EvacuationArea")
		.setCrs(DefaultGeographicCRS.WGS84)
		.addAttribute("name", String.class)
		.create();

		SimpleFeature f = ff.createPoint(new Coordinate(10, 20));

		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		features.add(f);

		Geometry g0 = (Geometry) f.getDefaultGeometry();

		ShapeFileWriter.writeGeometries(features, outFile);

		SimpleFeatureSource s1 = ShapeFileReader.readDataFile(outFile);
		SimpleFeatureCollection fts1 = s1.getFeatures();
		SimpleFeatureIterator it1 = fts1.features();
		SimpleFeature ft1 = it1.next();
		Geometry g1 = (Geometry) ft1.getDefaultGeometry();

		Assertions.assertEquals(g0.getCoordinates().length, g1.getCoordinates().length);
	}
}
