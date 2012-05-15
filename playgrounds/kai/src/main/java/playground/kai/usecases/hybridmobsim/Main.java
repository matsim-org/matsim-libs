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

import java.util.Random;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.qnetsimengine.KaiHybridEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.KaiHybridNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineFactory;

public class Main {

	public static void main(String[] args) {
		
		final KaiHybridEngine hybridEngine = new KaiHybridEngine() ;
		
		// reconfigure the netsimEngineFactory such that it uses the KaiHybridNetworkFactory:
		final QNetsimEngineFactory netsimEngineFactory = new QNetsimEngineFactory() {
			@Override
			public QNetsimEngine createQSimEngine(Netsim sim, Random random) {
				return new QNetsimEngine( (QSim)sim, random, new KaiHybridNetworkFactory(hybridEngine) ) ;
			}
		} ;
		
		// make sure that the mobsim indeed uses that reconfigured netsimEngineFactory:
		final MobsimFactory mobsimFactory = new MobsimFactory() {
			@Override
			public Mobsim createMobsim(Scenario sc, EventsManager events) {
				QSim qSim1 = new QSim(sc, events);
				ActivityEngine activityEngine = new ActivityEngine();
				qSim1.addMobsimEngine(activityEngine);
				qSim1.addActivityHandler(activityEngine);
				QNetsimEngine netsimEngine = netsimEngineFactory.createQSimEngine(qSim1, MatsimRandom.getRandom());
				qSim1.addMobsimEngine(netsimEngine);
				qSim1.addDepartureHandler(netsimEngine.getDepartureHandler());
				TeleportationEngine teleportationEngine = new TeleportationEngine();
				qSim1.addMobsimEngine(teleportationEngine);
				QSim qSim = qSim1;
				AgentFactory agentFactory = new DefaultAgentFactory(qSim);
				PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory, qSim);
				qSim.addAgentSource(agentSource);
				QSim qsim = qSim ;
				qsim.addMobsimEngine(hybridEngine) ;
				return qsim ;
			}
			
		} ;
		
		Controler controler = new Controler( "examples/config/hybrid-config.xml" ) ;
		controler.setOverwriteFiles(true) ;
		controler.setMobsimFactory(mobsimFactory) ;
		controler.run();
	
	}

}
