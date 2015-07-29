/* *********************************************************************** *
 * project: org.matsim.*
 * FeatureTransformer.java
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

package org.matsim.contrib.evacuation.control.algorithms;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.matsim.core.config.Config;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Geometry;

public abstract class FeatureTransformer {

	public static void transform(SimpleFeature ft,
			CoordinateReferenceSystem coordinateSystem, Config c) throws FactoryException, MismatchedDimensionException, TransformException, IllegalArgumentException {
		CoordinateReferenceSystem target = CRS.decode(c.global().getCoordinateSystem(),true);

		MathTransform transform = CRS.findMathTransform(coordinateSystem, target,true);
		Geometry geo = (Geometry) ft.getDefaultGeometry();

		ft.setDefaultGeometry(JTS.transform(geo, transform));

	}
}
