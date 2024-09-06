/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.core.population.io;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.PersonUtils;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * @author mrieser
 * @author balmermi
 */
abstract class AbstractPopulationWriterHandler implements PopulationWriterHandler {
	// all public non-final methods are empty. 

	@SuppressWarnings("unused")
	private final static Logger log = LogManager.getLogger(AbstractPopulationWriterHandler.class);
	
	@Override
	public final void writePerson(final Person person, final BufferedWriter writer) throws IOException {
		this.startPerson(person, writer);
		// travelcards
		if (PersonUtils.getTravelcards(person) != null) {
			for (String t : PersonUtils.getTravelcards(person)) {
				this.startTravelCard(t, writer);
				this.endTravelCard(writer);
			}
		}
		// plans
		for (Plan plan : person.getPlans()) {
			this.startPlan(plan, writer);
			// act/leg
			for (Object pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					Activity act = (Activity) pe;
					this.startAct(act, writer);
					this.endAct(writer);
				}
				else if (pe instanceof Leg) {
					Leg leg = (Leg) pe;
					this.startLeg(leg, writer);
					// route
					Route route = leg.getRoute();
					if (route != null) {
						this.startRoute(route, writer);
						this.endRoute(writer);
					}
					this.endLeg(writer);
				}
			}
			this.endPlan(writer);
		}
		this.endPerson(writer);
		this.writeSeparator(writer);
	}

	public abstract void startPerson(final Person person, final BufferedWriter out) throws IOException;

	public abstract void endPerson(final BufferedWriter out) throws IOException;

	public abstract void startTravelCard(final String travelcard, final BufferedWriter out) throws IOException;

	public abstract void endTravelCard(final BufferedWriter out) throws IOException;

	public abstract void startPlan(final Plan plan, final BufferedWriter out) throws IOException;

	public abstract void endPlan(final BufferedWriter out) throws IOException;

	public abstract void startAct(final Activity act, final BufferedWriter out) throws IOException;

	public abstract void endAct(final BufferedWriter out) throws IOException;

	public abstract void startLeg(final Leg leg, final BufferedWriter out) throws IOException;

	public abstract void endLeg(final BufferedWriter out) throws IOException;

	public abstract void startRoute(final Route route, final BufferedWriter out) throws IOException;

	public abstract void endRoute(final BufferedWriter out) throws IOException;

}
