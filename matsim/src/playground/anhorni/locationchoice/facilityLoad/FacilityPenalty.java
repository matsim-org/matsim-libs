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

package playground.anhorni.locationchoice.facilityLoad;

import org.jfree.util.Log;
import org.matsim.gbl.Gbl;

/*
 * TODO:
 * 1.	At the moment a facility has activities and an activity has a capacity.
 * 		We have to define this more precise:
 * 		For work and home in the same facility we need two independent capacities
 * 		(-> could be done in Activity)
 * 		But shopping and leisure in a shopping mall with movie theaters has to be treated with one cap
 * 		(-> so it is better handled in Facility)
 *
 * 		At the moment I need only shopping (and leisure) thus I only use one cap.
 * 		(The smallest of all shopping (and leisure) activities of the facility).
 *
 * 2.	The mobsim handles times > 24 h
 *		Facility load has to be handled for hour 0..24 only (acc. to M.B.)
 */

public class FacilityPenalty {
	
	private FacilityLoad facilityLoad;
	private double capacity = 0.0;
	private final int numberOfTimeBins = 4*24;
	private int scaleNumberOfPersons = 1;
	
	private double sumCapacityPenaltyFactor = 0.0;
	
	private double restraintFcnFactor = 0.0;
	private double restraintFcnExp = 0.0;
	
	FacilityPenalty(double minCapacity, int scaleNumberOfPersons) {
		this.capacity = minCapacity;
		this.facilityLoad = new FacilityLoad(this.numberOfTimeBins, scaleNumberOfPersons);
		this.scaleNumberOfPersons = scaleNumberOfPersons;
		
		this.restraintFcnFactor = Double.parseDouble(Gbl.getConfig().locationchoice().getRestraintFcnFactor());
		this.restraintFcnExp = Double.parseDouble(Gbl.getConfig().locationchoice().getRestraintFcnExp());
		}
		
	private double calculateCapPenaltyFactor(int startTimeBinIndex, int endTimeBinIndex) {
		
		int [] facilityload = this.facilityLoad.getLoad();
		double capPenaltyFactor = 0.0;
				
		for (int i=startTimeBinIndex; i<endTimeBinIndex+1; i++) {
			if (this.capacity > 0) {
			capPenaltyFactor += restraintFcnFactor*Math.pow(
					(double)facilityload[i]/(this.capacity), restraintFcnExp);
			}
			else {
				// do nothing: is penalized by costs for waiting time
			}
		}

		capPenaltyFactor /= (endTimeBinIndex-startTimeBinIndex+1);
		capPenaltyFactor = Math.min(1.0, capPenaltyFactor);		
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

		int startTimeBinIndex = Math.min(this.numberOfTimeBins-1, (int)(startTime/(3600/(this.numberOfTimeBins/24))));
		int endTimeBinIndex = Math.min(this.numberOfTimeBins-1, (int)(endTime/(3600/(this.numberOfTimeBins/24))));	
		return calculateCapPenaltyFactor(startTimeBinIndex, endTimeBinIndex);
	}
	
	public FacilityLoad getFacilityLoad() {
		return facilityLoad;
	}

	public void setFacilityLoad(FacilityLoad facilityLoad) {
		this.facilityLoad = facilityLoad;
	}
	
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
