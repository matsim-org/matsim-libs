/* *********************************************************************** *
 * project: org.matsim.*
 * AfcChainIdentifier.java
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
package playground.jjoubert.projects.capeTownMultimodal.afc;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.utilities.Header;

/**
 * Identify different MyCiTi 'activity' chains in terms of connection patterns.
 * 
 * @author jwjoubert
 */
public class AfcChainIdentifier {
	final private static Logger LOG = Logger.getLogger(AfcChainIdentifier.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(AfcChainIdentifier.class.toString(), args);
		
		analyseChain(args[0], args[1]);
		
		Header.printFooter();
	}
	
	private static void analyseChain(String filename, String stopNameFile){
		LOG.info("Parsing chain types from " + filename);
		
		Map<String, String> personMap = new HashMap<String, String>();
		Map<Integer, Integer> cardMap = new TreeMap<>();
		Map<String, List<Integer>> stopMap = new TreeMap<>();
		
		Map<String, Integer> gtfsMap = AfcUtils.parseStopIdFromGtfs(stopNameFile);
		
		BufferedReader br = IOUtils.getBufferedReader(filename);
		Counter counter = new Counter("  lines # ");
		try{
			String line = br.readLine(); /* Header */
			while((line = br.readLine()) != null){
				String[] sa = line.split(",");
				String id = sa[0];
				
				int stopId = Integer.parseInt(sa[4]);
				String stopName = sa[5];
				if(!stopMap.containsKey(stopName)){
					List<Integer> list = new ArrayList<Integer>();
					stopMap.put(stopName, list);
				}
				if(!stopMap.get(stopName).contains(stopId)){
					stopMap.get(stopName).add(stopId);
				}
				
				/* Check the card number length. */
				int idLength = id.length();
				if(!cardMap.containsKey(idLength)){
					cardMap.put(idLength, 1);
				} else{
					int oldCount = cardMap.get(idLength);
					cardMap.put(idLength, oldCount+1);
				}
				
				String act = AfcUtils.getTransactionAbbreviation(sa[6]);
				if(!personMap.containsKey(id)){
					personMap.put(id, act);
				} else{
					String partial = personMap.get(id);
					personMap.put(id, partial + "-" + act);
				}
				counter.incCounter();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read from " + filename);
		} finally{
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + filename);
			}
		}
		counter.printCounter();
		LOG.info("Done parsing chains. Total number of unique cards: " + personMap.size());
		
		Map<String, Integer> chainMap = new HashMap<String, Integer>();
		counter = new Counter("  persons # ");
		for(String pid : personMap.keySet()){
			String chain = personMap.get(pid);
			
			/*TODO Remove after debugging... */
			if(chain.contains("B-A-C-A")){
				LOG.info("Stop here for 'B-A-C-A' - " + pid);
			}
			
			
			if(!chainMap.containsKey(chain)){
				chainMap.put(chain, 1);
			} else{
				int oldCount = chainMap.get(chain);
				chainMap.put(chain, oldCount+1);
			}
			counter.incCounter();
		}
		counter.printCounter();
		
		/* Sort the chains by count. */
		Comparator<String> comparator = new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return -chainMap.get(o1).compareTo(chainMap.get(o2));
			}
		};
		Map<String, Integer> sortedMap = new TreeMap<>(comparator);
		sortedMap.putAll(chainMap);
		
		/* Report the different chains. */
		LOG.info("Activity chains observed:");
		for(String chain : sortedMap.keySet()){
			LOG.info("  " + chain + "  (" + sortedMap.get(chain) + ")");
		}
		
		/* Report the card number lengths. */
		LOG.info("Card number lengths:");
		for(int i : cardMap.keySet()){
			LOG.info(String.format("%4s: %d", String.valueOf(i), cardMap.get(i)));
		}
		
		/* Report stop IDs. */
		int stopsWithOneId = 0;
		int stopsWithTwoIds = 0;
		int stopsWithThreeIds = 0;
		int stopsWithFourIds = 0;
		int stopsWithMoreIds = 0;
		LOG.info("The following stop names have multiple IDs:");
		for(String stopName : stopMap.keySet()){
			List<Integer> list = stopMap.get(stopName);
			
			String s = "  " + stopName + ": ";
			for(Integer i : list){
				s += String.valueOf(i) + " ";
			}
			
			if(gtfsMap.containsKey(stopName)){
				s += "[" + gtfsMap.get(stopName) + "]";
			} else{
				s += "[!!]";
			}
			LOG.info(s);
			
			if(list.size() == 1){
				stopsWithOneId++;
			} else if(list.size() == 2){
				stopsWithTwoIds++;
			} else if(list.size() == 3){
				stopsWithThreeIds++;
			} else if(list.size() == 4){
				stopsWithFourIds++;
			} else{
				stopsWithMoreIds++;
				LOG.warn("Stop '" + stopName + "' has " + list.size() + " IDs.");
			}
		}
		LOG.info("-----------------------------------------");
		LOG.info("  Stops with 1: " + stopsWithOneId);
		LOG.info("  Stops with 2: " + stopsWithTwoIds);
		LOG.info("  Stops with 3: " + stopsWithThreeIds);
		LOG.info("  Stops with 4: " + stopsWithFourIds);
		LOG.info("  Stops with +: " + stopsWithMoreIds);
		LOG.info("-----------------------------------------");
	}
	

}
