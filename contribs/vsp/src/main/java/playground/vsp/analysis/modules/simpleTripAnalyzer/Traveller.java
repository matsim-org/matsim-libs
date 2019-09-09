/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.vsp.analysis.modules.simpleTripAnalyzer;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;

/**
 * @author droeder
 *
 */
public class Traveller{
	
	static final String HEADER = "id;tripIndex;maxTripIndex;" + Trip.HEADER;
	private List<Trip> trips = new ArrayList<Trip>();
	final Id id;
	int maxTripIndex = 0;
	
	Traveller(Id id){
		this.id = id;
	}
	
	void startTrip(double time, String mode, double dist){
		Trip t = new Trip();
		trips.add(t);
		t.start = time;
		t.mode = mode;
		t.dist = dist;
	}
	
	void passLink(double dist){
		Trip t = trips.get(trips.size()-1);
		t.dist += dist;
	}
	
	void endTrip(double time, double dist){
		Trip t = trips.get(trips.size()-1);
		t.dist += dist;
		t.end = time;
	}
	
	@Override
	public String toString(){
		StringBuffer b = new StringBuffer();
		for(int i = 0; i< trips.size(); i++){
			b.append(id.toString() + ";");
			b.append(i + ";");
			b.append(maxTripIndex + ";");
			b.append(trips.get(i).toString() + "\n");
		}
		return b.toString();
	}

	/**
	 * 
	 */
	public void setStuck() {
		trips.get(trips.size()-1).stuck = true;
	}
	
	public final List<Trip> getTrips(){
		return trips;
	}
}

