/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.taxi.util.stats;

import java.util.List;

import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.run.QSimScopeObjectListener;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.common.csv.CSVLineBuilder;
import org.matsim.contrib.common.csv.CompactCSVWriter;
import org.matsim.core.controler.IterationCounter;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.utils.io.IOUtils;

public class TaxiStatsDumper implements ShutdownListener, MobsimBeforeCleanupListener, QSimScopeObjectListener<Fleet> {
	private static final String[] HEADER = { "iter", null, //
			"PassWaitTime_avg", "PassWaitTime_sd", "PassWaitTime_95%ile", "PassWaitTime_max", null, //
			"EmptyDriveRatio_fleetAvg", "EmptyDriveRatio_avg", "EmptyDriveRatio_sd", null, //
			"StayRatio_fleetAvg", "StayRatio_avg", "StayRatio_sd", null, //
			"OccupDriveRatio_fleetAvg" };

	private final TaxiConfigGroup taxiCfg;
	private final OutputDirectoryHierarchy controlerIO;
	private final IterationCounter iterationCounter;
	private final CompactCSVWriter multiDayWriter;

	private Fleet fleet;

	public TaxiStatsDumper(TaxiConfigGroup taxiCfg, OutputDirectoryHierarchy controlerIO,
			IterationCounter iterationCounter) {
		this.taxiCfg = taxiCfg;
		this.controlerIO = controlerIO;
		this.iterationCounter = iterationCounter;

		multiDayWriter = new CompactCSVWriter(IOUtils.getBufferedWriter(
				controlerIO.getOutputFilename("taxi_daily_stats_" + taxiCfg.getMode() + ".txt")));
		multiDayWriter.writeNext(HEADER);
	}

	@Override
	public void objectCreated(Fleet fleet) {
		this.fleet = fleet;
	}

	@Override
	public void notifyMobsimBeforeCleanup(MobsimBeforeCleanupEvent e) {
		TaxiStatsCalculator calculator = new TaxiStatsCalculator(fleet.getVehicles().values());

		appendToMultiDayStats(calculator.getDailyStats(), iterationCounter.getIterationNumber());
		if (taxiCfg.getDetailedStats()) {
			writeDetailedStats(calculator.getTaxiStats(), iterationCounter.getIterationNumber());
		}
	}

	private void appendToMultiDayStats(TaxiStats s, int iteration) {
		multiDayWriter.writeNext(new CSVLineBuilder().add(iteration + "")
				.addEmpty()
				.addf("%.1f", s.passengerWaitTime.getMean())
				.addf("%.1f", s.passengerWaitTime.getStandardDeviation())
				.addf("%.0f", s.passengerWaitTime.getPercentile(95))
				.addf("%.0f", s.passengerWaitTime.getMax())
				.addEmpty()
				.addf("%.4f", s.calculateFleetEmptyDriveRatio().orElse(Double.NaN))
				.addf("%.4f", s.vehicleEmptyDriveRatio.getMean())
				.addf("%.4f", s.vehicleEmptyDriveRatio.getStandardDeviation())
				.addEmpty()
				.addf("%.4f", s.calculateFleetStayRatio().orElse(Double.NaN))
				.addf("%.4f", s.vehicleStayRatio.getMean())
				.addf("%.4f", s.vehicleStayRatio.getStandardDeviation())
				.addEmpty()
				.addf("%.4f", s.calculateOccupiedDriveRatio().orElse(Double.NaN)));
		multiDayWriter.flush();
	}

	private void writeDetailedStats(List<TaxiStats> taxiStats, int iteration) {
		String prefix = controlerIO.getIterationFilename(iteration, "taxi_");
		new TaxiStatsWriter(taxiStats).write(prefix + "stats_" + taxiCfg.getMode() + ".txt");
		new TaxiHistogramsWriter(taxiStats).write(prefix + "histograms_" + taxiCfg.getMode() + ".txt");
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		multiDayWriter.close();
	}
}
