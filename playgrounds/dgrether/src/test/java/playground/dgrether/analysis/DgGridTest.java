/* *********************************************************************** *
 * project: org.matsim.*
 * DgGridTest
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
package playground.dgrether.analysis;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.testcases.MatsimTestCase;
import org.opengis.feature.simple.SimpleFeature;

import playground.dgrether.utils.DgGrid;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Polygon;


/**
 * @author dgrether
 *
 */
public class DgGridTest extends MatsimTestCase {
	
	public void testGridCreation(){
		Coordinate c1 = new Coordinate(-100.0, -100.0);
		Coordinate c2 = new Coordinate(100.0, 100.0);
		Envelope env = new Envelope(c1, c2);
		DgGrid grid = new DgGrid(10, 10, env);
		
		Iterator<Polygon> pi = grid.iterator();
		List<SimpleFeature> features = new ArrayList<SimpleFeature>();

		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setName("grid");
		b.add("Polygon", Polygon.class);
		b.add("avgIncome", Double.class);
		SimpleFeatureBuilder builder = new SimpleFeatureBuilder(b.buildFeatureType());

		double avgIncome = 0.0;
		
		while (pi.hasNext()){
			Polygon p = pi.next();
			assertEquals(4 * 20.0, p.getLength());
			avgIncome += 100.0;
//		  CoordinateReferenceSystem targetCRS = MGC.getCRS(TransformationFactory.CH1903_LV03_GT);
		  SimpleFeature feature = builder.buildFeature(null,  new Object[]{p, avgIncome});
			features.add(feature);
		  //add to collection
		}
		
		ShapeFileWriter.writeGeometries(features, this.getOutputDirectory()+ "testGrid.shp");
	}

}
