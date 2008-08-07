/* *********************************************************************** *
 * project: org.matsim.*
 * QueryAgentActivityStatus.java
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

package org.matsim.utils.vis.otfvis.opengl.queries;

import java.util.Collection;

import org.matsim.events.Events;
import org.matsim.mobsim.QueueLink;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.mobsim.Vehicle;
import org.matsim.population.Act;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.utils.vis.otfvis.data.OTFServerQuad;
import org.matsim.utils.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.utils.vis.otfvis.interfaces.OTFQuery;


public class QueryAgentActivityStatus implements OTFQuery{
	/**
	 * 
	 */
	private static final long serialVersionUID = -8532403277319196797L;

	public String agentID;

	boolean calcOffset = true;

	double now;
	//out
	int activityNr = -1;
	double finished = 0;

	public void query(QueueNetworkLayer net, Population plans, Events events, OTFServerQuad quad) {
		Person person = plans.getPerson(this.agentID);
		if (person == null) return;

		Plan plan = person.getSelectedPlan();

		// find the acual activity by searchin all activity links
		// for a vehicle with this agent id

		for (int i=0;i< plan.getActsLegs().size(); i+=2) {
			Act act = (Act)plan.getActsLegs().get(i);
			QueueLink link = net.getQueueLink(act.getLinkId());
			Collection<Vehicle> vehs = link.getAllVehicles();
			for (Vehicle info : vehs) {
				if (info.getDriver().getId().toString().compareTo(this.agentID) == 0) {
					// we found the little nutty, now lets reason about the lngth of ist activity
					double departure = info.getDepartureTime_s();
					double diff =  departure - info.getLastMovedTime();
					this.finished = (this.now - info.getLastMovedTime()) / diff;
					this.activityNr = i/2;
				}
			}
		}

	}

	public void remove() {
		// TODO Auto-generated method stub

	}

	public void draw(OTFDrawer drawer) {
		// TODO Auto-generated method stub

	}

	public boolean isAlive() {
		return false;
	}

	public Type getType() {
		return OTFQuery.Type.AGENT;
	}

	public void setId(String id) {
		this.agentID = id;
	}

	public void setNow(double now) {
		this.now = now;
	}
}
