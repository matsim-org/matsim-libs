/* *********************************************************************** *
 * project: org.matsim.*
 * ShapeFileReader.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.utils.gis;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;

/**
 * @author glaemmel
 */
public class ShapeFileReader {

	private static final Logger log = Logger.getLogger(ShapeFileReader.class);
//	public static String WKT_WGS84 = "GEOGCS[\"WGS84\", DATUM[\"WGS84\", SPHEROID[\"WGS84\", 6378137.0, 298.257223563]], PRIMEM[\"Greenwich\", 0.0], UNIT[\"degree\",0.017453292519943295], AXIS[\"Longitude\",EAST], AXIS[\"Latitude\",NORTH]]";

	/**
	 * Reads a shape file and returns a <code>FeatureSource</code> containing all features.
	 *
	 * @param fileName File name of a shape file (ending in <code>*.shp</code>)
	 * @return FeatureSource containing all features.
	 * @throws IOException if the file cannot be found or another error happens during reading
	 */
	public static FeatureSource readDataFile(final String fileName) throws IOException {
		log.info("reading features from: " + fileName);

		File dataFile = new File(fileName);

		HashMap<String, URL> connect = new HashMap<String, URL>();
		connect.put("url", dataFile.toURL());

		DataStore dataStore = DataStoreFinder.getDataStore(connect);
		String[] typeNames = dataStore.getTypeNames();
		String typeName = typeNames[0];
		FeatureSource fs = dataStore.getFeatureSource(typeName);

		return fs;
	}

}
