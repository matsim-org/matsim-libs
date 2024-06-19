package org.matsim.core.mobsim.qsim;

import org.matsim.api.core.v01.population.Plan;

public class PreplanningUtils{
	private PreplanningUtils() {} // do not instantiate
	public static Double getPrebookingOffset_s( Plan plan ){
		// yyyy prebooking info needs to be in plan since it will not survive in the leg.  :-(  kai, jan'20
		return (Double) plan.getAttributes().getAttribute( "prebookingOffset_s" );
	}
	public static void setPrebookingOffset_s( Plan plan, double offset ){
		plan.getAttributes().putAttribute( "prebookingOffset_s", offset );
		// yyyy prebooking info needs to be in plan since it will not survive in the leg.  :-(  kai, jan'20
	}
}
