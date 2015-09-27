/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.synpop.gis;

import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class DataPool {

	private static final Logger logger = Logger.getLogger(DataPool.class);
	
	private final Map<String, Object> dataObjects;
	
	private final Map<String, DataLoader> dataLoaders;
	
	public DataPool() {
		dataObjects = new HashMap<>();
		dataLoaders = new HashMap<>();
	}
	
	public void register(DataLoader loader, String key) {
		if(dataLoaders.containsKey(key)) {
			logger.warn(String.format("Cannot override the data loader for key \"%s\"", key));
		} else {
			dataLoaders.put(key, loader);
		}
	}
	
	public Object get(String key) {
		Object data = dataObjects.get(key);

		if(data == null) {
			loadData(key);
			data = dataObjects.get(key);
		}
		
		return data;
	}
	
	private synchronized void loadData(String key) {
		DataLoader loader = dataLoaders.get(key);
		if(loader == null) {
			logger.warn(String.format("No data loader for key \"%s\" found. Register the corresponding data loader first.", key));
		} else {
			Object data = loader.load();
			dataObjects.put(key, data);
		}
	}
}
