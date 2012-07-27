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

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
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
	
	public Id createId(String idString){
		Integer i = this.createIntegerId(idString);
		return new IdImpl(Integer.toString(i));
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
			e.printStackTrace();
		};
	}
	
}
