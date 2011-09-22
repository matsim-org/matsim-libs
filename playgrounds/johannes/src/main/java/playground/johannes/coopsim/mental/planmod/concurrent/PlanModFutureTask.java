/* *********************************************************************** *
 * project: org.matsim.*
 * PlanModFutureTask.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.coopsim.mental.planmod.concurrent;

import java.util.concurrent.FutureTask;

/**
 * @author illenberger
 *
 */
class PlanModFutureTask extends FutureTask<Object> {

	private final PlanModRunnable runnable;
	
	PlanModFutureTask(PlanModRunnable runnable) {
		super(runnable, null);
		this.runnable = runnable;
	}
	
	PlanModRunnable getRunnable() {
		return runnable;
	}

}
