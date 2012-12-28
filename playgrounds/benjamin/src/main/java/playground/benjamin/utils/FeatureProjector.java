/* *********************************************************************** *
 * project: org.matsim.*
 * FeatureProjector.java
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
package playground.benjamin.utils;

import java.util.Collection;
import java.util.HashSet;

import org.apache.log4j.Logger;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Geometry;

/**
 * @author benjamin
 *
 */
public class FeatureProjector {
	private static Logger logger = Logger.getLogger(FeatureProjector.class);

	MathTransform transform;
	
	public FeatureProjector(String from, String to){
		Exception ex;
		try {
			this.transform = CRS.findMathTransform(CRS.decode(from), CRS.decode(to), true);
			return;
		} catch (NoSuchAuthorityCodeException e) {
			ex = e;
			e.printStackTrace();
		} catch (FactoryException e) {
			ex = e;
			e.printStackTrace();
		}
		throw new RuntimeException(ex);
	}
	
	public Collection<SimpleFeature> getProjectedFeatures(Collection<SimpleFeature> fts) {
		Collection<SimpleFeature> projectedFeatrues = new HashSet<SimpleFeature>();
		for (SimpleFeature ft : fts) {
			SimpleFeature projectedFeature = getProjectedFeature(ft);
			projectedFeatrues.add(projectedFeature);
		}
		return projectedFeatrues;
	}
	
	public SimpleFeature getProjectedFeature(SimpleFeature feature) {
		Exception ex;
		try {
			Geometry before = (Geometry) feature.getDefaultGeometry();
			Geometry after = JTS.transform(before, this.transform);
			feature.setDefaultGeometry(after);
//			logger.debug("Setting coordinate of geometry from " + before.getCoordinate() + " to " + after.getCoordinate());
			return feature;
		} catch (TransformException e) {
			ex = e; 
		} catch (IllegalArgumentException e) {
			ex = e; 
		}
		throw new RuntimeException(ex);
	}
}
