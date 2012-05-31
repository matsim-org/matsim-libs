package playground.pieter.singapore.demand;

import java.io.Serializable;
import java.util.ArrayList;

public class HouseholdSG implements Serializable {
	ArrayList<PaxSG> pax;
	int synthHouseholdId;
	boolean carAvailability;
	boolean carAlreadyAssigned=false;
	int numberOfLicenses;
	String homeFacilityId;
	int incomeHousehold;

	public HouseholdSG(int synthHouseholdId, int carAvailability,
			String homeFacilityId) {
		super();
		this.synthHouseholdId = synthHouseholdId;
		this.carAvailability = carAvailability > 0 ? true : false;
		this.pax = new ArrayList<PaxSG>();
		this.homeFacilityId = homeFacilityId;
	}
}
