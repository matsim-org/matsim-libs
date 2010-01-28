/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.kai.ptproject.qsim.interfaces;

import org.matsim.api.core.v01.population.Person;

/**
 * @author nagel
 *
 */
@Deprecated // do not yet use
public interface ActivityFacility {
	// that name is already taken.
	
	/**Adding a person, normally to do an activity.
	 * 
	 * @param person
	 * @return
	 */
	boolean addPerson( Person person ) ;
	// Is the "person" enough, or do we need to know how far s/he is in her/his plan?
	
	boolean update() ;
	// We can either say something like
	//    Person person = actFac.peekPersons() ;
	//    if ( nextDepartureTime( person ) <= now ) {
	//        actFac.remove( person ) ;
	//        parking.add( person ) ;
	//    }
	// _or_ something like
	//    actFac.update()
	// and contain the above dynamics _inside_ the container.
	//
	// Intuitions?
	//
	// The interface is more minimal in the second case.

}
