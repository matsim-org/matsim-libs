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
package org.matsim.ptproject.qsim.qnetsimengine;

import java.util.Random;

import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.interfaces.Mobsim;
import org.matsim.ptproject.qsim.interfaces.NetsimEngine;
import org.matsim.ptproject.qsim.interfaces.NetsimEngineFactory;


/**
 * @author dgrether
 *
 */
public class ParallelQSimEngineFactory implements NetsimEngineFactory {

	@Override
	public NetsimEngine createQSimEngine(Mobsim sim, Random random) {
		return new ParallelQSimEngine( (QSim) sim, random);
	}

}
