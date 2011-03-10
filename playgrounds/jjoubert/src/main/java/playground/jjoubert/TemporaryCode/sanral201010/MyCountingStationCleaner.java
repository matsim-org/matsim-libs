/* *********************************************************************** *
 * project: org.matsim.*
 * cleanCountingStations.java
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

package playground.jjoubert.TemporaryCode.sanral201010;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import org.matsim.counts.MatsimCountsReader;
import org.xml.sax.SAXException;

import playground.jjoubert.Utilities.FileSampler.MyFileFilter;

/**
 * Class to remove {@link Count} stations from {@link Counts} files.
 */
public class MyCountingStationCleaner {
	private Logger log = Logger.getLogger(MyCountingStationCleaner.class);
	private Map<Id, Id> linkMap;
	

	/**
	 * Main method creates and runs the counting station cleaner.
	 * @param args a {@link String} array containing the following required 
	 * 		arguments:
	 * <ol>
	 * 	<li> <b>root</b> the absolute path of the folder in which the {@link Counts} 
	 * 		files are located.
	 * 	<li> <b>linkFilename</b> the absolute path of the file that has, for each
	 * 		{@link Count} (CsId) a link Id (LocId). This file is also used to
	 * 		identify which counting stations to retain.
	 * 	<li> <b>networkFilename</b> the {@link Network} used to identify the 
	 * 		link Ids. The associated {@link Link}'s midpoint will be used as
	 * 		the coordinate for the counting station.
	 * </ol>
	 */
	public static void main(String[] args) {
		String root;
		String linkFilename;
		String networkFilename;
		if(args.length == 3){
			root = args[0];
			linkFilename = args[1];
			networkFilename = args[2];
		} else{
			throw new IllegalArgumentException("Incorrect number of arguments passed.");
		}
		
		MyCountingStationCleaner ccs = new MyCountingStationCleaner();
		
		Scenario sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		NetworkReaderMatsimV1 nr = new NetworkReaderMatsimV1(sc);
		try {
			nr.parse(networkFilename);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		ccs.readLinkIds(linkFilename);
		ccs.cleanCounts(root, sc.getNetwork());
	}

	
	public MyCountingStationCleaner() {
	
	}
	
	
	/**
	 * 
	 * @param linkFile
	 */
	private void readLinkIds(String linkFile){
		log.info("Reading link Ids from " + linkFile + "...");
		linkMap = new HashMap<Id, Id>();
		try {
			BufferedReader br = IOUtils.getBufferedReader(linkFile);
			try{
				String line = null;
				while((line = br.readLine()) != null){
					String [] entry = line.split(",");
					if(entry.length == 2){
						Id station = new IdImpl(entry[0]);
						Id link = new IdImpl(entry[1]);
						linkMap.put(station, link);
					}else{
						log.error("There seems to be something wrong with line " + line);							
					}
				}
			}finally{
				br.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("Done.");
	}


	
	private void cleanCounts(String root, Network network){
		
		log.info("Cleaning counts files from " + root + "...");
		// Check that root is actually a folder.
		File folder = new File(root);
		if(!folder.exists() || !folder.isDirectory()){
			throw new RuntimeException("Can not read from the directory " + root);
		}
		
		File[] files = folder.listFiles(new MyFileFilter(".xml.gz"));
		for (File file : files) {
			Counts cs = new Counts();
			MatsimCountsReader cr = new MatsimCountsReader(cs);

			// Create a duplicate counting station.
			Counts csNew = new Counts();
			
			try {
				cr.parse(file.getAbsolutePath());
				csNew.setName(cs.getName());
				csNew.setYear(cs.getYear());
				Set<Id> all = cs.getCounts().keySet();
				/*
				 * Now, clean the counting stations.
				 */
				for (Id id : all) {
					if(linkMap.containsKey(id)){
						// It is a station that must remain.
						Count c = cs.getCount(id);
						Count cNew = csNew.createCount(linkMap.get(id), c.getCsId());
												
						// Get the coordinate. First remove the suffix from the Id.
						cNew.setCoord(network.getLinks().get(linkMap.get(id)).getCoord());

						// Transfer all the volumes.
						for(int i = 1; i <= 24; i++ ){
							cNew.createVolume(i, c.getVolume(i).getValue());
						}
					}
				}
				/*
				 * Write the cleaned counting stations.
				 */
				CountsWriter cw = new CountsWriter(csNew);
				cw.write(file.getAbsolutePath());				
				
			} catch (SAXException e) {
				e.printStackTrace();
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		log.info("Done.");
	}

}
