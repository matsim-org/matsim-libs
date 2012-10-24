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

package org.matsim.contrib.locationchoice.facilityload;

//import org.apache.log4j.Logger;

public class FacilityLoad {
	
	//private final static Logger log = Logger.getLogger(FacilityLoad.class);
	
	private int [] arrivals = null;
	private int [] departures = null;
	private double [] load = null;
	
	private int numberOfTimeBins = 0;
	
	// visitors which are included in the penalty calculation (arrival before 24:00)
	private double numberOfVisitorsPerDay = 0.0;
	
	// including also visitors which arrive after 24:00
	private double allVisitors = 0.0;
	
	private double scaleNumberOfPersons = 1.0;
	
	// ----------------------------------------------------------------------
		
	FacilityLoad(int numberOfTimeBins, double scaleNumberOfPersons) {
		this.numberOfTimeBins = numberOfTimeBins;
		this.arrivals = new int [numberOfTimeBins];
		this.departures = new int [numberOfTimeBins];
		this.load = new double [numberOfTimeBins];
		this.scaleNumberOfPersons = scaleNumberOfPersons;

		for (int i = 0; i < numberOfTimeBins; i++){
			this.arrivals[i] = 0;
			this.departures[i] = 0;
			this.load[i] = 0.0;
		}
	}
	
	private void calculateFacilityLoad24() {
		//log.info("calculateFacilityLoad24");
		int numberOfVisitors = 0;
		for (int i = 0; i < this.numberOfTimeBins; i++) {
			numberOfVisitors += this.arrivals[i];
			this.load[i] = numberOfVisitors * this.scaleNumberOfPersons;
			numberOfVisitors -= this.departures[i];			
		}
	}
	
	public void addArrival(double time) {		
		this.addToAllVisitors(this.scaleNumberOfPersons);
		/* We do not handle times > 24h
		 * We do not care about #arrivals==#departures after the last time bin
		 */
		if (time > 24.0*3600.0) {		
			return;
		}
		int timeBinIndex = this.timeBinIndex(time);		
		this.arrivals[timeBinIndex] += 1;
		//log.info("arrival at: " + time + " bin: " + timeBinIndex);
		this.addToVisitorsPerDay(this.scaleNumberOfPersons);
	}
	
	public void addDeparture(double time) {
		/* We do not handle times > 24h
		 * We do not care about #arrivals==#departures after the last time bin
		 */
		if (time > 24.0*3600.0) {
			return;
		}
		int timeBinIndex = this.timeBinIndex(time);
		this.departures[timeBinIndex]+=1;
		//log.info("departure at: " + time + " bin: " + timeBinIndex);
	}
	
	public void addToAllVisitors(double scaleNumberOfPersons) {
		this.allVisitors += scaleNumberOfPersons;
	}
	
	public void addToVisitorsPerDay(double scaleNumberOfPersons) {
		this.numberOfVisitorsPerDay += scaleNumberOfPersons;
	}

	public double [] getLoad() {
		return load;
	}
	
	public double getLoadPerHour(int hour) {
		double hourlyLoad = 0.0;

		for (int i = 0; i < 4 ; i++) {
			int index = hour*4 + i;
			hourlyLoad += this.load[index];
		}
		return hourlyLoad/4;
	}
	
	public double getNumberOfVisitorsPerDay() {
		return numberOfVisitorsPerDay;
	}

	public double getAllVisitors() {
		return this.allVisitors;
	}

	public void finish() {
		//log.info("FacilityLoad finished");
		this.calculateFacilityLoad24();
	}
	
	public void reset() {
		for (int i=0; i<this.numberOfTimeBins; i++) {
			this.arrivals[i] = 0;
			this.departures[i] = 0;
			this.load[i] = 0.0;
		}
		this.numberOfVisitorsPerDay = 0.0;
		this.allVisitors = 0.0;
	}
	
	/* 
	 * All values >= 86400s (24h) are merged into the last time bin
	 */
	public int timeBinIndex(double time) {
		int lastBinIndex = this.numberOfTimeBins-1;
		int numberOfBinsPerHour = this.numberOfTimeBins/24;
		int secondsPerBin = 3600/numberOfBinsPerHour;
		return Math.min(lastBinIndex, (int)(time/secondsPerBin));
	}
}
