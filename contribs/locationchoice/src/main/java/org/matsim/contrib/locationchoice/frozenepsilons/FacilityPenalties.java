package org.matsim.contrib.locationchoice.frozenepsilons;

import java.util.TreeMap;

import org.matsim.api.core.v01.Id;

public class FacilityPenalties {
	/** name to use to add as a scenario element */
	public static final String ELEMENT_NAME = "faciliyPenalties";
	
	public FacilityPenalties() {} 

	private TreeMap<Id, FacilityPenalty> facilityPenalties = new TreeMap<Id, FacilityPenalty>();

	public TreeMap<Id, FacilityPenalty> getFacilityPenalties() {
		return this.facilityPenalties;
	}

}
