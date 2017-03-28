/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.michalm.drt.analysis;

import java.util.List;

import org.matsim.core.config.Config;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

import com.google.inject.Inject;

import playground.michalm.drt.run.DrtConfigGroup;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class DRTAnalysisControlerListener implements IterationEndsListener{

	@Inject 
	VehicleOccupancyEvaluator vehicleOccupancyEvaluator;
	@Inject
	DrtPassengerStats drtPassengerStats;
	@Inject
	MatsimServices matsimServices;
	private final DrtConfigGroup drtgroup ;
	/**
	 * 
	 */
	@Inject
	public DRTAnalysisControlerListener(Config config) {
		drtgroup = (DrtConfigGroup) config.getModules().get(DrtConfigGroup.GROUP_NAME);
		
	}

	
	/* (non-Javadoc)
	 * @see org.matsim.core.controler.listener.IterationEndsListener#notifyIterationEnds(org.matsim.core.controler.events.IterationEndsEvent)
	 */
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		vehicleOccupancyEvaluator.calcAndWriteFleetStats(matsimServices.getControlerIO().getIterationFilename(event.getIteration(), "vehicleOccupancy"));
		if (drtgroup.isPlotDetailedVehicleStats()){
			vehicleOccupancyEvaluator.writeDetailedOccupancyFiles(matsimServices.getControlerIO().getIterationFilename(event.getIteration(), "vehicleStats_"));
		}
		List<DrtTrip> trips = drtPassengerStats.getDrtTrips();
		
		if (drtgroup.isPlotDetailedCustomerStats()){
			//TODO: Add this
		}
		DrtTripsAnalyser.analyseWaitTimes(matsimServices.getControlerIO().getIterationFilename(event.getIteration(), "waitStats"), trips, 1800);
	}

}
