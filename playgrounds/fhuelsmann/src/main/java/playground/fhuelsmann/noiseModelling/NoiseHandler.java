/* 
*********************************************************************** *
 * project: org.matsim.*
 * NoiseTool.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *  
This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */


package playground.fhuelsmann.noiseModelling;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.LinkImpl;


	
public class NoiseHandler implements LinkLeaveEventHandler {

	private final Network network;
	// private final EventsManager NoiseEventsManager;

	private Map<Id, Map<String, double[]>> linkId2timePeriod2trafficInfo = new TreeMap<Id, Map<String, double[]>>();
	private Map<Id, List<Double>> linkTimes = new TreeMap<Id, List<Double>>();
	private Map <Id,double[][]> linkId2hour2vehicles = new TreeMap <Id, double[][]> ();

	// Constructor
	public NoiseHandler(Network network) {
		this.network = network;
	}

	@Override
	public void reset(int iteration) {
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		// System.out.println("leaveEvent");
		/*-------*/
		Id personId = event.getDriverId();
		Id linkId = event.getLinkId();
		/*-------*/
		double time = event.getTime();
		int timeClass = calculateTimeClass(time);
		String timePeriod = timeClassToTimePeriode(timeClass);
		/*-------*/
		LinkImpl link = (LinkImpl) this.network.getLinks().get(linkId);

		double freeSpeedInMs = link.getFreespeed(time);
		double freeSpeedInKmh = freeSpeedInMs * 3.6;
		
		/*********calculation of vehicles per hour**********/
		if(!linkId2hour2vehicles.containsKey(linkId)){
			double[][] hour2vehicles = new double[24][2];
			for(int i=0;i<24;++i){
				hour2vehicles[i][0] = 0.0 ; //first element includes the total number of vehicles
				hour2vehicles[i][1] = 0.0 ; // the second element includes the number of heavyduty vehicles 
			}
			linkId2hour2vehicles.put(linkId, hour2vehicles);
		}
		
		int index = timeClass - 1 ;
		if(index<24){
			if(personId.toString().contains("gv_")){ // check if it is a heavy duty vehicle
				++ linkId2hour2vehicles.get(linkId)[index][0] ;
				++ linkId2hour2vehicles.get(linkId)[index][1] ;
			}
			else{
				++ linkId2hour2vehicles.get(linkId)[index][0] ;
			}
		}
		/********calculation of vehicles per hour***********/
		
		// store the needed information about the traffic: 
		//freespeed [0]
		// number of total vehicles [1]
		// and heavy duty vehicles [2] 
		// if the linkId doesn't exist in the map
		if (!linkId2timePeriod2trafficInfo.containsKey(linkId)) {
			double[] trafficInfo = new double[3];
			// initialize the array
			trafficInfo[0] = freeSpeedInKmh; // the first element of the array
												// contains freespeed
			trafficInfo[1] = 1.0; // the second element of the array contains
									// the total number of vehicles

			if (personId.toString().contains("gv_")) {
				trafficInfo[2] = 1.0; // the third element contains the number
										// of heavy duty vehicles
			} else {
				trafficInfo[2] = 0.0;
			}
			Map<String, double[]> timeToTrafficInfo = new TreeMap<String, double[]>();
			timeToTrafficInfo.put(timePeriod, trafficInfo);
			linkId2timePeriod2trafficInfo.put(linkId, timeToTrafficInfo);
		} else {
			if (!linkId2timePeriod2trafficInfo.get(linkId).containsKey(timePeriod)) {
				double[] trafficInfo = new double[3];
				trafficInfo[0] = freeSpeedInKmh; // the first element of the
													// array contains freespeed
				trafficInfo[1] = 1.0; // the second element of the array
										// contains the total number of
										// transporters
				if (personId.toString().contains("gv_")) {
					trafficInfo[2] = 1.0; // the third element contains the
											// number of heavy transporters
				} else {
					trafficInfo[2] = 0.0;
				}
				linkId2timePeriod2trafficInfo.get(linkId).put(timePeriod, trafficInfo);
			} else {
				if (personId.toString().contains("gv_")) {
					++linkId2timePeriod2trafficInfo.get(linkId).get(timePeriod)[2];
				}
				++linkId2timePeriod2trafficInfo.get(linkId).get(timePeriod)[1];
			}
		}
	}

	private int calculateTimeClass(double time) {
		double timeClass = time / 3600;
		int timeClassrounded = (int) timeClass + 1;
		return timeClassrounded;
	}

	private String timeClassToTimePeriode(int timeClass) {
		if (timeClass >= 6 && timeClass < 18) {
			return "Day";
		} else if (timeClass >= 18 && timeClass <= 22) {

			return "Evening";
		} else {

			return "Night";

		}

	}
	
	public Map<Id, Map<String, double[]>> getlinkId2timePeriod2TrafficInfo() {
		return linkId2timePeriod2trafficInfo;
	}

	public Map<Id, List<Double>> getlinkTimes() {
		return linkTimes;
	}
	public Map <Id,double [][]> getlinkId2hour2vehicles(){
		return linkId2hour2vehicles;
	}

}

