/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2024 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.extension.companions;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.dvrp.passenger.PassengerGroupIdentifier;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;

import java.util.Optional;

/**
 * @author steffenaxer
 */
class DrtCompanionGroupIdentifier implements PassengerGroupIdentifier {
	private final Population population;
	DrtCompanionGroupIdentifier(final Population population)
	{
		this.population = population;
	}

	@Override
	public Optional<Id<PassengerGroup>> getGroupId(MobsimPassengerAgent agent) {
		Person person = wrapMobsimPassengerAgentToPerson(agent);
		return DrtCompanionUtils.getPassengerGroupIdentifier(person);
	}

	private Person wrapMobsimPassengerAgentToPerson(MobsimPassengerAgent agent)
	{
		return this.population.getPersons().get(agent.getId());
	}

}
