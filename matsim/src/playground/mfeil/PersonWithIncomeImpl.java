/* *********************************************************************** *
 * project: org.matsim.*
 * PersonWithIncomeImpl.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

package playground.mfeil;


import org.matsim.api.core.v01.Id;
import org.matsim.core.population.PersonImpl;

public class PersonWithIncomeImpl extends PersonImpl {
	
	private int hhIncome;

	public PersonWithIncomeImpl(final Id id) {
		super(id);
	}

	public void setHhIncome(final int hhIncome) {
		this.hhIncome = hhIncome;
	}
	
	public int getHhIncome () {
		return this.hhIncome;
	}

}
