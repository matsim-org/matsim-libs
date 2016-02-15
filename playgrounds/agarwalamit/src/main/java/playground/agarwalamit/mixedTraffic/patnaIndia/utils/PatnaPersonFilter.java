/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.agarwalamit.mixedTraffic.patnaIndia.utils;

import java.util.Arrays;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

/**
 * @author amit
 */

public class PatnaPersonFilter{
	
	public enum PatnaUserGroup {
		urban, commuter, through;
	}
	
	public static boolean isPersonBelongsToUrban(Id<Person> personId){
		if ( personId.toString().startsWith("slum") || personId.toString().startsWith("nonSlum") ) return true;
		else return false;
	}

	public static boolean isPersonBelongsToCommuter(Id<Person> personId){
		if( Arrays.asList( personId.toString().split("_") ).contains("E2I") ) return true;
		else return false;
	}
	
	public static boolean isPersonBelongsToThroughTraffic(Id<Person> personId){
		if( Arrays.asList( personId.toString().split("_") ).contains("E2E") ) return true;
		else return false;
	}
	
	public static PatnaUserGroup getUserGroup(Id<Person> personId){
		if(isPersonBelongsToUrban(personId)) return PatnaUserGroup.urban;
		else if(isPersonBelongsToCommuter(personId)) return PatnaUserGroup.commuter;
		else if (isPersonBelongsToThroughTraffic(personId)) return PatnaUserGroup.through;
		else throw new RuntimeException("Person id "+personId+" do not belong to any of the predefined user group. Aborting ...");
	}
}
