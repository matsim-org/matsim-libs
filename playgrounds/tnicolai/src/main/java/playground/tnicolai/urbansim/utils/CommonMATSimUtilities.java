/* *********************************************************************** *
 * project: org.matsim.*
 * CommonUtilies.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

/**
 *
 */
package playground.tnicolai.urbansim.utils;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;

import playground.tnicolai.urbansim.constants.Constants;

/**
 * @author thomas
 *
 */
public class CommonMATSimUtilities {

	/**
	 * This is used to parse a header line from a tab-delimited urbansim header and generate a Map that allows to look up column
	 * numbers (starting from 0) by giving the header line.
	 *
	 * I.e. if you have a header "from_id <tab> to_id <tab> travel_time", then idxFromKey.get("to_id") will return "1".
	 *
	 * This makes the reading of column-oriented files independent from the sequence of the columns.
	 *
	 * @param line
	 * @return idxFromKey as described above (mapping from column headers into column numbers)
	 *
	 * @author nagel
	 */
	public static Map<String,Integer> createIdxFromKey( String line, String seperator ) {
		String[] keys = line.split( seperator ) ;

		Map<String,Integer> idxFromKey = new HashMap<String, Integer>() ;
		for ( int i=0 ; i<keys.length ; i++ ) {
			idxFromKey.put(keys[i], i ) ;
		}
		return idxFromKey ;
	}

	/**
	 * Helper method to start a plan by inserting the home location.  This is really only useful together with "completePlanToHwh",
	 * which completes the plan, and benefits from the fact that the Strings for the "home" and the "work" act are now concentrated
	 * here.
	 *
	 * @param plan
	 * @param homeCoord
	 *
	 * @author nagel
	 */
	public static void makeHomePlan( PlanImpl plan, Coord homeCoord ) {
		plan.createAndAddActivity( Constants.ACT_HOME, homeCoord) ;
	}

	/**
	 * Helper method to complete a plan with *wh in a consistent way.  Assuming that the first activity is the home activity.
	 *
	 * @param plan
	 * @param workCoord
	 *
	 * @author nagel
	 */
	public static void completePlanToHwh ( PlanImpl plan, Coord workCoord ) {
		Activity act = plan.getFirstActivity();
		act.setEndTime( 7.*3600. ) ;
		Coord homeCoord = act.getCoord();

		plan.createAndAddLeg(TransportMode.car);
		act = plan.createAndAddActivity( Constants.ACT_WORK, workCoord );
		((ActivityImpl) act).setDuration( 8.*3600. ) ;

		plan.createAndAddLeg(TransportMode.car) ;
		plan.createAndAddActivity( Constants.ACT_HOME, homeCoord );
	}

}

