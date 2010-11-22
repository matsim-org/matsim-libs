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
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;

public class TravelTimes {
	private static TravelTimes instance;
	private Map<String, Double> travelTimeMap;
			
	private TravelTimes(){
		travelTimeMap = new TreeMap<String, Double>();
	}
	//singleton
	public static TravelTimes getInstance(){
		if(instance == null) {
			instance = new TravelTimes();
	    }
	    return instance;
	}
	
	//Synchronized method to add elements
	public synchronized void addElement(Id linkId, Id personId, double travelTime) {
		String key = String.valueOf(linkId) + ";" + String.valueOf(personId);
		if(travelTimeMap.containsKey(key)) {
			System.out.println("Fehler: Schlüssel schon vorhanden.");
		}
		else {
			travelTimeMap.put(key, travelTime);
		}
	}
	
	public synchronized Map<String, Double> getTravelTimes() {	
		return travelTimeMap;	
	}
	
	public synchronized void clear() {
		travelTimeMap.clear();
	}		
}
