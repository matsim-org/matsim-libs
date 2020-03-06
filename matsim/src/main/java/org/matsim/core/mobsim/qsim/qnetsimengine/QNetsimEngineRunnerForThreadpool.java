/* *********************************************************************** *
 * project: org.matsim.*
 * QSimEngineRunner.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.concurrent.Callable;

import org.matsim.core.gbl.Gbl;

/**
 * Split up the old {@code QNetsimEngineRunner} which was implementing
 * 2 different approaches.
 * 
 * @author droeder @ Senozon Deutschland GmbH
 */
final class QNetsimEngineRunnerForThreadpool extends AbstractQNetsimEngineRunner implements Callable<Boolean>{
	
	private volatile boolean simulationRunning = true;
	private boolean movingNodes;

	QNetsimEngineRunnerForThreadpool() {
	}

	@Override
	public Boolean call() {
		if (!this.simulationRunning) {
			Gbl.printCurrentThreadCpuTime();
			return false;
		}

		if (this.movingNodes) {
			moveNodes();
		} else {
			moveLinks();
		}
		return true ;
	}

	public final void afterSim() {
		this.simulationRunning  = false;
	}

	public final void setMovingNodes(boolean movingNodes) {
		this.movingNodes = movingNodes;
	}
}