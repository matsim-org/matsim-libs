/* *********************************************************************** *
 * project: org.matsim.*
 * HouseholdsReaderV10
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

import java.util.List;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.households.basic.BasicHousehold;
import org.matsim.households.basic.BasicHouseholds;
import org.matsim.households.basic.BasicHouseholdsReaderV10;
import org.matsim.vehicles.BasicVehicle;

/**
 * @author dgrether
 * 
 */
public class HouseholdsReaderV10 extends BasicHouseholdsReaderV10 {

	private Households households;

	private ScenarioImpl scenario;

	public HouseholdsReaderV10(ScenarioImpl scenario) {
		this((BasicHouseholds<?>) scenario.getHouseholds());
		this.households = scenario.getHouseholds();
		this.scenario = scenario;
	}

	protected HouseholdsReaderV10(BasicHouseholds<? extends BasicHousehold> households) {
		super((BasicHouseholds<BasicHousehold>) households);
	}

	@Override
	protected BasicHousehold createHousehold(Id currentHhId, List<Id> memberIds, List<Id> vehicleIds) {
		Household hh = this.households.getBuilder().createHousehold(currentHhId);
		if (memberIds != null) {
			for (Id i : memberIds) {
				PersonImpl p = scenario.getPopulation().getPersons().get(i);
				if (p == null) {
					throw new IllegalStateException("Person referenced in households file with id " + i.toString()
							+ " is not existing in population. A consistent MATSim scenario could not be loaded. "
							+ " Use the basic classes and parsers to avoid this restriction.");
				}
				hh.getMembers().put(i, p);
				p.setHousehold(hh);
			}
		}
		if (vehicleIds != null) {
			for (Id id : vehicleIds) {
				BasicVehicle v = scenario.getVehicles().getVehicles().get(id);
				if (v == null) {
					throw new IllegalStateException(
							"Vehicle referenced in households file with id "
									+ id.toString()
									+ " is not existing in scenario's vehicle definitions. A consistent MATSim scenario could not be loaded. "
									+ " Use the basic classes and parsers to avoid this restriction.");
				}
				hh.getVehicles().put(id, v);
			}
		}
		return hh;
	}

}
