package org.matsim.contrib.locationchoice.frozenepsilons;

import org.matsim.api.core.v01.population.Person;

class FrozenTastesUtils{

	private static final String LOCATIONCHOICE_MAXDCSCORE = "locationchoice_maxDCScore_";

	public static double getMaxDcScore( Person person, String activityType ) {
		Double result = (Double) person.getAttributes().getAttribute( LOCATIONCHOICE_MAXDCSCORE + activityType );
		if ( result == null ) {
			result =  (Double) person.getAttributes().getAttribute( activityType );
//			// write under new key
//			setMaxDcScore( person, activityType, result );
//			// remove under old key 
//			person.getAttributes().removeAttribute( activityType );
			// maybe not helpful
		}
		return result;
	}
	public static void setMaxDcScore( Person person, String activityType, double score ) {
		person.getAttributes().putAttribute( LOCATIONCHOICE_MAXDCSCORE + activityType, score );
	}
	
}
