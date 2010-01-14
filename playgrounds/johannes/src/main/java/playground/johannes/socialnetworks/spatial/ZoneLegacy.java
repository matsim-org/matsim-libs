/* *********************************************************************** *
 * project: org.matsim.*
 * Zone.java
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
package playground.johannes.socialnetworks.spatial;

import org.geotools.factory.FactoryConfigurationError;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeBuilder;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.sna.gis.CRSUtils;
import org.matsim.core.basic.v01.IdImpl;

import com.vividsolutions.jts.geom.Geometry;

/**
 * @author illenberger
 *
 */
public class ZoneLegacy {

	public static final String FEATURE_TYPE_NAME = "Zone";
	
	private static final String GEOMETRY_ATTR_NAME = "geometry";
	
	private static final String INHABITANT_ATTR_NAME = "inhabitant";
		
	private Feature feature;
	
	private Id id;
	
	public ZoneLegacy(Feature feature) {
		this.feature = feature;
		this.id = new IdImpl(feature.getID());
	}
	
	/**
	 * @deprecated
	 */
	public ZoneLegacy(Geometry polygon, Id id) {
		this(polygon, 0);
		this.id = id;
	}
	
	public ZoneLegacy(Geometry polygon, int inhabitants) {
		AttributeType attrs[] = new AttributeType[2];
		attrs[0] = DefaultAttributeTypeFactory.newAttributeType("geometry", Geometry.class, true, null, null, CRSUtils.getCRS(21781));
		attrs[1] = AttributeTypeFactory.newAttributeType("inhabitant", Integer.class);
		
		try {
			FeatureType fType = FeatureTypeBuilder.newFeatureType(attrs, FEATURE_TYPE_NAME);
			
			Object concreteAttrs[] = new Object[2];
			polygon.setSRID(21781);
			concreteAttrs[0] = polygon;
			concreteAttrs[1] = new Integer(inhabitants);
			
			feature = fType.create(concreteAttrs);
			this.id = new IdImpl(feature.getID());
		} catch (FactoryConfigurationError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SchemaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAttributeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public Geometry getBorder() {
		return feature.getDefaultGeometry();
	}
	
	/**
	 * @deprecated
	 * @return
	 */
	public Id getId() {
		return id;
	}
	
	public double getInhabitants() {
		return ((Double)feature.getAttribute("inhabitant")).doubleValue();
	}
	
	public Feature getFeature() {
		return feature;
	}
}
