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
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.vividsolutions.jts.index.bintree.Key;

public class VelocityAverageCalculate {

	public VelocityAverageCalculate(
			Map<String,Map<String, LinkedList<String>>> oldtravelTimesWithLengthAndAverageSpeed, String filename) {
		super();
		this.OldtravelTimesWithLengthAndAverageSpeed = oldtravelTimesWithLengthAndAverageSpeed;
		this.filename = filename;
	}

	// Sectionnumber,<objectId,RoadType>
	
	String filename;	
	
	private final Map<String,Map<String, LinkedList<String>>> OldtravelTimesWithLengthAndAverageSpeed;
	
	private final Map<String,Map<String, LinkedList<SingleEvent>>> mapOfSingleEvent
		= new TreeMap<String,Map<String, LinkedList<SingleEvent>>>();

	
	public void calculateAverageVelocity1() {

		try{

			FileInputStream fstream = new FileInputStream(filename);
		    // Get the object of DataInputStream
		    DataInputStream in = new DataInputStream(fstream);
	        BufferedReader br = new BufferedReader(new InputStreamReader(in));
		    String strLine;
		    //Read File Line By Line
		    br.readLine();
			    while ((strLine = br.readLine()) != null)   {
			    	//for all lines (whole text) we split the line to an array 
			    
			       	String[] array = strLine.split(";");

			       	String length = array[5];
			       	int freeVelocity;
			    	int roadSectionNr;
			    	int visumRoadType;
			    	String[] a = array[1].split(",");
			    	roadSectionNr = Integer.parseInt(a[0]);
			    	visumRoadType = Integer.parseInt(array[4]);
			    	freeVelocity = Integer.parseInt(array[6]);
	 				int roadSectionNumber = roadSectionNr;
	 				
			  	if(this.OldtravelTimesWithLengthAndAverageSpeed.get(roadSectionNr+"") !=null){
			       	Iterator iterator = this.OldtravelTimesWithLengthAndAverageSpeed.get(roadSectionNr+"").keySet().iterator();
		 				while (iterator.hasNext()) {
			 				String personalId =  (String) iterator.next();
			 			
			 			LinkedList list = this.OldtravelTimesWithLengthAndAverageSpeed.get(roadSectionNr+"").get(personalId);
			 				
			 			String v = (String) list.peek();
			 			String[] activityAndTraveltime=v.split(",");
				 		String travelTimeString = activityAndTraveltime[1];
				 		String activity = activityAndTraveltime[0];
						double length1= Double.parseDouble(length.split(",")[0]);
		 				double length2= Double.parseDouble(length.split(",")[1]);
		 				double length3 = length1 + (0.01* length2);
		 				
				 				
			 				double travelTimeDouble =  Double.parseDouble(travelTimeString);
			
				 				double averageSpeed =  length3 / (travelTimeDouble / 3600)  ;
				 				SingleEvent result = new SingleEvent(activity,travelTimeString,averageSpeed, 
				 						personalId,length3,roadSectionNumber,roadSectionNr,visumRoadType,activityAndTraveltime[2], freeVelocity);
						
				 				Map<String, LinkedList<SingleEvent>> map1 = this.mapOfSingleEvent.get(roadSectionNr+"");
				 			
				 				if (map1 != null){
									if (map1.get(personalId+"")!=null){
										map1.get(personalId+"").push(result);
										}else{
											LinkedList<SingleEvent> list1 = new LinkedList<SingleEvent>();
											list1.push(result);
											map1.put(personalId+"",list1);}}
								else{
									
											LinkedList<SingleEvent> list1 = new LinkedList<SingleEvent>();
											list1.push(result);
											Map<String, LinkedList<SingleEvent>> map2 = new TreeMap<String, LinkedList<SingleEvent>>();
											map2.put(personalId, list1);
											this.mapOfSingleEvent.put(roadSectionNr+"", map2);

										try{
											}catch(Exception e){
												System.out.println(e.getMessage());
											}
								}}
				    	}}//Close the input stream
			    in.close();
			    }catch (Exception e){//Catch exception if any
			      System.err.println("Error: " + e.getMessage());
			    }
	}
	

	public void printVelocities(){
		
		 final Map<String,Map<String, LinkedList<SingleEvent>>> map = this.mapOfSingleEvent;
		 
		 String result="";
		 
		 for(Entry<String, Map<String, LinkedList<SingleEvent>>> LinkIdEntry : map.entrySet()){	 
			 for (Iterator iter = LinkIdEntry.getValue().
		    			entrySet().iterator(); iter.hasNext();) {
		 				Map.Entry entry = (Map.Entry) iter.next();
		 				LinkedList value = (LinkedList)entry.getValue();	
		 					
		 			try{ 
		 				SingleEvent obj = (SingleEvent) value.pop();
		 				//System.out.println(obj);
		 				String activity = obj.getActivity();
		 				String travelTimeString = obj.getTravelTime();
//		 				String enterTimeString = obj.getEnterTime();
		 				double v_mean = obj.getAverageSpeed();
		 				String Person_id = obj.getPersonal_id();
		 				int linkId_Veh_Id = obj.getLink_id();
		 				double length = obj.getLinkLength();
		 				String enterTime=obj.getEnterTime();
		 				value.push(obj);
		 				//map.get(entry.getKey()).put(obj.getPersonal_id(), value);
		 			
		 			System.out.println("\t"+activity 
		 				+"\tEnterTime :" + enterTime
		 				+"\tTravelTime :" + travelTimeString 
 						+"\tAverageSpeed :" + v_mean
 						+"\tLinkId : " + linkId_Veh_Id 
 						+"\tPersonId :" + Person_id 
 						+"\tLinklength : "+ length);
		 			
		 			result = result +"\n" + "\t"+activity 
			 				+"\tEnterTime :" + enterTime
			 				+"\tTravelTime :" + travelTimeString 
	 						+"\tAverageSpeed :" + v_mean
	 						+"\tLinkId : " + linkId_Veh_Id 
	 						+"\tPersonId :" + Person_id 
	 						+"\tLinklength : "+ length;
		 				
		 				}catch(Exception e){}
		 			}
		    	}
		 
		try{
	  
	    // Create file 
	    FileWriter fstream = new FileWriter("../fhuelsmann/test/emissions/out.txt");
	        BufferedWriter out = new BufferedWriter(fstream);
	        out.write(result);
	    //Close the output stream
	    out.close();
			}catch (Exception e){//Catch exception if any
				System.err.println("Error: " + e.getMessage());
	    }
	}
		   	
	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public Map<String,Map<String, LinkedList<String>>> getOldtravelTimesWithLengthAndAverageSpeed() {
		return OldtravelTimesWithLengthAndAverageSpeed;
	}

	public Map<String,Map<String, LinkedList<SingleEvent>>> getmapOfSingleEvent() {
		return mapOfSingleEvent;
	}
	

}
