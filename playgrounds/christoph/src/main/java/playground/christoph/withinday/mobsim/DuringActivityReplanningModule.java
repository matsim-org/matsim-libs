/* *********************************************************************** *
 * project: org.matsim.*
 * ActEndReplanningModule.java
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

import playground.christoph.withinday.replanning.parallel.ParallelDuringActivityReplanner;
import playground.christoph.withinday.replanning.replanners.interfaces.WithinDayDuringActivityReplanner;

public class DuringActivityReplanningModule extends WithinDayReplanningModule<WithinDayDuringActivityReplanner> {
		
	public DuringActivityReplanningModule(ParallelDuringActivityReplanner parallelDuringActivityReplanner) {
		this.parallelReplanner = parallelDuringActivityReplanner;
	}

}