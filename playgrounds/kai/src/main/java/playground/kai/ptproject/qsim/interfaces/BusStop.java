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
public interface BusStop {
	
	/**Adding a person that eventually wants to board a bus.
	 * 
	 * @param person
	 * @return
	 */
	boolean addPerson( Person person ) ;
	
	/**Getting the persons for a certain pt line one by one
	 * 
	 * @param busLine
	 * @return
	 */
	Person getNextPersonForPTLine( String busLine ) ;
	// get the types right!!

}
