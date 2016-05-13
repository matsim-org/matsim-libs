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

import java.io.PrintWriter;
import java.util.Map;

import org.matsim.contrib.taxi.data.TaxiData;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.*;
import org.matsim.core.controler.listener.*;
import org.matsim.core.utils.io.IOUtils;

import com.google.inject.Inject;


public class TaxiStatsDumper
    implements AfterMobsimListener, ShutdownListener
{
    private static final String HEADER = "iter"//
            + "\t|\tTP_avg\tTP_sd\tTP_p95\tTP_max"//
            + "\t|\tRE_fleet\tRE_avg\tRE_sd"//
            + "\t|\tRW_fleet\tRW_avg\tRW_sd"//
            + "\t|\tTO";

    private final TaxiData taxiData;
    private final TaxiConfigGroup taxiCfg;
    private final OutputDirectoryHierarchy controlerIO;
    private final PrintWriter multiDayWriter;


    @Inject
    public TaxiStatsDumper(TaxiData taxiData, TaxiConfigGroup taxiCfg, OutputDirectoryHierarchy controlerIO)
    {
        this.taxiData = taxiData;
        this.taxiCfg = taxiCfg;
        this.controlerIO = controlerIO;
        multiDayWriter = new PrintWriter(
                IOUtils.getBufferedWriter(controlerIO.getOutputFilename("taxi_daily_stats.txt")));
        multiDayWriter.println(HEADER);
    }


    @Override
    public void notifyAfterMobsim(AfterMobsimEvent event)
    {
        TaxiStatsCalculator calculator = new TaxiStatsCalculator(
                taxiData.getVehicles().values());

        appendToMultiDayStats(calculator.getDailyStats(), event);
        
        if (taxiCfg.getDetailedStats()) {
            writeDetailedStats(calculator.getTaxiStats(), event);
        }
    }


    private void appendToMultiDayStats(DailyTaxiStats s, AfterMobsimEvent event)
    {
        multiDayWriter.printf("%d", event.getIteration());

        multiDayWriter.printf("\t|\t%.1f\t%.1f\t%.1f\t%.1f", //
                s.passengerWaitTime.getMean(), //
                s.passengerWaitTime.getStandardDeviation(), //
                s.passengerWaitTime.getPercentile(95), //
                s.passengerWaitTime.getMax());

        multiDayWriter.printf("\t|\t%.3f\t%.3f\t%.3f", //
                s.getFleetEmptyDriveRatio(), //
                s.vehicleEmptyDriveRatio.getMean(), //
                s.vehicleEmptyDriveRatio.getStandardDeviation());

        multiDayWriter.printf("\t|\t%.3f\t%.3f\t%.3f", //
                s.getFleetStayRatio(), //
                s.vehicleStayRatio.getMean(), //
                s.vehicleStayRatio.getStandardDeviation());

        multiDayWriter.printf("\t|\t%.3f", s.getOccupiedDriveRatio());

        multiDayWriter.println();
        multiDayWriter.flush();
    }


    private void writeDetailedStats(Map<String, TaxiStats> taxiStats, AfterMobsimEvent event)
    {
        String prefix = controlerIO.getIterationFilename(event.getIteration(), "taxi_");

        new TaxiStatsWriter(taxiStats).write(prefix + "detailed_stats.txt");
        new TaxiHistogramsWriter(taxiStats).write(prefix + "detailed_histograms.txt");
    }


    @Override
    public void notifyShutdown(ShutdownEvent event)
    {
        multiDayWriter.close();
    }
}
