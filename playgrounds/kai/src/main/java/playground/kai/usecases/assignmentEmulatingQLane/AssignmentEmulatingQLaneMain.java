/* *********************************************************************** *
 * project: org.matsim.*												   *
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
package playground.kai.usecases.assignmentEmulatingQLane;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup.MobsimType;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.mobsim.qsim.qnetsimengine.AssignmentEmulatingQLaneNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.scenario.ScenarioUtils;


/**
 * @author nagel
 *
 */
public class AssignmentEmulatingQLaneMain {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final Config config = ConfigUtils.createConfig();

		final Scenario scenario = ScenarioUtils.loadScenario(config) ;

		Controler controler = new Controler( scenario ) ;
		controler.setMobsimFactory(new MobsimFactory() {
			@Override
			public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {
				String mobsim = config.controler().getMobsim();
				if ( MobsimType.qsim.toString().equals(mobsim) ) {
					throw new RuntimeException("the following will only work for " + MobsimType.qsim.toString() ) ;
				}
				// ===
				QSim simulation = (QSim) QSimUtils.createDefaultQSim(sc, eventsManager);
				simulation.addMobsimEngine(new QNetsimEngine(simulation, new AssignmentEmulatingQLaneNetworkFactory() ) );
				return simulation ;
				// note: this is called before "enrichSimulation" so we do not need to worry about that.
			}
		});
	}

}
