/* *********************************************************************** *
 * project: org.matsim.*
 * ParallelDuringLegReplanner.java
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

package playground.christoph.withinday.replanning.parallel;

import java.util.List;

import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.listeners.SimulationListener;

import playground.christoph.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplanner;

/**
 * A class for running {@link WithinDayDuringLegReplanner} in parallel using threads.
 *
 * @author Christoph Dobler
 */
public class ParallelDuringLegReplanner extends ParallelReplanner<WithinDayDuringLegReplanner> {

	public ParallelDuringLegReplanner(int numOfThreads, Controler controler) {
		super(numOfThreads, controler);
		this.init("ParallelDuringLegReplanner");
	}
	
	public ParallelDuringLegReplanner(int numOfThreads, List<SimulationListener> list) {
		super(numOfThreads, list);
		this.init("ParallelDuringLegReplanner");
	}
	
}