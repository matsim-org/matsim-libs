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

package playground.christoph.evacuation.withinday.replanning.utils;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.gis.ShapeFileReader;

import com.vividsolutions.jts.geom.Geometry;

/*
 * Read a set of given SHP Files and merges all contained features to a single one.
 */
public class SHPFileUtil {
	
	public Set<Feature> readFile(String file) {
		try {
			Set<Feature> features = new HashSet<Feature>();
			
			FeatureSource featureSource = ShapeFileReader.readDataFile(file);
			for (Object o : featureSource.getFeatures()) {
				features.add((Feature) o);
			}
			return features;
		} catch (IOException e) {
			Gbl.errorMsg(e);
			return null;
		}
	}
	
	public Geometry mergeGeomgetries(Set<Feature> features) {
		Geometry geometry = null;
		
		for (Feature feature : features) {
			if (geometry == null) {
				geometry = (Geometry) feature.getDefaultGeometry().clone();
				continue;
			} else geometry.union(feature.getDefaultGeometry());
		}
		
		return geometry;
	}
}
