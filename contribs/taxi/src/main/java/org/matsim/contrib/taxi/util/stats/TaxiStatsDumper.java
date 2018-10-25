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

import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.taxi.run.Taxi;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.util.CSVLineBuilder;
import org.matsim.contrib.util.CompactCSVWriter;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.utils.io.IOUtils;

import com.google.inject.Inject;

public class TaxiStatsDumper implements AfterMobsimListener, ShutdownListener {
	private static final String[] HEADER = { "iter", null, //
			"PassWaitTime_avg", "PassWaitTime_sd", "PassWaitTime_95%ile", "PassWaitTime_max", null, //
			"EmptyDriveRatio_fleetAvg", "EmptyDriveRatio_avg", "EmptyDriveRatio_sd", null, //
			"StayRatio_fleetAvg", "StayRatio_avg", "StayRatio_sd", null, //
			"OccupDriveRatio_fleetAvg" };

	private final Fleet fleet;
	private final TaxiConfigGroup taxiCfg;
	private final OutputDirectoryHierarchy controlerIO;
	private final CompactCSVWriter multiDayWriter;

	@Inject
	public TaxiStatsDumper(@Taxi Fleet fleet, TaxiConfigGroup taxiCfg, OutputDirectoryHierarchy controlerIO) {
		this.fleet = fleet;
		this.taxiCfg = taxiCfg;
		this.controlerIO = controlerIO;
		multiDayWriter = new CompactCSVWriter(IOUtils.getBufferedWriter(
				controlerIO.getOutputFilename("taxi_daily_stats_" + taxiCfg.getMode() + ".txt")));
		multiDayWriter.writeNext(HEADER);
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		TaxiStatsCalculator calculator = new TaxiStatsCalculator(fleet.getVehicles().values());

		appendToMultiDayStats(calculator.getDailyStats(), event);

		if (taxiCfg.getDetailedStats()) {
			writeDetailedStats(calculator.getTaxiStats(), event);
		}
	}

	private void appendToMultiDayStats(TaxiStats s, AfterMobsimEvent event) {
		multiDayWriter.writeNext(new CSVLineBuilder().add(event.getIteration() + "") //
				.addEmpty() //
				.addf("%.1f", s.passengerWaitTime.getMean()).addf("%.1f", s.passengerWaitTime.getStandardDeviation()) //
				.addf("%.0f", s.passengerWaitTime.getPercentile(95)) //
				.addf("%.0f", s.passengerWaitTime.getMax()) //
				.addEmpty() //
				.addf("%.4f", s.getFleetEmptyDriveRatio()) //
				.addf("%.4f", s.vehicleEmptyDriveRatio.getMean()) //
				.addf("%.4f", s.vehicleEmptyDriveRatio.getStandardDeviation()) //
				.addEmpty() //
				.addf("%.4f", s.getFleetStayRatio()) //
				.addf("%.4f", s.vehicleStayRatio.getMean()) //
				.addf("%.4f", s.vehicleStayRatio.getStandardDeviation()) //
				.addEmpty() //
				.addf("%.4f", s.getOccupiedDriveRatio()));
		multiDayWriter.flush();
	}

	private void writeDetailedStats(List<TaxiStats> taxiStats, AfterMobsimEvent event) {
		String prefix = controlerIO.getIterationFilename(event.getIteration(), "taxi_");

		new TaxiStatsWriter(taxiStats).write(prefix + "stats.txt");
		new TaxiHistogramsWriter(taxiStats).write(prefix + "histograms.txt");
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		multiDayWriter.close();
	}
}
