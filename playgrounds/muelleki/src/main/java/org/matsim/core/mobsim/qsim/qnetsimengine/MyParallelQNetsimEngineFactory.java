package org.matsim.core.mobsim.qsim.qnetsimengine;

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

import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineFactory;


/**
 * @author dgrether
 *
 */
public class MyParallelQNetsimEngineFactory implements QNetsimEngineFactory {

	@Override
	public QNetsimEngine createQSimEngine(Netsim sim) {
		return new MyParallelQNetsimEngine( (QSim) sim);
	}

}
