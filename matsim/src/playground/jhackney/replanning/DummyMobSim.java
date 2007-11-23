/* *********************************************************************** *
 * project: org.matsim.*
 * DummyMobSim.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.jhackney.replanning;

import org.matsim.events.Events;
import org.matsim.mobsim.Simulation;
import org.matsim.plans.Plans;


public class DummyMobSim extends Simulation {
    protected Plans plans=null;
    protected Events events= null;
    
	public DummyMobSim(final Plans plans, final Events events) {
		super();
//		setEvents(events);
		this.events=events;
		this.plans = plans;
		System.out.println("NOTE this doesn't change the plan scores. Are the events non-null?");

	}
	public final Events getEvents() {
		return events;
	}
}
