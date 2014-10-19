/* *********************************************************************** *
 * project: org.matsim.*
 * SubplaceCountChecker.java
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

package playground.southafrica.population.analysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.utilities.Header;

public class SubplaceCountChecker {
	private final static Logger LOG = Logger.getLogger(SubplaceCountChecker.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(SubplaceCountChecker.class.toString(), args);
		String inputFile = args[0];
		String outputFile = args[1];
		Map<Id<Object>, Tuple<Integer, Integer>> map = new HashMap<>();
		
		LOG.info("Reading lines...");
		Counter counter = new Counter("  lines # ");
		BufferedReader br = IOUtils.getBufferedReader(inputFile);
		try {
			String line = br.readLine(); /* Header */
			while((line = br.readLine()) != null){
				String[] sa = line.split(",");
				int mn = Integer.parseInt(sa[6]);
				Id<Object> sp = Id.create(sa[12], Object.class);
				
				if(!map.containsKey(sp)){
					map.put(sp, new Tuple<Integer, Integer>(0, 0));
				}
				if(mn == 1){
					/* New household. Add household AND individual. */
					map.put(sp, new Tuple<Integer, Integer>(
							map.get(sp).getFirst()+1, 
							map.get(sp).getSecond()+1 ));
				} else{
					/* Just add individual. */
					map.put(sp, new Tuple<Integer, Integer>(
							map.get(sp).getFirst(), 
							map.get(sp).getSecond()+1 ));
				}
				
				counter.incCounter();
			}
		} catch (IOException e) {
			throw new RuntimeException("Could not read from BufferedReader " + inputFile);
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				throw new RuntimeException("Could not close BufferedReader " + inputFile);
			}
		}
		
		LOG.info("Writing output...");
		BufferedWriter bw = IOUtils.getBufferedWriter(outputFile);
		try {
			bw.write("SPCode,Hhs,Persons");
			bw.newLine();
			for(Id<Object> id : map.keySet()){
				bw.write(id.toString());
				bw.write(",");
				bw.write(String.valueOf(map.get(id).getFirst()));
				bw.write(",");
				bw.write(String.valueOf(map.get(id).getSecond()));
				bw.newLine();				
			}
		} catch (IOException e) {
			throw new RuntimeException("Could not write to BufferedWriter " + outputFile);
		} finally {
			try {
				bw.close();
			} catch (IOException e) {
				throw new RuntimeException("Could not close BufferedWriter " + outputFile);
			}
		}
		
		Header.printFooter();
	}

}

