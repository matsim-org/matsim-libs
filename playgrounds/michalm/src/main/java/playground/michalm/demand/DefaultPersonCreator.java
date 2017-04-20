/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.michalm.demand;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.zone.Zone;

public class DefaultPersonCreator implements PersonCreator {
	private final PopulationFactory pf;
	private final String idFormat;
	private int currentAgentId = 0;

	public DefaultPersonCreator(Scenario scenario) {
		this(scenario, "%07d");
	}

	// idFormat: e.g. "%07d", "taxi_customer_%04d"...
	public DefaultPersonCreator(Scenario scenario, String idFormat) {
		this.pf = scenario.getPopulation().getFactory();
		this.idFormat = idFormat;
	}

	@Override
	public Person createPerson(Plan plan, Zone fromZone, Zone toZone) {
		String strId = String.format(idFormat + "_%s_%s", currentAgentId++, fromZone.getId(), toZone.getId());
		Person person = pf.createPerson(Id.create(strId, Person.class));
		return person;
	}
}
