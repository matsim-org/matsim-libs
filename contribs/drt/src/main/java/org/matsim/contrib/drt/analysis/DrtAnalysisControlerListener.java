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
package org.matsim.contrib.drt.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.io.IOUtils;

import com.google.inject.Inject;

/**
 * @author jbischoff
 *
 */
public class DrtAnalysisControlerListener implements IterationEndsListener {

	@Inject
	DrtVehicleOccupancyEvaluator vehicleOccupancyEvaluator;
	@Inject
	DynModePassengerStats drtPassengerStats;
	@Inject
	MatsimServices matsimServices;
	@Inject
	Network network;
	@Inject
	DrtRequestAnalyzer drtRequestAnalyzer;
	private final DrtConfigGroup drtgroup;
	private boolean headerWritten = false;
	private boolean vheaderWritten = false;

	/**
	 * 
	 */
	@Inject
	public DrtAnalysisControlerListener(Config config) {
		drtgroup = (DrtConfigGroup)config.getModules().get(DrtConfigGroup.GROUP_NAME);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.matsim.core.controler.listener.IterationEndsListener#notifyIterationEnds(org.matsim.core.controler.events.
	 * IterationEndsEvent)
	 */
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {

		vehicleOccupancyEvaluator.calcAndWriteFleetStats(
				matsimServices.getControlerIO().getIterationFilename(event.getIteration(), "vehicleOccupancy"));
		if (drtgroup.isPlotDetailedVehicleStats()) {
			vehicleOccupancyEvaluator.writeDetailedOccupancyFiles(
					matsimServices.getControlerIO().getIterationFilename(event.getIteration(), "vehicleStats_"));
		}
		drtRequestAnalyzer.writeAndPlotWaitTimeEstimateComparison(matsimServices.getControlerIO().getIterationFilename(event.getIteration(), "waitTimeComparison.png"), matsimServices.getControlerIO().getIterationFilename(event.getIteration(), "waitTimeComparison.csv"));
		List<DynModeTrip> trips = drtPassengerStats.getDrtTrips();

		writeIterationPassengerStats(DynModeTripsAnalyser.summarizeTrips(trips, ";"), event.getIteration());
		writeIterationVehicleStats(DynModeTripsAnalyser.summarizeVehicles(drtPassengerStats.getVehicleDistances(), ";"),
				event.getIteration());
		if (drtgroup.isPlotDetailedCustomerStats()) {
			DynModeTripsAnalyser.collection2Text(trips,
					matsimServices.getControlerIO().getIterationFilename(event.getIteration(), "drt_trips.csv"),
					DynModeTrip.HEADER);
		}
		DynModeTripsAnalyser.writeVehicleDistances(drtPassengerStats.getVehicleDistances(),
				matsimServices.getControlerIO().getIterationFilename(event.getIteration(), "vehicleDistanceStats.csv"));
		DynModeTripsAnalyser.analyseDetours(network, trips, drtgroup.getEstimatedBeelineDistanceFactor(),
				drtgroup.getEstimatedDrtSpeed(),
				matsimServices.getControlerIO().getIterationFilename(event.getIteration(), "drt_detours"));
		DynModeTripsAnalyser.analyseWaitTimes(
				matsimServices.getControlerIO().getIterationFilename(event.getIteration(), "waitStats"), trips, 1800);
	}

	/**
	 * @param summarizeTrips
	 * @param iteration
	 */
	private void writeIterationPassengerStats(String summarizeTrips, int it) {
		BufferedWriter bw = IOUtils.getAppendingBufferedWriter(
				matsimServices.getControlerIO().getOutputFilename("drt_customer_stats.csv"));
		try {
			if (!headerWritten) {
				headerWritten = true;
				bw.write(
						"iteration;rides;wait_average;wait_max;wait_p95;wait_p75;wait_median;inVehicleTravelTime_mean;distance_m_mean;directDistance_m_mean;totalTravelTime_mean");
			}
			bw.newLine();
			bw.write(it + ";" + summarizeTrips);
			bw.flush();
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * @param summarizeTrips
	 * @param iteration
	 */
	private void writeIterationVehicleStats(String summarizeVehicles, int it) {
		BufferedWriter bw = IOUtils
				.getAppendingBufferedWriter(matsimServices.getControlerIO().getOutputFilename("drt_vehicle_stats.csv"));
		try {
			if (!vheaderWritten) {
				vheaderWritten = true;
				bw.write(
						"iteration;vehicles;totalDistance;totalEmptyDistance;emptyRatio;totalRevenueDistance;averageDrivenDistance;averageEmptyDistance;averageRevenueDistance");

			}
			bw.newLine();
			bw.write(it + ";" + summarizeVehicles);
			bw.flush();
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
