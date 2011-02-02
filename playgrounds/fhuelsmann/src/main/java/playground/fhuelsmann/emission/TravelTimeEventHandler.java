package playground.fhuelsmann.emission;
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
 * *********************************************************************** */


import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;




public class TravelTimeEventHandler implements LinkEnterEventHandler,LinkLeaveEventHandler, 
AgentArrivalEventHandler,AgentDepartureEventHandler,ActivityEndEventHandler,ActivityStartEventHandler {


	private final Network network;
	private  HbefaObject[][] HbefaTable;

	
	
	private LinkAccountAnalyseModul linkAccountAnalyseModul = new LinkAccountAnalyseModul();


	public LinkAccountAnalyseModul getLinkAccountAnalyseModul() {
		return linkAccountAnalyseModul;
	}

	public void setLinkAccountAnalyseModul(
			LinkAccountAnalyseModul linkAccountAnalyseModul) {
		this.linkAccountAnalyseModul = linkAccountAnalyseModul;
	}

	public TravelTimeEventHandler(final Network network,HbefaObject[][] HbefaTable) {
		this.HbefaTable = HbefaTable;
		this.network = network;
	}

	private final Map<Id, Double> linkenter = new TreeMap<Id, Double>();
	private final Map<Id, Double> activityend = new TreeMap<Id, Double>();
	private final Map<Id, Double> activitystart = new TreeMap<Id, Double>();
	private final Map<Id, Double> agentarrival = new TreeMap<Id, Double>();
	private final Map<Id, Double> agentdeparture = new TreeMap<Id, Double>();





	public Map<Id,Map<Integer,DistanceObject>> coldDistance  =
		new TreeMap<Id,Map<Integer,DistanceObject>>();

	public Map<Id,Map<Integer,ParkingTimeObject>> parkingTime  =
		new TreeMap<Id,Map<Integer,ParkingTimeObject>>();


	public void reset(int iteration) {

		this.linkenter.clear();
		this.agentarrival.clear();
		this.agentdeparture.clear();
		System.out.println("reset...");
	}

	public void handleEvent(LinkEnterEvent event) {
//		String id = event.getPersonId().toString();
//		Id onelink = event.getLinkId();
	//	if (onelink.equals(new IdImpl("590000822"))){
//			System.out.println(onelink);
//			if(id.contains("testVehicle")){
				this.linkenter.put(event.getPersonId(), event.getTime());
			}
//		}
//	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
//		String id = event.getPersonId().toString();
//		if(id.contains("testVehicle")){
		this.activityend.put(event.getPersonId(), event.getTime());
		Id personId= event.getPersonId();
		Id linkId = event.getLinkId();

		// cold start emissions: parking time calculation: time difference between activity start and activity end	
		LinkImpl link = (LinkImpl) this.network.getLinks().get(linkId);

		if(this.parkingTime.get(personId)!=null){


			String temp1 = event.getAttributes().toString();
			String[] temp2 = temp1.split(",");

			ParkingTimeObject object = 
				new ParkingTimeObject(event.getPersonId(),event.getTime(),temp2[1].split("=")[1]);
			this.parkingTime.get(personId).put((parkingTime.get(personId).size()),object );}

		else{

			String temp1 = event.getAttributes().toString();
			String[] temp2 = temp1.split(",");

			ParkingTimeObject object = 
				new ParkingTimeObject(event.getPersonId(),event.getTime(),temp2[1].split("=")[1]);

			Map<Integer,ParkingTimeObject> tempMap = 
				new TreeMap<Integer,ParkingTimeObject>();
				tempMap.put(0, object);
				parkingTime.put(personId, tempMap);}

		}
//	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
//		String id = event.getPersonId().toString();
//		Id onelink = event.getLinkId();
//		if (onelink == new IdImpl("590000822")){
//		if(id.contains("testVehicle")){
		this.activitystart.put(event.getPersonId(), event.getTime());
		Id personId= event.getPersonId();
		Id linkId = event.getLinkId();

		//cold start emissions: Distance calculation: Length of links between two activities 
		LinkImpl link = (LinkImpl) this.network.getLinks().get(linkId);

		if(this.coldDistance.get(personId)!=null){

			String temp1 = event.getAttributes().toString();
			String[] temp2 = temp1.split(",");


			DistanceObject object = 
				new DistanceObject(temp2[1].split("=")[1],link.getLength(),event.getPersonId(),event.getLinkId());
			this.coldDistance.get(personId).put((coldDistance.get(personId).size()),object );}

		else{
			String temp1 = event.getAttributes().toString();
			String[] temp2 = temp1.split(",");

			DistanceObject object = 
				new DistanceObject(temp2[1].split("=")[1],link.getLength(),event.getPersonId(),event.getLinkId());

			Map<Integer,DistanceObject> tempMap = 
				new TreeMap<Integer,DistanceObject>();
			tempMap.put(0, object);
			coldDistance.put(personId, tempMap);}

		// cold start emissions: parking time calculation: time difference between activity start and activity end	
		if(this.parkingTime.get(personId)!=null){


			String temp1 = event.getAttributes().toString();
			String[] temp2 = temp1.split(",");

			ParkingTimeObject object = 
				new ParkingTimeObject(event.getPersonId(),event.getTime(),temp2[1].split("=")[1]);
			this.parkingTime.get(personId).put((parkingTime.get(personId).size()),object );}

		else{

			String temp1 = event.getAttributes().toString();
			String[] temp2 = temp1.split(",");

			ParkingTimeObject object = 
				new ParkingTimeObject(event.getPersonId(),event.getTime(),temp2[1].split("=")[1]);

			Map<Integer,ParkingTimeObject> tempMap = 
				new TreeMap<Integer,ParkingTimeObject>();
				tempMap.put(0, object);
				parkingTime.put(personId, tempMap);}
		}
