/* *********************************************************************** *
 * project: org.matsim.*
 * ZoneLayerTest.java
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
import org.matsim.contrib.sna.gis.CRSUtils;
import org.matsim.contrib.sna.gis.Zone;
import org.matsim.contrib.sna.gis.ZoneLayer;

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
public class ZoneLayerTest extends TestCase {

	public void test() throws FactoryConfigurationError, SchemaException, IllegalAttributeException {
//		AttributeType attrs[] = new AttributeType[2];
//		attrs[0] = DefaultAttributeTypeFactory.newAttributeType(Zone.GEOMETRY_ATTR_NAME, Geometry.class, true, null, null, CRSUtils.getCRS(21781));
//		attrs[1] = AttributeTypeFactory.newAttributeType(Zone.INHABITANT_ATTR_NAME, Integer.class);
//		
//		
//		FeatureType fType = FeatureTypeBuilder.newFeatureType(attrs, Zone.FEATURE_TYPE_NAME);
//		
//		GeometryFactory geoFactory = new GeometryFactory();
//		
//		Set<Zone> zones = new HashSet<Zone>();
//		for(int x = 0; x < 30; x+=10) {
//			for(int y = 0; y < 30; y+=10) {
//				Coordinate points[] = new Coordinate[5];
//				points[0] = new Coordinate(x, y);
//				points[1] = new Coordinate(x+9, y);
//				points[2] = new Coordinate(x+9, y+9);
//				points[3] = new Coordinate(x, y+9);
//				points[4] = points[0];
//				
//				LinearRing ring = geoFactory.createLinearRing(points);
//				Polygon polygon = geoFactory.createPolygon(ring, null);
//				
//				Object concreteAttrs[] = new Object[2];
//				concreteAttrs[0] = polygon;
//				concreteAttrs[1] = new Integer(1234);
//				
//				Feature feature = fType.create(concreteAttrs);
//				
//				Zone zone = new Zone(feature);
//				zones.add(zone);
//			}
//		}
//		
//		ZoneLayer layer = new ZoneLayer(zones);
//		
//		Point point1 = geoFactory.createPoint(new Coordinate(5, 5));
//		Zone zone1 = layer.getZone(point1);
//		assertTrue(zone1.getGeometry().contains(point1));
//		
//		Point point2 = geoFactory.createPoint(new Coordinate(31, 5));
//		Zone zone2 = layer.getZone(point2);
//		assertNull(zone2);
//		
	}
}
