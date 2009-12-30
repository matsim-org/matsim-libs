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

package org.matsim.households;

import java.util.ArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.households.Income.IncomePeriod;

/**
 * @author dgrether
 */
public class HouseholdsFactoryImpl implements HouseholdsFactory {

	public HouseholdImpl createHousehold(Id householdId) {
		HouseholdImpl hh = new HouseholdImpl(householdId);
		hh.setMemberIds(new ArrayList<Id>());
		hh.setVehicleIds(new ArrayList<Id>());
		return hh;
	}
	
	public Income createIncome(double income, IncomePeriod period) {
		return new IncomeImpl(income, period);
	}

}
