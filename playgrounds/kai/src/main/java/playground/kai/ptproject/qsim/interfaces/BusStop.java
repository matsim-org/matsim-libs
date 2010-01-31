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
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.ptproject.qsim.PersonAgentI;

/**
 * @author nagel
 *
 */
public class BusStop {
	
	/**Adding a person that eventually wants to board a bus.	 */
	void addPerson( PersonAgentI person ) {}
	
	/**Getting the persons for a certain pt line one by one	 */
	Person getNextPersonForPTLine( String busLine ) {
		return new PersonImpl(new IdImpl("lsdkfj")) ; // dummy
	}
	// get the types right!!

}
