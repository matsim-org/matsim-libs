/* *********************************************************************** *
 * project: kai
 * Main.java
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

package playground.kai.usecases.janus;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimFactory;

/**
 * @author nagel
 *
 */
class Main {

	public static void main(String[] args) {
		final Controler ctrl = new Controler("abcd") ;
		ctrl.setMobsimFactory(new MobsimFactory(){
			@Override
			public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {
				QSim qsim = (QSim) new QSimFactory().createMobsim(sc, eventsManager) ;

				MobsimAgent ma = new MyMobsimDriverAgent( sc.getNetwork() ) ;
				qsim.insertAgentIntoMobsim(ma) ;
				
				return qsim ;
			}
		}) ;
	}

}
