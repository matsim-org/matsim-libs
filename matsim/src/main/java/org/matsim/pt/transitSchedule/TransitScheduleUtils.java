package org.matsim.pt.transitSchedule;

import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class TransitScheduleUtils {
	// Logic gotten from PopulationUtils, but I am actually a bit unsure about the value of those methods now that
	// attributable is the only way to get attributes...

	public static Object getStopFacilityAttribute(TransitStopFacility facility, String key) {
		return facility.getAttributes().getAttribute( key );
	}

	public static void putStopFacilityAttribute(TransitStopFacility facility, String key, Object value ) {
		facility.getAttributes().putAttribute( key, value ) ;
	}

	public static Object removeStopFacilityAttribute( TransitStopFacility facility, String key ) {
		return facility.getAttributes().removeAttribute( key );
	}

	public static Object getLineAttribute(TransitLine facility, String key) {
		return facility.getAttributes().getAttribute( key );
	}

	public static void putLineAttribute(TransitLine facility, String key, Object value ) {
		facility.getAttributes().putAttribute( key, value ) ;
	}

	public static Object removeLineAttribute( TransitLine facility, String key ) {
		return facility.getAttributes().removeAttribute( key );
	}
}
