/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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
package org.matsim.contrib.roadpricing;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A road pricing scheme that is subject to person-specific avoidance. Those
 * vehicles (not persons) with a '<code>toll_evader</code>' attribute do not
 * pay
 */
public class RoadPricingSchemeWithTollAvoidance implements RoadPricingScheme {
	public static final String ATTR_AVOIDANCE = "toll_evader";
	private static final String DESCRIPTION = "Scheme adapted with person-specific toll avoidance";

	private final RoadPricingSchemeImpl delegate;
	private Scenario scenario;

	public RoadPricingSchemeWithTollAvoidance(RoadPricingSchemeImpl scheme, Scenario scenario) {
		this.delegate = scheme;
		this.scenario = scenario;
	}

	@Override
	public String getName() {
		return delegate.getName();
	}

	@Override
	public String getType() {
		return delegate.getType();
	}

	@Override
	public String getDescription() {
		return delegate + ". " + DESCRIPTION;
	}

	@Override
	public Set<Id<Link>> getTolledLinkIds() {
		return delegate.getTolledLinkIds();
	}

	@Override
	public CostInfo getLinkCostInfo( Id<Link> linkId, double time, Id<Person> personId, Id<Vehicle> vehicleId ) {
		Person person = this.scenario.getPopulation().getPersons().get(personId);
		Object attr = person.getAttributes().getAttribute(ATTR_AVOIDANCE);
		if (attr == null) {
			return delegate.getLinkCostInfo(linkId, time, personId, vehicleId);
		} else {
			boolean tollEvader = (boolean) attr;
			if (tollEvader) {
				return new CostInfo(0.0, Time.getUndefinedTime(), 0.0);
			} else {
				return delegate.getLinkCostInfo(linkId, time, personId, vehicleId);
			}
		}
	}

	@Override
	public CostInfo getTypicalLinkCostInfo( Id<Link> linkId, double time ) {
		return delegate.getTypicalLinkCostInfo(linkId, time);
	}

	@Override
	public Iterable<CostInfo> getTypicalCosts() {
		return delegate.getTypicalCosts();
	}

	@Override
	public Map<Id<Link>, List<CostInfo>> getTypicalCostsForLink() {
		return delegate.getTypicalCostsForLink();
	}

}
