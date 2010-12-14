/* *********************************************************************** *
 * project: kai
 * MyPersonImpl.java
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

package playground.kai.ids;

import org.matsim.api.core.v01.Id;
import org.matsim.core.population.PersonImpl;

/**
 * @author nagel
 *
 */
public class MyPersonImpl extends PersonImpl {
	PersonId id ;

	/**
	 * @param id
	 */
	public MyPersonImpl(Id id) {
		super(id);
		this.id = (PersonId) id ;
	}
	
	public PersonId getId() {
		return id ;
	}

}
