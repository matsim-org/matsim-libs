package org.matsim.utils.gis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import junit.framework.Assert;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.testcases.MatsimTestUtils;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

public class ShapeFileWriterTest {
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils(); 

	@Test
	public void testShapeFileWriter() throws IOException{
		
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
			
			Assert.assertEquals(g.getCoordinates().length, g1.getCoordinates().length);
			
	}
	
	@Test
	public void testShapeFileWriterWithSelfCreatedContent() throws IOException {
		String outFile = utils.getOutputDirectory() + "/test.shp"; 
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setName("EvacuationArea");
		b.setCRS(DefaultGeographicCRS.WGS84);
		b.add("location", MultiPolygon.class);
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
		
		Assert.assertEquals(g0.getCoordinates().length, g1.getCoordinates().length);
		
		
	}
}
