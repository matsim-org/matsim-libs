/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.julia.toi;

import org.matsim.api.core.v01.Coord;

public class Trip {

	Coord startCoordinates;
	Coord endCoordinates; 
	Double time;
	String legmode; 
	String actType; 
	int tripnumber;
	
	public Trip(Coord startCoordinates, Coord endCoordinates, Double time,
			String legmode, String actType, int tripnumber) {
		this.startCoordinates = startCoordinates;
		this.endCoordinates = endCoordinates;
		this.time = time;
		//car bike walk pt
		
		if(legmode.equals("car"))this.legmode="car";
		if(legmode.equals("bicycle/walking")) this.legmode="walk";
		if(legmode.equals("public transport")) this.legmode="pt";
		if(this.legmode==null){
			System.out.println("legmode not set" + legmode);
			this.legmode="car";
		}
		this.actType=actType;
		this.tripnumber=tripnumber;
	}

	public String getActivityType() {
		return actType;
	}

	public Coord getStartCoord() {
		return startCoordinates;
	}

	public String getLeg() {
		return legmode;
	}

	public double getTime() {
		return time;
	}

	public Coord getEndCoord() {
		return endCoordinates;
	}

	public int getNumber() {
		return tripnumber;
	}

}