//		}
//	}

	public void handleEvent(AgentArrivalEvent event) {
//		String id = event.getPersonId().toString();
		Id onelink = event.getLinkId();
		if (onelink == new IdImpl("590000822")){
//			if(id.contains("testVehicle")){
				this.agentarrival.put(event.getPersonId(), event.getTime());
			}
		}
		
//	}

	public void handleEvent(AgentDepartureEvent event) {
//		String id = event.getPersonId().toString();
		Id onelink = event.getLinkId();
			if (onelink == new IdImpl("590000822")){
//				if(id.contains("testVehicle")){
					this.agentdeparture.put(event.getPersonId(), event.getTime());
				}
		}
//	}
		



	public void handleEvent(LinkLeaveEvent event) {	
	;

//		String id = event.getPersonId().toString();
//		Id onelink = event.getLinkId();
//		if (onelink.equals(new IdImpl("590000822"))){
//				if(id.contains("testVehicle")){
				Id personId= event.getPersonId();
				Id linkId = event.getLinkId();

		//get attributes of the network per link

		LinkImpl link = (LinkImpl) this.network.getLinks().get(linkId);
		double distance = link.getLength();
		
//		String roadType = link.getType();
//		int roadType = Integer.parseInt(roadTypes);
		int freeVelocity = (int) link.getFreespeed();
		int roadType= 55;

	
		if (this.linkenter.containsKey(event.getPersonId())) {						
			//with activity
			if (this.agentarrival.containsKey(personId)) {

				double enterTime = this.linkenter.get(personId);
				double arrivalTime = this.agentarrival.get(personId);
				double departureTime = this.agentdeparture.get(personId);

				double travelTime = event.getTime() - enterTime -departureTime+ arrivalTime;
				double averageSpeed=(distance/1000)/(travelTime/3600);

				this.agentarrival.remove(personId);
			
				linkAccountAnalyseModul.bearbeite(travelTime, linkId, averageSpeed,roadType, freeVelocity, distance, HbefaTable);	
				}
		

				
			// if 	(this.agentarrival.containsKey(personId)) is not the case

			else { // without activity


				double enterTime = this.linkenter.get(personId);
				double travelTime = event.getTime() - enterTime;
				double averageSpeed=(distance/1000)/(travelTime/3600);

				this.agentarrival.remove(personId);					
				linkAccountAnalyseModul.bearbeite(travelTime, linkId, averageSpeed,roadType, freeVelocity,distance,HbefaTable);	
					
				}
			}
				}
//	}
//	}

	public Map<Id, Map<Integer, DistanceObject>> getCold() {
		return coldDistance;
	}

	public void setCold(Map<Id, Map<Integer, DistanceObject>> cold) {
		this.coldDistance = cold;
	}


	//Cold start emissions: Distance calculation
	public void printTable(){

		String result ="";

		for(Entry<Id, Map<Integer, DistanceObject>> LinkIdEntry : coldDistance.entrySet()){
			double tempresult=0.0;
			for (Iterator iter = LinkIdEntry.getValue().
					entrySet().iterator(); iter.hasNext();) {
				Map.Entry entry = (Map.Entry) iter.next();
				DistanceObject value = (DistanceObject)entry.getValue();
				{
					try{ 
						tempresult+=value.getDistance();
						value.setSumDistance(tempresult);
						result =  
							value.getDistance() +
							"\t \t" + value.getPersonId() +
							"\t \t" + value.getLinkId() +
							"\t \t" + value.getActivity() +
							"\t \t" + value.getSumDistance() +"\n"

							+ result;

					}catch(Exception e){}
				}
			}

			try {

				// Create file 
				FileWriter fstream = 
					new FileWriter("C:/Users/Elias/matsim/matsim/output/outColdDistance.txt");
				BufferedWriter out = new BufferedWriter(fstream);
				out.write("Distance \t  PersonId \t \t \t \t LinkId \t \t Activity \n"   
						+ result);
				//Close the output stream
				out.close();
			}catch (Exception e){//Catch exception if any
				System.err.println("Error: " + e.getMessage());
			}

		}}



	//Cold start emissions: Parking time calculation
	public void printTable2(){

		String result ="";

		for(Entry<Id, Map<Integer, ParkingTimeObject>> LinkIdEntry : parkingTime.entrySet()){
			int count=0;
			boolean start=false;

			double tempresult=0.0;
			for (Iterator iter = LinkIdEntry.getValue().
					entrySet().iterator(); iter.hasNext();) {
				Map.Entry entry = (Map.Entry) iter.next();
				ParkingTimeObject value = (ParkingTimeObject)entry.getValue();
				{
					try{ 
						double timedifference=0.0; 

						if (value.getActivity().equals("actstart")) start=true;
						if (value.getActivity().equals("actend") && start){ 
							timedifference =  value.getTime() - parkingTime.get((LinkIdEntry.getKey())).get(count-1).getTime() ; 
							start=false;}

						value.setTimedifference(timedifference);
						
						count++;
						result+=  
							value.getPersonId() +
							"\t " +value.getTime() +
							"\t " + timedifference +
							"\t  \t \t"+ value.getActivity() +
							"\n";

					}catch(Exception e){}
				}
			}

			try {

				// Create file 
				FileWriter fstream = 
					new FileWriter("C:/Users/Elias/matsim/matsim/output/outColdParking.txt");
				BufferedWriter out = new BufferedWriter(fstream);
				out.write("PersonId \t  \t \t Time \t \t  TimeDifference \t   Activity\n"   
						+ result);
				//Close the output stream
				out.close();
			}catch (Exception e){//Catch exception if any
				System.err.println("Error: " + e.getMessage());
			}

		}



	}



}
