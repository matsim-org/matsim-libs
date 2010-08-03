/* *********************************************************************** *
 * project: org.matsim.*
 * EvacuationDuringLegReplanningModule.java
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

package playground.christoph.evacuation.mobsim;

import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.withinday.mobsim.DuringLegReplanningModule;
import playground.christoph.withinday.replanning.parallel.ParallelDuringLegReplanner;

public class EvacuationDuringLegReplanningModule extends DuringLegReplanningModule {
	
	private double evacuationTime = EvacuationConfig.evacuationTime;
	
	public EvacuationDuringLegReplanningModule(ParallelDuringLegReplanner parallelDuringLegReplanner) {
		super(parallelDuringLegReplanner);
	}

	@Override
	public void doReplanning(double time) {
		if (time != evacuationTime) return;
//		if (time < evacuationTime) return;
		else super.doReplanning(time);
	}
}