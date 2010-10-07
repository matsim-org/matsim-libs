/* *********************************************************************** *
 * project: matsim
 * OccupancyAnalyzerListener.java
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

package org.matsim.pt.counts;

import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;

@Deprecated // this is now absorbed into PtCountControlerListener.  kai, oct'10
public class OccupancyAnalyzerListener implements
BeforeMobsimListener, AfterMobsimListener {

	private OccupancyAnalyzer occupancyAnalyzer;

	private OccupancyAnalyzerListener(OccupancyAnalyzer occupancyAnalyzer) {
		this.occupancyAnalyzer = occupancyAnalyzer;
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		int iter = event.getIteration();
		occupancyAnalyzer.reset(iter);
		event.getControler().getEvents().addHandler(occupancyAnalyzer);
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		int it = event.getIteration();
		event.getControler().getEvents().removeHandler(occupancyAnalyzer);
		occupancyAnalyzer.write(event.getControler().getControlerIO()
				.getIterationFilename(it, "occupancyAnalysis.txt"));
	}

}