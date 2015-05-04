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
package playground.gregor.casim.simulation;

import org.matsim.core.mobsim.qsim.QSim;

import playground.gregor.casim.simulation.physics.CAMultiLaneNetworkFactory;
import playground.gregor.casim.simulation.physics.CANetworkFactory;

public final class CANetsimEngineModule {

	public static void configure(QSim qSim) {
		CANetworkFactory fac = new CAMultiLaneNetworkFactory();
		CANetsimEngine cae = new CANetsimEngine(qSim, fac);
		qSim.addMobsimEngine(cae);
		qSim.addDepartureHandler(cae.getDepartureHandler());

	}

}
