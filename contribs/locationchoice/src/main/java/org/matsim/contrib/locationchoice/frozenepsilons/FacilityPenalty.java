/* *********************************************************************** *
 * project: org.matsim.*
 * FacilityPenalty.java
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

package org.matsim.contrib.locationchoice.frozenepsilons;


/*
 * TODO: (discussion)
 * 1.	At the moment a facility has activities and an activity has a capacity.
 * 		We have to define this more precise:
 * 		For work and home in the same facility we need two independent capacities
 * 		(-> could be done in Activity)
 * 		But shopping and leisure in a shopping mall with movie theaters additionally needs a
 * 		shared capacity (e.g. parking)
 *
 * 		At the moment I need only shopping (and leisure) thus I only use one cap.
 * 		(The smallest of all shopping (and leisure) activities of the facility).
 *
 * 2.	The mobsim handles times > 24 h
 *		Facility load has to be handled for hour 0..24 only (according to M.B.)
 */

import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroupI;

class FacilityPenalty {

	private FacilityLoad facilityLoad;
	private double capacity = 0.0;
	private static int numberOfTimeBins = 4*24;
	private double scaleNumberOfPersons = 1;
	private double sumCapacityPenaltyFactor = 0.0;
	private double restraintFcnFactor = 0.0;
	private double restraintFcnExp = 0.0;

	public FacilityPenalty( double minCapacity, double scaleNumberOfPersons, DestinationChoiceConfigGroupI config ) {
		this.capacity = minCapacity;
		this.facilityLoad = new FacilityLoad(FacilityPenalty.numberOfTimeBins, scaleNumberOfPersons);
		this.scaleNumberOfPersons = scaleNumberOfPersons;
		this.restraintFcnFactor = config.getRestraintFcnFactor();
		this.restraintFcnExp = config.getRestraintFcnExp();
	}

	private double calculateCapPenaltyFactor(int startTimeBinIndex, int endTimeBinIndex) {

		double [] facilityload = this.facilityLoad.getLoad();
		double capPenaltyFactor = 0.0;

		for (int i=startTimeBinIndex; i<endTimeBinIndex+1; i++) {
			if (this.capacity > 0) {
			capPenaltyFactor += restraintFcnFactor*Math.pow(
					(facilityload[i]-scaleNumberOfPersons)/(this.capacity), restraintFcnExp);
			}

			/*
			 * facilityload[i]-scaleNumberOfPersons: being alone in a facility does not
			 * reduce the utility.
			 *
			 * } else: do nothing: is penalized by costs for waiting time
			 */
		}
		capPenaltyFactor /= (endTimeBinIndex-startTimeBinIndex+1);
		capPenaltyFactor = Math.min(0.5, capPenaltyFactor);
		this.sumCapacityPenaltyFactor += capPenaltyFactor * this.scaleNumberOfPersons;
		return capPenaltyFactor;
	}

	public double getCapacityPenaltyFactor(double startTime, double endTime) {

		if (startTime>24.0*3600.0 && endTime>24.0*3600.0) {
			return 0.0;
		}
		else if (endTime>24.0*3600.0) {
			endTime=24.0*3600.0;
		}

		int startTimeBinIndex = this.facilityLoad.timeBinIndex(startTime);
		int endTimeBinIndex = this.facilityLoad.timeBinIndex(endTime);
		return calculateCapPenaltyFactor(startTimeBinIndex, endTimeBinIndex);
	}

	public FacilityLoad getFacilityLoad() {
		return facilityLoad;
	}

//	public void setFacilityLoad(FacilityLoad facilityLoad) {
//		this.facilityLoad = facilityLoad;
//	}

	public double getCapacity() {
		return capacity;
	}


	public void setCapacity(double capacity) {
		this.capacity = capacity;
	}


	public double getSumCapacityPenaltyFactor() {
		return sumCapacityPenaltyFactor;
	}


	public void setSumCapacityPenaltyFactor(double sumCapacityPenaltyFactor) {
		this.sumCapacityPenaltyFactor = sumCapacityPenaltyFactor;
	}

	public void finish() {
		this.facilityLoad.finish();
	}

	public void reset() {
		this.sumCapacityPenaltyFactor = 0.0;
		this.facilityLoad.reset();
	}

}
