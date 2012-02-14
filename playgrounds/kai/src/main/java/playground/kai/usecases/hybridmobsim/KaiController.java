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
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.framework.Simulation;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.interfaces.Netsim;
import org.matsim.ptproject.qsim.qnetsimengine.HybridNetworkFactory;
import org.matsim.ptproject.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.ptproject.qsim.qnetsimengine.QNetsimEngineFactory;

public class KaiController {

	public static void main(String[] args) {
		
		final QNetsimEngineFactory netsimEngineFactory = new QNetsimEngineFactory() {
			@Override
			public QNetsimEngine createQSimEngine(Netsim sim, Random random) {
//				return new QNetsimEngine( (QSim) sim, random ) ;
				return new QNetsimEngine( (QSim)sim, random, new HybridNetworkFactory() ) ;
			}
		} ;
		MobsimFactory mobsimFactory = new MobsimFactory() {
			@Override
			public Simulation createMobsim(Scenario sc, EventsManager events) {
				return QSim.createQSimAndAddAgentSource(sc, events, netsimEngineFactory) ;
			}
			
		} ;
		
		Controler controler = new Controler( "examples/config/hybrid-config.xml" ) ;
		controler.setOverwriteFiles(true) ;
		controler.setMobsimFactory(mobsimFactory) ;
		controler.run();
	
	}

}
