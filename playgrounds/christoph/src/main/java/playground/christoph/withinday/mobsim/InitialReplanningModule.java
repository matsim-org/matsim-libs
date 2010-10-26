/* *********************************************************************** *
 * project: org.matsim.*
 * InitialReplanningModule.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.christoph.withinday.mobsim;

import playground.christoph.withinday.replanning.parallel.ParallelInitialReplanner;
import playground.christoph.withinday.replanning.replanners.interfaces.WithinDayInitialReplanner;

public class InitialReplanningModule extends WithinDayReplanningModule<WithinDayInitialReplanner> {
	
	public InitialReplanningModule(ParallelInitialReplanner parallelInitialReplanner) {
		this.parallelReplanner = parallelInitialReplanner;
	}
	
}
