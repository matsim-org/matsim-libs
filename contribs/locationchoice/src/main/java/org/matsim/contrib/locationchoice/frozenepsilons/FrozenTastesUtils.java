package org.matsim.contrib.locationchoice.frozenepsilons;

import org.matsim.api.core.v01.population.Person;
import org.matsim.facilities.ActivityFacility;

class FrozenTastesUtils{

	private static final String LOCATIONCHOICE_MAXDCSCORE = "locationchoice_maxDCScore_";
	private static final String LOCATIONCHOICE_PERSONALKEYVALUE = "locationchoice_personKValue";
	private static final String LOCATIONCHOICE_OWNFACILITYVALUE = "locationchoice_facilityKValue";

	public static Double getMaxDcScore( Person person, String activityType ) {
		return (Double) person.getAttributes().getAttribute( LOCATIONCHOICE_MAXDCSCORE + activityType );
	}
	public static void setMaxDcScore( Person person, String activityType, double score ) {
		person.getAttributes().putAttribute( LOCATIONCHOICE_MAXDCSCORE + activityType, score );
	}

	public static Double getPersonalKeyValue( Person person) {
		return (Double) person.getAttributes().getAttribute( LOCATIONCHOICE_PERSONALKEYVALUE);
	}

	public static void setPersonalKeyValue( Person person, double keyValue ) {
		person.getAttributes().putAttribute( LOCATIONCHOICE_PERSONALKEYVALUE, keyValue );
	}

	public static Double getOwnFacilityValue( ActivityFacility facility) {
		return (Double) facility.getAttributes().getAttribute( LOCATIONCHOICE_OWNFACILITYVALUE);
	}

	public static void setOwnFacilityValue(ActivityFacility facility, double facilityValue ) {
		facility.getAttributes().putAttribute( LOCATIONCHOICE_OWNFACILITYVALUE, facilityValue );
	}


}
