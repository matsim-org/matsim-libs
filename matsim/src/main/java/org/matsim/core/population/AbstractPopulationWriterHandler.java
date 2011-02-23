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

package org.matsim.core.population;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Iterator;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.knowledges.KnowledgeImpl;
import org.matsim.knowledges.Knowledges;
import org.matsim.population.Desires;

/**
 * @author mrieser
 * @author balmermi
 */
public abstract class AbstractPopulationWriterHandler implements PopulationWriterHandler {

	private final Knowledges knowledges;

	public AbstractPopulationWriterHandler() {
		this.knowledges = null;
	}

	public AbstractPopulationWriterHandler(final Knowledges knowledges) {
		this.knowledges = knowledges;
	}

	@Override
	public final void writePerson(final Person person, final BufferedWriter writer) throws IOException {
		this.startPerson(person, writer);
		if (person instanceof PersonImpl) {
			PersonImpl p = (PersonImpl)person;
			// travelcards
			if (p.getTravelcards() != null) {
				for (String t : p.getTravelcards()) {
					this.startTravelCard(t, writer);
					this.endTravelCard(writer);
				}
			}
			// desires
			if (p.getDesires() != null) {
				Desires d = p.getDesires();
				this.startDesires(d, writer);
				if (d.getActivityDurations() != null) {
					for (String act_type : d.getActivityDurations().keySet()) {
						this.startActDur(act_type,d.getActivityDurations().get(act_type), writer);
						this.endActDur(writer);
					}
				}
				this.endDesires(writer);
			}
			// knowledge
			if ((this.knowledges != null) && (this.knowledges.getKnowledgesByPersonId().get(p.getId()) != null)) {
				KnowledgeImpl k = this.knowledges.getKnowledgesByPersonId().get(p.getId());
				this.startKnowledge(k, writer);
				// activities
				Iterator<String> at_it = k.getActivityTypes().iterator();
				while (at_it.hasNext()) {
					String act_type = at_it.next();
					this.startActivity(act_type, writer);
					// locations (primary)
					for (ActivityOptionImpl a : k.getActivities(act_type,true)) {
						this.startPrimaryLocation(a, writer);
						this.endPrimaryLocation(writer);
					}
					// locations (secondary)
					for (ActivityOptionImpl a : k.getActivities(act_type,false)) {
						this.startSecondaryLocation(a, writer);
						this.endSecondaryLocation(writer);
					}
					this.endActivity(writer);
				}
				this.endKnowledge(writer);
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
		writer.flush();
	}

	public abstract void startPerson(final Person person, final BufferedWriter out) throws IOException;

	public abstract void endPerson(final BufferedWriter out) throws IOException;

	public abstract void startTravelCard(final String travelcard, final BufferedWriter out) throws IOException;

	public abstract void endTravelCard(final BufferedWriter out) throws IOException;

	public abstract void startDesires(final Desires desires, final BufferedWriter out) throws IOException;

	public abstract void endDesires(final BufferedWriter out) throws IOException;

	public abstract void startActDur(final String act_type, final double dur, final BufferedWriter out) throws IOException;

	public abstract void endActDur(final BufferedWriter out) throws IOException;

	public abstract void startKnowledge(final KnowledgeImpl knowledge, final BufferedWriter out) throws IOException;

	public abstract void endKnowledge(final BufferedWriter out) throws IOException;

	public abstract void startActivity(final String act_type, final BufferedWriter out) throws IOException;

	public abstract void endActivity(final BufferedWriter out) throws IOException;

	public abstract void startPrimaryLocation(final ActivityOptionImpl activity, final BufferedWriter out) throws IOException;

	public abstract void endPrimaryLocation(final BufferedWriter out) throws IOException;

	public abstract void startSecondaryLocation(final ActivityOptionImpl activity, final BufferedWriter out) throws IOException;

	public abstract void endSecondaryLocation(final BufferedWriter out) throws IOException;

	public abstract void startPlan(final Plan plan, final BufferedWriter out) throws IOException;

	public abstract void endPlan(final BufferedWriter out) throws IOException;

	public abstract void startAct(final Activity act, final BufferedWriter out) throws IOException;

	public abstract void endAct(final BufferedWriter out) throws IOException;

	public abstract void startLeg(final Leg leg, final BufferedWriter out) throws IOException;

	public abstract void endLeg(final BufferedWriter out) throws IOException;

	public abstract void startRoute(final Route route, final BufferedWriter out) throws IOException;

	public abstract void endRoute(final BufferedWriter out) throws IOException;

}
