/* *********************************************************************** *
 * project: org.matsim.*
 * TransitQSimEngineFactory.java
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

package playground.christoph.mobsim.modularqsimfactory;

import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.pt.ComplexTransitStopHandlerFactory;
import org.matsim.core.mobsim.qsim.pt.TransitQSimEngine;

public class TransitQSimEngineFactory implements MobsimEngineFactory {

	@Override
	public MobsimEngine createMobsimSimEngine(Netsim sim) {
		TransitQSimEngine transitEngine = new TransitQSimEngine((QSim) sim);
		transitEngine.setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
		return transitEngine;
	}

}