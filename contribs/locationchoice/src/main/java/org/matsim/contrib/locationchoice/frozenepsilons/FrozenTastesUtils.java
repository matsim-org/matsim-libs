package org.matsim.contrib.locationchoice.frozenepsilons;

import org.matsim.api.core.v01.population.Person;
import org.matsim.facilities.ActivityFacility;

class FrozenTastesUtils{

	private static final String LOCATIONCHOICE_MAXDCSCORE = "locationchoice_maxDCScore_";
	private static final String LOCATIONCHOICE_PERSONALKEYVALUE = "locationchoice_personalKeyValue";
	private static final String LOCATIONCHOICE_OWNFACILITYVALUE = "locationchoice_ownFacilityValue";

	public static Double getMaxDcScore( Person person, String activityType ) {
		Double result = (Double) person.getAttributes().getAttribute( LOCATIONCHOICE_MAXDCSCORE + activityType );
		return result;
	}
	public static void setMaxDcScore( Person person, String activityType, double score ) {
		person.getAttributes().putAttribute( LOCATIONCHOICE_MAXDCSCORE + activityType, score );
	}

	public static Double getPersonalKeyValue( Person person) {
		Double result = (Double) person.getAttributes().getAttribute( LOCATIONCHOICE_PERSONALKEYVALUE);
		return result;
	}

	public static void setPersonalKeyValue( Person person, double keyValue ) {
		person.getAttributes().putAttribute( LOCATIONCHOICE_PERSONALKEYVALUE, keyValue );
	}

	public static Double getOwnFacilityValue( ActivityFacility facility) {
		Double result = (Double) facility.getAttributes().getAttribute( LOCATIONCHOICE_OWNFACILITYVALUE);
		return result;
	}

	public static void setOwnFacilityValue(ActivityFacility facility, double facilityValue ) {
		facility.getAttributes().putAttribute( LOCATIONCHOICE_OWNFACILITYVALUE, facilityValue );
	}


}
