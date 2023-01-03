package org.matsim.households;

import org.matsim.utils.objectattributes.attributable.Attributable;

public class HouseholdUtils {
	// Logic gotten from PopulationUtils, but I am actually a bit unsure about the value of those methods now that
	// attributable is the only way to get attributes...

	public static <F extends Household & Attributable> Object getHouseholdAttribute(F household, String key) {
		return household.getAttributes().getAttribute( key );
	}

	public static <F extends Household & Attributable> void putHouseholdAttribute(F household, String key, Object value ) {
		household.getAttributes().putAttribute( key, value ) ;
	}

	public static <F extends Household & Attributable> Object removeHouseholdAttribute( F household, String key ) {
		return household.getAttributes().removeAttribute( key );
	}
}
