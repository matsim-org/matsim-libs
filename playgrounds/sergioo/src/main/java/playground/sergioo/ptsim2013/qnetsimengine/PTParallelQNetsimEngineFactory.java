/* *********************************************************************** *
 * project: org.matsim.*
 * ParallelQSimEngineFactory
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
package playground.sergioo.ptsim2013.qnetsimengine;


import org.matsim.core.mobsim.qsim.interfaces.Netsim;

import playground.sergioo.ptsim2013.QSim;


/**
 * @author dgrether
 *
 */
public class PTParallelQNetsimEngineFactory implements PTQNetsimEngineFactory {

	@Override
	public PTParallelQNetsimEngine createQSimEngine(Netsim sim) {
		return new PTParallelQNetsimEngine((QSim) sim, new PTQNetworkFactory());
	}

}
