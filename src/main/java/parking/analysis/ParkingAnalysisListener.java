/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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
  
package parking.analysis;

import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

import com.google.inject.Inject;

public class ParkingAnalysisListener implements IterationEndsListener {

	@Inject
	MatsimServices matsimServices;
	@Inject
	ParkingOccupancyEventHandler parkingOccupancyEventHandler;
	
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		String occupancyFileName = matsimServices.getControlerIO().getIterationFilename(event.getIteration(), "parkingOccupancy.csv");
		parkingOccupancyEventHandler.writeParkingOccupancyStats(occupancyFileName);
	}

}
