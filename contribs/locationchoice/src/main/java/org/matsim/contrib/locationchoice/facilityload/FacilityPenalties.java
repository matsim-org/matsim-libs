package org.matsim.contrib.locationchoice.facilityload;

import java.util.TreeMap;

import org.matsim.api.core.v01.Id;

public class FacilityPenalties {
	
	public FacilityPenalties() {} 

	private TreeMap<Id, FacilityPenalty> facilityPenalties = new TreeMap<Id, FacilityPenalty>();

	public TreeMap<Id, FacilityPenalty> getFacilityPenalties() {
		return this.facilityPenalties;
	}

}
