/* *********************************************************************** *
 * project: org.matsim.*
 * PointListDeserialiser.java
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

/**
 * 
 */
package playground.southafrica.freight.digicore.algorithms.djcluster.pointList;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.freight.digicore.containers.DigicoreVehicles;
import playground.southafrica.freight.digicore.io.algorithms.PointListAlgorithm;
import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.containers.MyZone;

/**
 * Class to deserialize the map objects that were created as a result of running
 * the {@link PointListAlgorithm} on a {@link DigicoreVehicles} container.
 * 
 * @author jwjoubert
 */
public class PointListDeserializer {
	final private static Logger LOG = Logger.getLogger(PointListDeserializer.class);
	final private File root;
	final private List<File> listOfFiles;
	
	
	public PointListDeserializer(String root) {
		this.root = new File(root);
		if(!this.root.isDirectory()){
			throw new IllegalArgumentException("The given root is not a directory.");
		}
		this.listOfFiles = FileUtils.sampleFiles(this.root, Integer.MAX_VALUE, FileUtils.getFileFilter(".data"));
		LOG.info("The given folder has " + this.listOfFiles.size() + " files to deserialize.");
	}
	
	
	public Map<Id<MyZone>, List<Coord>> deserializeAll(){
		LOG.info("Deserializing all files...");
		Map<Id<MyZone>, List<Coord>> map =  new HashMap<Id<MyZone>, List<Coord>>();
		Counter counter = new Counter("  files # ");
		for(File f : this.listOfFiles){
			Map<Id<MyZone>, List<Coord>> thisMap = deserialize(f);
			if(thisMap != null){
				for(Id<MyZone> id : thisMap.keySet()){
					if(map.containsKey(id)){
						map.get(id).addAll(thisMap.get(id));
					} else{
						map.put(id, thisMap.get(id));
					}
				}
			}
			counter.incCounter();
		}
		counter.printCounter();
		LOG.info("Done deserializing.");
		LOG.info("The consolidated map has " + map.size() + " zones with one " +
				"or more coordinates");
		return map;
	}

	
	@SuppressWarnings("unchecked")
	public Map<Id<MyZone>, List<Coord>> deserialize(File file){
		Map<Id<MyZone>, List<Coord>> map =  null;
		FileInputStream fis = null;
		ObjectInputStream ois = null;
		try {
			fis = new FileInputStream(file);
			ois = new ObjectInputStream(fis);
			
			Object o = ois.readObject();
			if(o instanceof Map<?,?>){
				Map<?,?> mo = (Map<?,?>)o;
				Iterator<?> iter = mo.keySet().iterator();
				if(iter.hasNext()){
					Object oo = iter.next();
					if(oo instanceof Id<?>){
						Id<?> id =(Id<?>)oo;
						Object ooo = mo.get(id);
						if(ooo instanceof List<?>){
							List<?> list = (List<?>) ooo;
							Object co = list.get(0);
							if(co instanceof Coord){
								map = (Map<Id<MyZone>, List<Coord>>)mo;
							}
						}
					}
				} else{
					/* Null return warning will be given later. */
				}
			}
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot deserialize " + file.getAbsolutePath());
		} finally{
			try {
				fis.close();
				ois.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot deserialize " + file.getAbsolutePath());
			}
		}
		
		if(map == null){
			/* An alternative would be to just not write out data for vehicles 
			 * with no activities in the given map. */
			LOG.warn("No deserializable map in " + file.getName() + ". Returning null.");
		}
		
		return map;
	}
}
