/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.santiago.counts;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

/**
 * Exports the coordinates of the links with counting stations.
 * The output can be used for generation of new count-files, when linkIds of the network have changed.
 * @author kturner
 */
public class ExportCsLinkIdInfo {
	
	private static final String svnWorkingDir = "../../shared-svn/"; 	
	private static final String workingDirInputFiles = svnWorkingDir + "Kai_und_Daniel/inputFromElsewhere/counts/" ;
	
	//Position (linkId) of Counting Stations
	private static final String CSIdFILE = workingDirInputFiles + "CSId-LinkId_merged.csv" ;
	
	//Network the CSIdFile correspondent to -> to generate the coordinates of links with counting stations.
	private static final String NETFILE = workingDirInputFiles + "santiago_merged_cl.xml.gz";	


	public static void main(String args[]){
		Map<String,Id<Link>> idMap = new TreeMap<String,Id<Link>>();	
		
		//Network-Stuff
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(NETFILE);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		idMap = createIdMap();
		exportCoordinates(idMap, scenario.getNetwork());
		
		System.out.println("### Done. ###");		
	}
	
	/**
	 * Exports the coordinates of Links with counting stations.
	 * Output can be used for 
	 * @param idMap
	 * @param network
	 */
	private static void exportCoordinates(Map<String, Id<Link>> idMap, Network network) {
		FileWriter writer;
		File file = new File(workingDirInputFiles + "CSLinkCoordinates.csv");
		File file2 = new File(workingDirInputFiles + "CSLinkCoordinates.txt");
		
		try {
			writer = new FileWriter(file);  

			writer.write("CS-Id;FromX;FromY;ToX;ToY" +System.getProperty("line.separator"));
			writer.flush();

			// close stream
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			writer = new FileWriter(file2);  

			writer.write("Generated code for creation of map (CSId2LinkId) by coordinates " +System.getProperty("line.separator"));
			writer.flush();

			// close stream
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//get Coordinates
		for (String csId : idMap.keySet()){
			double fromX = network.getLinks().get(idMap.get(csId)).getFromNode().getCoord().getX();
			double fromY = network.getLinks().get(idMap.get(csId)).getFromNode().getCoord().getY();
			double toX = network.getLinks().get(idMap.get(csId)).getToNode().getCoord().getX();
			double toY = network.getLinks().get(idMap.get(csId)).getToNode().getCoord().getY();
			System.out.println(csId + ": FromX: " + fromX + ": FromY: " + fromY + " ; toX: " + toX + " ; toY: " + toY  );		
			
			//write them to .csv-file
			try {
				writer = new FileWriter(file, true);  //true ---> write to end 
				writer.write(csId + ";" + fromX + ";" + fromY + ";" + toX + ";" + toY + System.getProperty("line.separator"));
				writer.flush();
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		    //and write them as command for class CreateCountingStations (just copy/paste) to .txt -file	
			try {
				writer = new FileWriter(file2, true);  //true ---> write to end 
				writer.write("csIdString2LinkCoordinates.put(\"" + csId + "\", new ArrayList<Double>(Arrays.asList(" + fromX + ", " + fromY + ", " + toX + ", " + toY + ")));" + System.getProperty("line.separator"));
				writer.flush();
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}

	/**
	* Reads CSIdFile and generates idMap <CS-Id (String), Link-Id> from the data.
	*/
	private static Map<String,Id<Link>> createIdMap(){
		
		Map<String,Id<Link>> idMap = new TreeMap<String,Id<Link>>();	
			BufferedReader r = IOUtils.getBufferedReader(CSIdFILE);
			
			try{
				String line = r.readLine();

				while((line = r.readLine()) != null){	
					String[] splittedLine = line.split(";");
					String cs_id = splittedLine[0];		//Name of CS in database
					String link_id = splittedLine[1];		//Link of CS


					if (cs_id != null){
						if (link_id.equalsIgnoreCase("") == false) {
							if(idMap.containsKey(cs_id) == false){
								idMap.put(cs_id, Id.create(link_id, Link.class));	
								System.out.println("added to map: " + cs_id );
							} else {
								System.out.println("key already exists!");
							}
						} else {
							System.out.println("CS with Id has no linkId: " + cs_id);
						}
					}
				}
			} catch(IOException e){
				e.printStackTrace();
			}
			return idMap;
		}
	
	
}
