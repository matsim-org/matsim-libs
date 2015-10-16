/* *******************************************import org.matsim.core.mobsim.qsim.QSim;
: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.gregor.ctsim.simulation;

import org.matsim.core.mobsim.qsim.QSim;
import playground.gregor.ctsim.simulation.physics.CTNetsimEngine;

public final class CTNetsimEngineModule {

	public static void configure(QSim qSim) {


		CTNetworkFactory fac = new CTNetworkFactory();

		CTNetsimEngine cae = new CTNetsimEngine(qSim, fac);
		if (qSim.getScenario().getConfig().network().isTimeVariantNetwork()) {
//			CANetworkChangeEventsEngine change = new CANetworkChangeEventsEngine(cae);
//			qSim.addMobsimEngine(change);
			throw new RuntimeException("not yet implemented!");
		}
		qSim.addMobsimEngine(cae);
		qSim.addDepartureHandler(cae.getDepartureHandler());

	}

}
