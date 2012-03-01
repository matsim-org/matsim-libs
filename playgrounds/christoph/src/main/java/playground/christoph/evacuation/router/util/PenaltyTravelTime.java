/* *********************************************************************** *
 * project: org.matsim.*
 * PenaltyTravelTime.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.router.util;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.PersonalizableTravelTime;

public class PenaltyTravelTime implements PersonalizableTravelTime {

	private final PersonalizableTravelTime travelTime;
	private final AffectedAreaPenaltyCalculator penaltyCalculator;
	
	public PenaltyTravelTime(PersonalizableTravelTime travelTime, AffectedAreaPenaltyCalculator penaltyCalculator) {
		this.travelTime = travelTime;
		this.penaltyCalculator = penaltyCalculator;
	}
	
	@Override
	public double getLinkTravelTime(Link link, double time) {
		double tt = travelTime.getLinkTravelTime(link, time);
		double penaltyFactor = penaltyCalculator.getPenaltyFactor(link.getId(), time);
		return tt*penaltyFactor;
	}

	@Override
	public void setPerson(Person person) {
		travelTime.setPerson(person);
	}

}
