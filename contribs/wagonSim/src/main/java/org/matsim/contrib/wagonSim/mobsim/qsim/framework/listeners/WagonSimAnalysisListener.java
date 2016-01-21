/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

/**
 * 
 */
package org.matsim.contrib.wagonSim.mobsim.qsim.framework.listeners;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.wagonSim.Utils;
import org.matsim.contrib.wagonSim.analysis.stuckWagons.EventsStuckAgentsCollector;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;

/**
 * @author balmermi
 *
 */
public class WagonSimAnalysisListener implements BeforeMobsimListener, AfterMobsimListener {

	//////////////////////////////////////////////////////////////////////
	// variables
	//////////////////////////////////////////////////////////////////////

	private Set<Id<Person>> stuckAgents;
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public WagonSimAnalysisListener() {
	}

	//////////////////////////////////////////////////////////////////////
	// interface implementation
	//////////////////////////////////////////////////////////////////////
	
	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		stuckAgents = new HashSet<>();
		event.getServices().getEvents().addHandler(new EventsStuckAgentsCollector(stuckAgents));
	}
	
	//////////////////////////////////////////////////////////////////////

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		String outFile = event.getServices().getControlerIO().getIterationFilename(event.getIteration(),"stuckAgents.txt");
		try { Utils.writeObjectIds(stuckAgents, outFile, null); }
		catch (IOException e) { throw new RuntimeException(e); }
	}
}
