/* *********************************************************************** *
 * project: org.matsim.*
 * SHPFileUtil.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.polettif.boescpa.lib.tools;

import java.util.Set;

import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Merges all contained features to a single one.
 *
 * copy from christoph-playground
 */
public class SHPFileUtils {
	
	public Geometry mergeGeometries(Set<SimpleFeature> features) {
		Geometry geometry = null;
		
		for (SimpleFeature feature : features) {
			if (geometry == null) {
				geometry = (Geometry) ((Geometry) feature.getDefaultGeometry()).clone();
				continue;
			} else geometry = geometry.union((Geometry) feature.getDefaultGeometry());
		}
		return geometry;
	}
}
