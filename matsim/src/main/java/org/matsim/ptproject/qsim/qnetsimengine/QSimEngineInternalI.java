/* *********************************************************************** *
 * project: matsim
 * QSimEngineInternalI.java
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

import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.interfaces.NetsimEngine;

/**
 * @author nagel
 *
 */
abstract class QSimEngineInternalI extends NetElementActivator implements NetsimEngine {
	
	abstract AgentSnapshotInfoBuilder getAgentSnapshotInfoBuilder() ;
	
	/** internally, we provide the full QNetwork */
	@Override
	abstract public QNetwork getNetsimNetwork() ;

	/** internally, we provide the full QSim */
	@Override
	abstract public QSim getMobsim() ;
}
