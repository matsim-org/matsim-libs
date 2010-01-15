/* *********************************************************************** *
 * project: org.matsim.*
 * FeatureSHP.java
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
package playground.johannes.socialnetworks.gis.io;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;

/**
 * Utility class for reading shape (.shp) files.
 * 
 * @author illenberger
 * 
 */
public class FeatureSHP {

	/**
	 * Calls {@link FeatureSHP#readFeatures(String, String)} with <tt>null</tt>
	 * as feature type indicating to read all features.
	 * 
	 * @param filename
	 *            the path to the shape file.
	 * @return a set of features.
	 * @throws IOException
	 */
	public static Set<Feature> readFeatures(String filename) throws IOException {
		return FeatureSHP.readFeatures(filename, null);
	}

	/**
	 * Reads a shape file and returns a set of features.
	 * 
	 * @param filename
	 *            the path to the shape file.
	 * @param type
	 *            the feature type to read in, or <tt>null</tt> if all available
	 *            feature types should be read. (I have the feeling that this
	 *            does not work reliable. joh15/01/10)
	 * @return a set of features
	 * @throws IOException
	 */
	public static Set<Feature> readFeatures(String filename, String type) throws IOException {
		Map<String, URL> params = new HashMap<String, URL>();
		params.put("url", new File(filename).toURI().toURL());

		DataStore dataStore = DataStoreFinder.getDataStore(params);
		
		Set<Feature> features = new HashSet<Feature>();
		if(type == null) {
			for(String fType : dataStore.getTypeNames()) {
				addFeatures(dataStore.getFeatureSource(fType), features);
			}
		} else {
			addFeatures(dataStore.getFeatureSource(type), features);
		}
		
		return features;
		
	}
	
	private static void addFeatures(FeatureSource source, Set<Feature> features) throws IOException {
		for(Object feature : source.getFeatures()) {
			features.add((Feature) feature);
		}
	}
}
