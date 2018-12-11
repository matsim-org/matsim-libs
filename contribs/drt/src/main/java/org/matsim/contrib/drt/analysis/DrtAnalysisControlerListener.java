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

package org.matsim.contrib.drt.analysis;

import com.google.inject.Inject;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.run.Drt;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

/**
 * @author jbischoff
 */
public class DrtAnalysisControlerListener implements IterationEndsListener {

	private final DynModePassengerStats drtPassengerStats;
	private final MatsimServices matsimServices;
	private final Network network;
	private final DrtRequestAnalyzer drtRequestAnalyzer;
	private final DrtConfigGroup drtCfg;
	private final QSimConfigGroup qSimCfg;
	private boolean headerWritten = false;
	private boolean vheaderWritten = false;
	private final String runId;
	private final DecimalFormat format = new DecimalFormat();
	private final int maxcap;

	@Inject
	public DrtAnalysisControlerListener(Config config, DrtConfigGroup drtCfg, @Drt Fleet fleet,
			DynModePassengerStats drtPassengerStats, MatsimServices matsimServices, Network network,
			DrtRequestAnalyzer drtRequestAnalyzer) {
		this.drtPassengerStats = drtPassengerStats;
		this.matsimServices = matsimServices;
		this.network = network;
		this.drtRequestAnalyzer = drtRequestAnalyzer;
		this.drtCfg = drtCfg;
		this.qSimCfg = config.qsim();
		runId = config.controler().getRunId();
		maxcap = DynModeTripsAnalyser.findMaxCap(fleet);

		format.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
		format.setMinimumIntegerDigits(1);
		format.setMaximumFractionDigits(2);
		format.setGroupingUsed(false);
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {

		drtRequestAnalyzer.writeAndPlotWaitTimeEstimateComparison(filename(event, "waitTimeComparison", ".png"),
				filename(event, "waitTimeComparison", ".csv"));
		List<DynModeTrip> trips = drtPassengerStats.getDrtTrips();

		DynModeTripsAnalyser.collection2Text(drtRequestAnalyzer.getRejections(),
				filename(event, "drt_rejections", ".csv"), "time;personId;fromLinkId;toLinkId;fromX;fromY;toX;toY");

		double rejectionRate = (double)drtRequestAnalyzer.getRejections().size()
				/ (double)(drtRequestAnalyzer.getRejections().size() + trips.size());
		String tripsSummarize = DynModeTripsAnalyser.summarizeTrips(trips, ";");
		double directDistanceMean = DynModeTripsAnalyser.getDirectDistanceMean(trips);
		writeIterationPassengerStats(
				tripsSummarize + ";" + drtRequestAnalyzer.getRejections().size() + ";" + format.format(rejectionRate),
				event.getIteration());
		double l_d = DynModeTripsAnalyser.getTotalDistance(drtPassengerStats.getVehicleDistances()) / (trips.size()
				* directDistanceMean);
		String vehStats = DynModeTripsAnalyser.summarizeVehicles(drtPassengerStats.getVehicleDistances(), ";")
				+ ";"
				+ format.format(l_d);
		String occStats = DynModeTripsAnalyser.summarizeDetailedOccupancyStats(drtPassengerStats.getVehicleDistances(),
				";", maxcap);
		writeIterationVehicleStats(vehStats, occStats, event.getIteration());
		if (drtCfg.isPlotDetailedCustomerStats()) {
			DynModeTripsAnalyser.collection2Text(trips, filename(event, "drt_trips", ".csv"), DynModeTrip.HEADER);
		}
		DynModeTripsAnalyser.writeVehicleDistances(drtPassengerStats.getVehicleDistances(),
				filename(event, "vehicleDistanceStats", ".csv"));
		DynModeTripsAnalyser.analyseDetours(network, trips, drtCfg, filename(event, "drt_detours"));
		DynModeTripsAnalyser.analyseWaitTimes(filename(event, "waitStats"), trips, 1800);
			DynModeTripsAnalyser.analyzeBoardingsAndDeboardings(trips, ";", qSimCfg.getStartTime(),
					qSimCfg.getEndTime(), 3600, filename(event, "drt_boardings", ".csv"),
					filename(event, "drt_alightments", ".csv"), network);

    }

	private String filename(IterationEndsEvent event, String prefix) {
		return filename(event, prefix, "");
	}

	private String filename(IterationEndsEvent event, String prefix, String extension) {
		return matsimServices.getControlerIO()
				.getIterationFilename(event.getIteration(), prefix + "_" + drtCfg.getMode() + extension);
	}

	/**
	 * @param summarizeTrips
	 * @param it             iteration
	 */
	private void writeIterationPassengerStats(String summarizeTrips, int it) {
		BufferedWriter bw = getAppendingBufferedWriter("drt_customer_stats", ".csv");
		try {
			if (!headerWritten) {
				headerWritten = true;
				bw.write(
						"runId;iteration;rides;wait_average;wait_max;wait_p95;wait_p75;wait_median;inVehicleTravelTime_mean;distance_m_mean;directDistance_m_mean;totalTravelTime_mean;rejections;rejectionRate");
			}
			bw.newLine();
			bw.write(runId + ";" + it + ";" + summarizeTrips);
			bw.flush();
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * @param summarizeVehicles
	 * @param it                iteration
	 */
	private void writeIterationVehicleStats(String summarizeVehicles, String vehOcc, int it) {
		BufferedWriter bw = getAppendingBufferedWriter("drt_vehicle_stats", ".csv");
		try {
			if (!vheaderWritten) {
				bw.write(
						"runId;iteration;vehicles;totalDistance;totalEmptyDistance;emptyRatio;totalRevenueDistance;averageDrivenDistance;averageEmptyDistance;averageRevenueDistance;d_r/d_t");

				bw.write(";l_det");

			}
			bw.newLine();
			bw.write(runId + ";" + it + ";" + summarizeVehicles);
			bw.flush();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		BufferedWriter bw2 = getAppendingBufferedWriter("drt_detailed_distanceStats", ".csv");
		try {
			if (!vheaderWritten) {
				vheaderWritten = true;
				bw2.write("runId;iteration");
				for (int i = 0; i <= maxcap; i++) {
					bw2.write(";" + i + " pax distance_m");
				}

			}
			bw2.newLine();
			bw2.write(runId + ";" + it + vehOcc);
			bw2.flush();
			bw2.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private BufferedWriter getAppendingBufferedWriter(String prefix, String extension) {
		return IOUtils.getAppendingBufferedWriter(
				matsimServices.getControlerIO().getOutputFilename(prefix + "_" + drtCfg.getMode() + extension));
	}
}
