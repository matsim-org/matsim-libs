/* *********************************************************************** *
 * project: org.matsim.*
 * MultiPolygonMaker.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.gregor.gis.union;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.geotools.data.FeatureSource;
import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeFactory;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

/**
 * @author laemmel
 * 
 */
public class MultiPolygonMaker {

	public static void main(String[] args) throws IOException, FactoryRegistryException, SchemaException, IllegalAttributeException {
		String fileIn = "/Users/laemmel/teach/simpleEnvPolygon.shp";
		String fileOut = "/Users/laemmel/teach/simpleEnvMultiPolygon.shp";

		List<Polygon> p = new ArrayList<Polygon>();
		FeatureSource fs = ShapeFileReader.readDataFile(fileIn);
		Iterator it = fs.getFeatures().iterator();
		while (it.hasNext()) {
			Feature ft = (Feature) it.next();
			if (ft.getDefaultGeometry() instanceof Polygon) {
				p.add((Polygon) ft.getDefaultGeometry());
			} else if (ft.getDefaultGeometry() instanceof MultiPolygon) {
				for (int i = 0; i < ft.getDefaultGeometry().getNumGeometries(); i++) {
					p.add((Polygon) ft.getDefaultGeometry().getGeometryN(i));
				}
			}
		}

		GeometryFactory geofac = new GeometryFactory();

		Polygon[] polygons = new Polygon[p.size()];
		for (int i = 0; i < p.size(); i++) {
			polygons[i] = p.get(i);
		}

		MultiPolygon mp = geofac.createMultiPolygon(polygons);

		ArrayList<Feature> features = new ArrayList<Feature>();
		CoordinateReferenceSystem crs = fs.getSchema().getDefaultGeometry().getCoordinateSystem();
		AttributeType geom = DefaultAttributeTypeFactory.newAttributeType("MultiPolygon", MultiPolygon.class, true, null, null, crs);
		AttributeType floor = AttributeTypeFactory.newAttributeType("floor", Integer.class);
		FeatureType ft = FeatureTypeFactory.newFeatureType(new AttributeType[] { geom, floor }, "FLOORS");

		Feature feature = ft.create(new Object[] { mp, 0 });
		features.add(feature);

		ShapeFileWriter.writeGeometries(features, fileOut);
	}

}
