/* *********************************************************************** *
 * project: org.matsim.*
 * DgOnTheFlyQueueSimQuad
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
package playground.dgrether.signalVis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.vis.otfvis.OTFVisQueueSim;

/**
 * This is actually a more or less direct copy of OnTheFlyQueueSimQuad, because
 * the important command, i.e. new OnTheFlyClientQuad(..., MyConncetionManager)
 * is not accessible even in case of an overwritten prepareSim() that has to
 * call QueueSimulation.prepareSim() in any case;-).
 * 
 * @author dgrether
 * 
 */
public class DgOnTheFlyQueueSimQuad extends OTFVisQueueSim {

	public DgOnTheFlyQueueSimQuad(Scenario scenario, EventsManagerImpl events) {
		super(scenario, events);
		this.setConnectionManager(new DgConnectionManagerFactory().createConnectionManager());
	}
}
