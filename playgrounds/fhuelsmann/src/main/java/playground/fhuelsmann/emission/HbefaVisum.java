/* *********************************************************************** *
 * project: org.matsim.*
 * FhEmissions.java
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
 *                                                                         
 * *********************************************************************** */
package playground.fhuelsmann.emission;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;

public class HbefaVisum {

	private final VisumObject[] roadTypes = new VisumObject[100];
	Map<Id, Map<Id, Collection<SingleEvent>>> map = new TreeMap<Id,Map<Id, Collection<SingleEvent>>>();
	
	public HbefaVisum(Map<Id, Map<Id, Collection<SingleEvent>>> map) {
		super();
		this.map = map;
	}

	public void createRoadTypes(String filename){

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
				    
				       	String[] array = strLine.split(",");
				    	
				    	VisumObject obj = new VisumObject(Integer.parseInt(array[0]), array[2]);
				    	this.roadTypes[obj.getVISUM_RT_NR()] = obj;
				    }
				    //Close the input stream
				    in.close();
				    }catch (Exception e){//Catch exception if any
				      System.err.println("Error: " + e.getMessage());
				    }
		}
		
	public void printHbefaVisum(){
			
			for(int i=0;i<100; i++){
		
				VisumObject obj = roadTypes[i];
				
				System.out.println("\nVISUM_RT_NR :" + obj.getVISUM_RT_NR() +
									"\nHBEFA_RT_NR :" + obj.getHBEFA_RT_NR() );								
			}
	}

	public  String findHbefaFromVisum(int VisumRoadTypeNumber){
		return this.roadTypes[VisumRoadTypeNumber].getHBEFA_RT_NR();
	}
	
	
	public void createMapWithHbefaRoadTypeNumber(){

	for(Entry<Id, Map<Id, Collection<SingleEvent>>> LinkIdEntry : this.map.entrySet()){	 
		 for (Iterator iter = LinkIdEntry.getValue().
	    			entrySet().iterator(); iter.hasNext();) {
				Map.Entry entry = (Map.Entry) iter.next();
 				LinkedList value = (LinkedList)entry.getValue();	
		 				
		 			SingleEvent singleEvent = (SingleEvent) value.pop();
		 			HbefaTable hbefaTable = new HbefaTable();
		 			String a = findHbefaFromVisum(singleEvent.getRoadType());
		 			singleEvent.setHbefa_Road_type(Integer.parseInt(a));
		 			value.push(singleEvent);
		 			
		 			
		 			}
		 }
	}
	
	public void printTable(){
	
		for(Entry<Id, Map<Id, Collection<SingleEvent>>> LinkIdEntry : this.map.entrySet()){	 
			 for (Iterator iter = LinkIdEntry.getValue().
		    			entrySet().iterator(); iter.hasNext();) {
					Map.Entry entry = (Map.Entry) iter.next();
	 				LinkedList value = (LinkedList)entry.getValue();
	 				
		 		try{ 
		 				SingleEvent obj = (SingleEvent) value.pop();
		 						 				
		 				String activity = obj.getActivity();
		 				double travelTime = obj.getTravelTime();
		 				double v_mean = obj.getAverageSpeed();
		 				Id Person_id = obj.getPersonal_id();
		 				Id linkId_Veh_Id = obj.getLink_id();
		 				double length = obj.getLinkLength();
		 			
		 				
		 			System.out.println("\n"+activity 
						+"\nTravelTime :" + travelTime 
						+"\nAverageSpeed :" + v_mean
						+"\nLinkId : " + linkId_Veh_Id 
						+"\nPersonId :" + Person_id 
						+"\nLinklength : "+ length
						+"\nHbefaNr :"+ obj.getHbefa_Road_type());
		 				
		 				}catch(Exception e){}
		 			}
		    	}
		   }
		   	
}
		
		
	


