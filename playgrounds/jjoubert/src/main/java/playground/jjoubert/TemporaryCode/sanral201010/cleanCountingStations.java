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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import org.matsim.counts.MatsimCountsReader;
import org.xml.sax.SAXException;

import playground.jjoubert.Utilities.FileSampler.MyFileFilter;

/**
 * Class to remove {@link Count} stations from {@link Counts} files.
 */
public class cleanCountingStations {
	private Logger log = Logger.getLogger(cleanCountingStations.class);
	private List<Id> removeList;
	private Map<Id, Id> linkMap;
	private Map<Id, String[]> positionMap;
	

	/**
	 * Main method creates and runs the counting station cleaner.
	 * @param args a {@link String} array containing the following required 
	 * 		arguments:
	 * <ol>
	 * <li> <b>root</b> the absolute path of the folder in which the {@link Counts} 
	 * 		files are located.
	 * <li> <b>listFile</b> the absolute path of the flat file indicating which
	 * 		{@link Count} stations to remove.  
	 * </ol>
	 */
	public static void main(String[] args) {
		String root;
		String cleanFile;
		String positionFile;
		String linkFile;
		if(args.length == 4){
			root = args[0];
			cleanFile = args[1];
			if(cleanFile.equalsIgnoreCase("null")){
				cleanFile = null;
			}
			positionFile = args[2];
			if(positionFile.equalsIgnoreCase("null")){
				positionFile = null;
			}
			linkFile = args[3];
			if(linkFile.equalsIgnoreCase("null")){
				linkFile = null;
			}
		} else{
			throw new IllegalArgumentException("Incorrect number of arguments passed.");
		}
		
		cleanCountingStations ccs = new cleanCountingStations();
		
		ccs.readFiles(cleanFile, positionFile, linkFile);
		ccs.cleanCounts(root);

		
	}
	
	public cleanCountingStations() {
		
		
	}
	
	private void readFiles(String cleanFile, String positionFile, String linkFile){
		/*
		 * First read in the Ids of the links that must be removed.
		 */
		if(cleanFile != null){
			log.info("Reading the counting stations to remove from " + cleanFile + "...");
			removeList = new ArrayList<Id>();
			try {
				BufferedReader br = IOUtils.getBufferedReader(cleanFile);
				try{
					String line = null; // No header in file.
					while((line = br.readLine()) != null){
						removeList.add(new IdImpl(line));
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
		
		/*
		 * Reading the file that contains the coordinates of each station, as
		 * well as the directions serviced by each station.
		 */
		if(positionFile != null){
			log.info("Reading position data from " + positionFile + "...");
			positionMap = new HashMap<Id, String[]>();
			try {
				BufferedReader br = IOUtils.getBufferedReader(positionFile);
				try{
					String line = br.readLine(); // Header in file.
					while((line = br.readLine()) != null){
						String[] entry = line.split(",");
						if(entry.length == 4 || entry.length == 5){
							positionMap.put(new IdImpl(entry[0]), entry);
						} else{
							log.error("The station " + line + " is probably incorrect.");
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
			log.info("Position map built.");			
		}

		/*
		 * Read the link Ids associated with each counting station.
		 */
		if(linkFile != null){
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
	}
	
	private void cleanCounts(String root){
		
		log.info("Cleaning counts files from " + root + "...");
		// Check that root is actually a folder.
		File folder = new File(root);
		if(!folder.exists() || !folder.isDirectory()){
			throw new RuntimeException("Can not read from the directory " + root);
		}
		List<Id> listNoLink = new ArrayList<Id>();
		List<Id> listNoCoord = new ArrayList<Id>();
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84", "WGS84_UTM35S");
		
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
					if(!removeList.contains(id)){
						// It is a station that must remain.
						Count c = cs.getCount(id);
						Count cNew = null;
						
						// Get the Link Id.
						if(linkMap.containsKey(id)){
							cNew = csNew.createCount(linkMap.get(id), c.getCsId());
						} else{
							listNoLink.add(id);
						}
						
						// Get the coordinate. First remove the suffix from the Id.
						String s = id.toString().substring(0, id.toString().length()-1);
						Id idPlain = new IdImpl(s);
						if(cNew != null){
							if(positionMap.containsKey(idPlain)){
								String[] entry = positionMap.get(idPlain);
								String suffix = id.toString().substring(id.toString().length()-1, id.toString().length());
								Coord coordWGS = new CoordImpl(entry[1], entry[2]);
								Coord coordUTM = ct.transform(coordWGS);
								int xOffset = 0;
								int yOffset = 0;
								int index = 0;
								if(suffix.equalsIgnoreCase("a")){
									index = 3;
								} else if(suffix.equalsIgnoreCase("b")){
									index = 4;
								} else{
									log.error("Station suffix must be either `a' or `b', but is " + suffix);
								}
								String direction = entry[index];
								if(direction.equalsIgnoreCase("EAST")){
									yOffset = 30;
								} else if(direction.equalsIgnoreCase("WEST")){
									yOffset = -30;
								} else if(direction.equalsIgnoreCase("NORTH")){
									xOffset = -30;
								} else if(direction.equalsIgnoreCase("SOUTH")){
									xOffset = 30;
								} else{
									log.warn("Improper direction: " + direction);
								}
								coordUTM.setXY(coordUTM.getX() + xOffset, coordUTM.getY() + yOffset);
								cNew.setCoord(coordUTM);						
							}else{
								listNoCoord.add(id);
							}
							// Transfer all the volumes.
							for(int i = 1; i <= 24; i++ ){
								cNew.createVolume(i, c.getVolume(i).getValue());
							}
						}		
					}
				}
				CountsWriter cw = new CountsWriter(csNew);
				cw.write(file.getAbsolutePath());				
				
				/*
				 * Write the cleaned counting stations.
				 */
			} catch (SAXException e) {
				e.printStackTrace();
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if(listNoLink.size() > 0){
			log.warn("The following stations had no LinkId:");
			for(Id id : listNoLink){
				log.warn("   ..." + id.toString());
			}			
		}
		if(listNoCoord.size() > 0){
			log.warn("Thw following stations does not have a coordinate:");
			for(Id id : listNoCoord){
				log.warn("   ..." + id.toString());
			}			
		}
		
		log.info("Done.");
	}

}
