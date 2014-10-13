package playground.pieter.singapore.demand;

import java.io.Serializable;
import java.util.ArrayList;

class HouseholdSG implements Serializable {
	final ArrayList<PaxSG> pax;
	final int synthHouseholdId;
	final boolean carAvailability;
	boolean carAlreadyAssigned=false;
	int numberOfLicenses;
	final String homeFacilityId;
	int incomeHousehold;

	public HouseholdSG(int synthHouseholdId, int carAvailability,
			String homeFacilityId) {
		super();
		this.synthHouseholdId = synthHouseholdId;
		this.carAvailability = carAvailability > 0;
		this.pax = new ArrayList<>();
		this.homeFacilityId = homeFacilityId;
	}
}
