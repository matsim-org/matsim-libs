/* *********************************************************************** *
 * project: org.matsim.*
 * ZoneLayerSHPTest.java
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
package playground.johannes.gis;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.geotools.factory.FactoryConfigurationError;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeBuilder;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.contrib.sna.TestCaseUtils;
import org.matsim.contrib.sna.gis.CRSUtils;
import org.matsim.contrib.sna.gis.Zone;
import org.matsim.contrib.sna.gis.ZoneLayer;

import playground.johannes.socialnetworks.gis.io.ZoneLayerSHP;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * @author jillenberger
 *
 */
public class ZoneLayerSHPTest extends TestCase {

	public void test() throws FactoryConfigurationError, SchemaException, IllegalAttributeException, IOException {
		AttributeType attrs[] = new AttributeType[2];
		attrs[0] = DefaultAttributeTypeFactory.newAttributeType(Zone.GEOMETRY_ATTR_NAME, Geometry.class, true, null, null, CRSUtils.getCRS(21781));
		attrs[1] = AttributeTypeFactory.newAttributeType(Zone.INHABITANT_ATTR_NAME, Integer.class);
		
		
		FeatureType fType = FeatureTypeBuilder.newFeatureType(attrs, Zone.FEATURE_TYPE_NAME);
		
		GeometryFactory geoFactory = new GeometryFactory();
		
		Coordinate points[] = new Coordinate[5];
		points[0] = new Coordinate(0, 0);
		points[1] = new Coordinate(10, 0);
		points[2] = new Coordinate(10, 10);
		points[3] = new Coordinate(0, 10);
		points[4] = points[0];
		
		LinearRing ring = geoFactory.createLinearRing(points);
		Polygon polygon = geoFactory.createPolygon(ring, null);
		polygon.setSRID(21781);
		
		Object concreteAttrs[] = new Object[2];
		concreteAttrs[0] = polygon;
		concreteAttrs[1] = new Integer(1234);
		
		Feature feature = fType.create(concreteAttrs);
		
		Zone zone = new Zone(feature);
		Set<Zone> zones = new HashSet<Zone>();
		zones.add(zone);
		ZoneLayer layer1 = new ZoneLayer(zones);
		
//		String tmpfile = TestCaseUtils.getOutputDirectory() + "Zone.shp";
		String tmpfile = "path/to/shapefile/once/this/test/works";
		ZoneLayerSHP.write(layer1, tmpfile);
		
		ZoneLayer layer2 = ZoneLayerSHP.read(tmpfile);
		
		Point point = geoFactory.createPoint(new Coordinate(5, 5));
		Zone zone1 = layer1.getZone(point);
		Zone zone2 = layer2.getZone(point);
		
		assertEquals(zone1.getInhabitants(), zone2.getInhabitants());
//		assertEquals(zone1.getGeometry().getSRID(), zone2.getGeometry().getSRID());
		
		Coordinate[] coords1 = zone1.getGeometry().getCoordinates();
		Coordinate[] coords2 = zone2.getGeometry().getCoordinates();
		
		assertEquals(coords1.length, coords2.length);
		for(int i = 0; i < coords1.length; i++) {
			boolean found = false;
			for(int k = 0; k < coords2.length; k++) {
				if(coords1[i].x == coords2[k].x && coords1[i].y == coords2[k].y)
					found = true;
			}
			assertTrue(found);
		}
	}
}
