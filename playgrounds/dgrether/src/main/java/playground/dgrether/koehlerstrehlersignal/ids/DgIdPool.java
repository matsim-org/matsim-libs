/* *********************************************************************** *
 * project: org.matsim.*
 * DgIdPool
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
package playground.dgrether.koehlerstrehlersignal.ids;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;


/**
 * @author dgrether
 *
 */
public class DgIdPool {

	private static final Logger log = Logger.getLogger(DgIdPool.class);
	
	private int currentId = 10000; // < 10000 reserved for default ids
	
	private Map<String, Integer> ids = new HashMap<String, Integer>();
	private Map<Integer, String> intStringMap = new HashMap<Integer, String>();
	
	public <T> Id<T> createId(String idString, Class<T> type) {
		Integer i = this.createIntegerId(idString);
		return Id.create(Integer.toString(i), type);
	}
	
	public Integer createIntegerId(String idString) {
		if (ids.containsKey(idString)){
			return ids.get(idString);
		}
		Integer i = Integer.valueOf(this.currentId);
		this.ids.put(idString, i);
		this.intStringMap.put(i, idString);
		log.debug("created id matching from idstring " + idString + " to " + i);
		this.currentId++;
		return i;
	}
	
	public String getStringId(Integer intId) {
		return this.intStringMap.get(intId);
	}

	
	
	public void writeToFile(String filename) {
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		try {
			bw.write("matsim_id \t ks2010_id");
			bw.newLine();
			for (Entry<String, Integer> e : this.ids.entrySet()){
				bw.write(e.getKey());
				bw.write("\t");
				bw.write(Integer.toString(e.getValue()));
				bw.newLine();
			}
			bw.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		};
	}
	
	public static DgIdPool readFromFile(String filename){
		DgIdPool pool = new DgIdPool();
		BufferedReader br = null;
		try {
			br = IOUtils.getBufferedReader(filename);
			String line = br.readLine();
			line = br.readLine();
			Integer maxInt = Integer.MIN_VALUE;
			
			while (line != null) {
				String[] s = line.split("\t");
				String idString = s[0].trim();
				Integer idInt = Integer.valueOf(s[1].trim());
				
				if (maxInt < idInt){
					maxInt = idInt;
				}
				
				pool.ids.put(idString, idInt);
				pool.intStringMap.put(idInt, idString);
				line = br.readLine();
			}
			br.close();
			
			pool.currentId = maxInt++;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return pool;
	}

	
}
