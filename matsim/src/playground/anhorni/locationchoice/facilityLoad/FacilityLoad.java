/* *********************************************************************** *
 * project: org.matsim.*
 * FacilityLoad.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.anhorni.locationchoice.facilityLoad;

public class FacilityLoad {
	
	private int [] arrivals = null;
	private int [] departures = null;
	private int [] load = null;
	
	private int numberOfTimeBins = 0;
	
	// visitors which are included in the penalty calculation (arrival before 24:00)
	private int numberOfVisitorsPerDay = 0;
	
	// including also visitors which arrive after 24:00
	private int allVisitors = 0;
	
	private int scaleNumberOfPersons = 1;
	
	// ----------------------------------------------------------------------
		
	FacilityLoad(int numberOfTimeBins) {
		this.numberOfTimeBins = numberOfTimeBins;
		this.arrivals = new int [numberOfTimeBins];
		this.departures = new int [numberOfTimeBins];
		this.load = new int [numberOfTimeBins];
		

		for (int i = 0; i < numberOfTimeBins; i++){
			this.arrivals[i] = 0;
			this.departures[i] = 0;
			this.load[i] = 0;
		}
	}
	
	private void calculateFacilityLoad24() {
		int numberOfVisitors = 0;
		for (int i=0; i<this.numberOfTimeBins; i++) {
			numberOfVisitors += this.arrivals[i];
			this.load[i] = numberOfVisitors*this.scaleNumberOfPersons;
			numberOfVisitors -= this.departures[i];
		}
	}
	
	
	// time in seconds from midnight
	public void addArrival(double time) {
		
		
		// we do not handle times > 24h
		// we do not care about #arrivals==#departures after the last time bin
		if (time > 24.0*3600.0) {
			return;
		}
		int timeBinIndex=Math.min(this.numberOfTimeBins-1, (int)(time/(3600/(this.numberOfTimeBins/24))));		
		this.arrivals[timeBinIndex]+=1;
		this.addToVisitorsPerDay(this.scaleNumberOfPersons);
		this.addToAllVisitors(this.scaleNumberOfPersons);
	}
	
	public void addDeparture(double time) {
		// we do not handle times > 24h
		// we do not care about #arrivals==#departures after the last time bin
		if (time > 24.0*3600.0) {
			return;
		}
		int timeBinIndex=Math.min(this.numberOfTimeBins-1, (int)(time/(3600/(this.numberOfTimeBins/24))));
		this.departures[timeBinIndex]+=1;
	}
	
	public void addToAllVisitors(int scaleNumberOfPersons) {
		this.allVisitors += scaleNumberOfPersons;
	}
	
	public void addToVisitorsPerDay(int scaleNumberOfPersons) {
		this.numberOfVisitorsPerDay += scaleNumberOfPersons;
	}

	public void setLoad(int [] load) {
		this.load = load;
	}

	public int [] getLoad() {
		return load;
	}
	
	public double getLoadPerHour(int hour) {
		double hourlyLoad = 0.0;

		for (int i = 0; i<4 ; i++) {
			int index = hour*4 + i;
			hourlyLoad += this.load[index];
		}
		return hourlyLoad/4;
	}
	
	public int getNumberOfVisitorsPerDay() {
		return numberOfVisitorsPerDay;
	}

	public void setNumberOfVisitorsPerDay(int numberOfVisitorsPerDay) {
		this.numberOfVisitorsPerDay = numberOfVisitorsPerDay;
	}

	public int getAllVisitors() {
		return allVisitors;
	}

	public void setAllVisitors(int allVisitors) {
		this.allVisitors = allVisitors;
	}

	public void finish() {
		this.calculateFacilityLoad24();
	}
	
	public void reset() {
		for (int i=0; i<this.numberOfTimeBins; i++) {
			this.arrivals[i] = 0;
			this.departures[i] = 0;
			this.load[i] = 0;
			this.numberOfVisitorsPerDay = 0;
			this.allVisitors = 0;
		}
	}
}
