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

package org.matsim.core.utils.gis;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureType;
import org.matsim.core.api.internal.MatsimSomeReader;

import com.vividsolutions.jts.geom.Envelope;

/**
 * @author glaemmel
 * @author dgrether
 */
public class ShapeFileReader implements MatsimSomeReader {

	private static final Logger log = Logger.getLogger(ShapeFileReader.class);

	private FeatureSource featureSource = null;

	private Envelope bounds = null;

	private DataStore dataStore = null;

	private FeatureCollection featureCollection = null;

	private FeatureType schema = null;
	
	private Set<Feature> featureSet = null;
	
	/**
	 * Provides access to a shape file and returns a <code>FeatureSource</code> containing all features.
	 * Take care access means on disk access, i.e. the FeatureSource is only a pointer to the information 
	 * stored in the file. This can be horribly slow if invoked many times and throw exceptions if two many read
	 * operations to the same file are performed. In those cases it is recommended to use the method readDataFileToMemory
	 * of this class.
	 *
	 * @param fileName File name of a shape file (ending in <code>*.shp</code>)
	 * @return FeatureSource containing all features.
	 * @throws IOException if the file cannot be found or another error happens during reading
	 * @deprecated use non static readFileAndInitialize Method
	 */
	@Deprecated
	public static FeatureSource readDataFile(final String fileName) throws IOException {
		return new ShapeFileReader().openFeatureSource(fileName);
	}
	
	private FeatureSource openFeatureSource(final String filename) throws IOException{
		log.info("reading features from: " + filename);
		File dataFile = new File(filename);
		HashMap<String, URL> connect = new HashMap<String, URL>();
		connect.put("url", dataFile.toURL());
		DataStore dataStore = DataStoreFinder.getDataStore(connect);
		String[] typeNames = dataStore.getTypeNames();
		String typeName = typeNames[0];
		FeatureSource fs = dataStore.getFeatureSource(typeName);
		return fs;
	}
	
	/**
	 * Reads all Features in the file into the returned Set and initializes the instance of this class.
	 */
	public Set<Feature> readFileAndInitialize(final String filename) throws IOException{
		this.featureSource = ShapeFileReader.readDataFile(filename);
		this.init();
		Feature ft = null;
		Iterator it = this.featureSource.getFeatures().iterator();
		this.featureSet = new HashSet<Feature>();
		while (it.hasNext()){
			ft = (Feature) it.next();
			this.featureSet.add(ft);
		}
		return this.featureSet;
	}


	private void init() throws IOException {
		this.bounds = this.featureSource.getBounds();
		this.dataStore = this.featureSource.getDataStore();
		this.featureCollection = this.featureSource.getFeatures();
		this.schema = this.featureSource.getSchema();
	}


	
	public FeatureSource getFeatureSource() {
		return featureSource;
	}
	
	public Envelope getBounds() {
		return bounds;
	}
	
	public DataStore getDataStore() {
		return dataStore;
	}
	
	public FeatureCollection getFeatureCollection() {
		return featureCollection;
	}
	
	public FeatureType getSchema() {
		return schema;
	}

	public Set<Feature> getFeatureSet() {
		return featureSet;
	}

	
	

}
