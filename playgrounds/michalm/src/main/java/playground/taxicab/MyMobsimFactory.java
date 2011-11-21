/* *********************************************************************** *
 * project: org.matsim.*
 * MyMobsimFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.taxicab;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.framework.Simulation;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.interfaces.DepartureHandler;

/**
 * @author nagel
 *
 */
public class MyMobsimFactory implements MobsimFactory {
	private static final Logger log = Logger.getLogger(MyMobsimFactory.class);

	@Override
	public Simulation createMobsim(final Scenario sc, EventsManager events) {

		final QSim mobsim = new QSim( sc, events ) ;
		
		// add the taxi departure handler to the mobsim:
		DepartureHandler departureHandler = new TaxiModeDepartureHandler(mobsim) ;
		mobsim.addDepartureHandler(departureHandler) ;

		// add one taxi:
		AgentSource taxiDriverSource = new AgentSource() {
			@Override
			public List<MobsimAgent> insertAgentsIntoMobsim() {
				List<MobsimAgent> agents = new ArrayList<MobsimAgent>() ;
				
				TaxicabAgent taxiagent = TaxicabAgent.insertTaxicabAgent(mobsim) ;
				agents.add(taxiagent) ;
				mobsim.getEventsManager().addHandler(taxiagent) ;
				
				return agents ;
			}
		} ;
		mobsim.addAgentSource(taxiDriverSource) ;

		// add the taxicab dispatcher:
		Dispatcher dispatcher = new Dispatcher( mobsim.getEventsManager() ) ;
		mobsim.getEventsManager().addHandler(dispatcher ) ;
		
		// I add this to have some specific reporting on the console.
		mobsim.getEventsManager().addHandler( new MyEventsReporter() ) ;

		return mobsim ;
	}

}
