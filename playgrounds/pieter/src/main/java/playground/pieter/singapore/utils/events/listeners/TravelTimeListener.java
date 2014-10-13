/* *********************************************************************** *
 * project: org.matsim.*
 * EventWriterXML.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

package playground.pieter.singapore.utils.events.listeners;

import java.io.BufferedWriter;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.events.Event;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.network.NetworkImpl;

public class TravelTimeListener implements BasicEventHandler {
	private BufferedWriter out = null;

	private final HashMap<String,Double> paxTravelTimes = new HashMap<>();
	public HashMap<String, Double> getPaxTravelTimes() {
		return paxTravelTimes;
	}
	private final HashMap<String,Double> paxTempTravelTimes = new HashMap<>();
	private final HashMap<String,String> paxModes = new HashMap<>();
	public HashMap<String, String> getPaxModes() {
		return paxModes;
	}
	private NetworkImpl network;
	public void reset(int iteration) {
	}


	public void handleEvent(Event event) {
		StringBuilder eventXML = new StringBuilder(180);
		Map<String, String> attr = event.getAttributes();
		String type = attr.get("type");
		String mode = "";
		if(type.equals("actend") || type.equals("actstart") || type.equals("departure")){
			String pax_idx = attr.get("person");
			double currentTime = 0;
			if (pax_idx != null){
				if(type.equals("departure"))
					mode += attr.get("legMode");
				if(type.equals("actend")){
					paxTempTravelTimes.put(pax_idx, Double.parseDouble(attr.get("time")));					
				}else{
					currentTime += Double.parseDouble(attr.get("time")) - paxTempTravelTimes.get(pax_idx);
//					paxTempTravelTimes.remove(pax_idx);					
				}
				if(paxTravelTimes.get(pax_idx) == null){
					paxTravelTimes.put(pax_idx, currentTime);
					paxModes.put(pax_idx, mode);
				}else{
					currentTime += paxTravelTimes.get(pax_idx);
					mode = paxModes.get(pax_idx) + mode;
					paxTravelTimes.put(pax_idx,currentTime);
					paxModes.put(pax_idx, mode);
					
				}
			}
			
		}

	}

	public TravelTimeListener(){
	}

	public void init() {

	}
	public static void main(String[] args){
		
	}

}
