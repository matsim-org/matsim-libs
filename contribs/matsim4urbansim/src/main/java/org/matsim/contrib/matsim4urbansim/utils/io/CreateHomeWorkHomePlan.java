package org.matsim.contrib.matsim4urbansim.utils.io;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.contrib.matsim4urbansim.constants.InternalConstants;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationUtils;
import org.matsim.facilities.ActivityFacility;


public class CreateHomeWorkHomePlan {
	
	/**
	 * Helper method to start a plan by inserting the home location.  This is really only useful together with "completePlanToHwh",
	 * which completes the plan, and benefits from the fact that the Strings for the "home" and the "work" act are now concentrated
	 * here.
	 *
	 * @param plan
	 * @param homeCoord
	 * @param homeLocation
	 *
	 * @author nagel
	 */
	public static void makeHomePlan( Plan plan, Coord homeCoord, ActivityFacility homeLocation) {
		final Coord coord = homeCoord;
		Activity act = PopulationUtils.createAndAddActivityFromCoord(plan, InternalConstants.ACT_HOME, coord) ;
		act.setFacilityId( homeLocation.getId() );	// tnicolai: added facility id to compute zone2zone trips
	}

	/**
	 * Helper method to complete a plan with *wh in a consistent way.  Assuming that the first activity is the home activity.
	 *
	 * @param plan
	 * @param workCoord
	 * @param jobLocation
	 *
	 * @author nagel
	 */
	public static void completePlanToHwh ( Plan plan, Coord workCoord, ActivityFacility jobLocation ) {
		
		// complete the first activity (home) by setting end time. 
		Activity act = (Activity)PopulationUtils.getFirstActivity( plan );

		final double hmDpTime = (6.+2.*MatsimRandom.getRandom().nextDouble())*3600.;
		act.setEndTime( hmDpTime ) ;
		// (I just made this random between 6am and 8am to avoid the departure peak at 7.  In particular for pt users, it does not
		// seem to go away. kai, apr'13)
		// (tnicolai: make configurable: see actType1.setOpeningTime(7*3600))

		// gather coordinate and facility id needed for last activity
		Coord homeCoord = act.getCoord();
		Id homeId = act.getFacilityId();
		
		// set Leg
		PopulationUtils.createAndAddLeg( plan, TransportMode.car );
		final Coord coord = workCoord;
		
		// set second activity (work)
		act = PopulationUtils.createAndAddActivityFromCoord(plan, InternalConstants.ACT_WORK, coord);
		act.setFacilityId( jobLocation.getId() );
//		act.setMaximumDuration( 8.*3600. ) ; // tnicolai: make configurable: actType1.setTypicalDuration(8*60*60);
		act.setEndTime( hmDpTime + 9.*3600. ) ; // avoid durations except when short (e.g. plugging in hybrid veh).  kai, may'13
		
		// set Leg
		PopulationUtils.createAndAddLeg( plan, TransportMode.car );
		final Coord coord1 = homeCoord;
		
		// set last activity (=first activity) and complete home-work-home plan.
		PopulationUtils.createAndAddActivityFromCoord(plan, InternalConstants.ACT_HOME, coord1);
		act = (Activity)PopulationUtils.getLastActivity(plan);
		act.setFacilityId( homeId );
	}

}
