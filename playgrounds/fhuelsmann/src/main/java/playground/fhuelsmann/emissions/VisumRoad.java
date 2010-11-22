/* *********************************************************************** *
 * project: org.matsim.*
 * BKickControler
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

package playground.fhuelsmann.emissions;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.TreeMap;


public class VisumRoad {

	// for example <ObjectId ( from the file : Visum_Road_network_line_types)
	private final Map<String, String[]> VisumRoadNetwork = new TreeMap<String, String[]>();

	public Map<String, String[]> getVisumRoadNetwork(){
		return VisumRoadNetwork;}
	
	
	

	public void createVisumRoadNetworkMapFromVisumRoadFile(String filename) {
		
		//OBJECTID;Road_section_number;FROMNODENO;TONODENO;ROAD_TYPE_NO;LENGTH;V0PRT;VCUR_PRT_1		   
		try{
	
				FileInputStream fstream = new FileInputStream(filename);
			    // Get the object of DataInputStream
			    DataInputStream in = new DataInputStream(fstream);
		        BufferedReader br = new BufferedReader(new InputStreamReader(in));
			    String strLine;
			    //Read File Line By Line
			    while ((strLine = br.readLine()) != null)   {
			    	//each line (whole text) is split to an array
			    	String[] array = strLine.split(";");
			    	/* first cell in the array is the ObjectId (see file), ObjectId is unique and will be 
			    	used as key */
					this.VisumRoadNetwork.put(array[0].split(",")[0],array);
			    }
			    //Close the input stream
			    in.close();
			    }catch (Exception e){//Catch exception if any
			      System.err.println("Error: " + e.getMessage());
			    }
	}
}
