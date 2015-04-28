/* *********************************************************************** *
 * project: kai
 * KaiControler.java
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

package playground.kai.usecases.hybridmobsim;

import com.google.inject.Provider;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.qnetsimengine.KaiHybridEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.KaiHybridNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;

public class Main {

	public static void main(String[] args) {
		
		final MobsimFactory mobsimFactory = new MobsimFactory() {
			@Override
			public Mobsim createMobsim(Scenario sc, EventsManager events) {
				QSim qsim = new QSim(sc, events);

				ActivityEngine activityEngine = new ActivityEngine(events, qsim.getAgentCounter());
				qsim.addMobsimEngine(activityEngine);
				qsim.addActivityHandler(activityEngine);
				
				final KaiHybridEngine hybridEngine = new KaiHybridEngine() ;
				qsim.addMobsimEngine(hybridEngine) ;

				QNetsimEngine netsimEngine = new QNetsimEngine( qsim, new KaiHybridNetworkFactory(hybridEngine) ) ; 
				qsim.addMobsimEngine(netsimEngine);
				qsim.addDepartureHandler(netsimEngine.getDepartureHandler());

				TeleportationEngine teleportationEngine = new TeleportationEngine(sc, events);
				qsim.addMobsimEngine(teleportationEngine);
				qsim.addDepartureHandler(teleportationEngine) ;

				AgentFactory agentFactory = new DefaultAgentFactory(qsim);
				PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory, qsim);
				qsim.addAgentSource(agentSource);

				return qsim ;
			}
			
		} ;
		
		final Controler controler = new Controler( "examples/config/hybrid-config.xml" ) ;
		controler.setOverwriteFiles(true) ;
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindMobsim().toProvider(new Provider<Mobsim>() {
					@Override
					public Mobsim get() {
						return mobsimFactory.createMobsim(controler.getScenario(), controler.getEvents());
					}
				});
			}
		});
		controler.run();
	
	}

}
