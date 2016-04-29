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

import org.matsim.contrib.taxi.data.TaxiData;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.*;
import org.matsim.core.controler.listener.*;
import org.matsim.core.utils.io.IOUtils;

import com.google.inject.Inject;


public class TaxiStatsDumper
    implements AfterMobsimListener, ShutdownListener
{
    private static final String HEADER = "WaitT\t" //
            + "95pWaitT\t"//
            + "MaxWaitT\t"//
            + "OccupiedT\t"//
            + "%EmptyDrive\t";

    private final TaxiData taxiData;
    private final PrintWriter pw;


    @Inject
    public TaxiStatsDumper(TaxiData taxiData, OutputDirectoryHierarchy controlerIO)
    {
        this.taxiData = taxiData;
        pw = new PrintWriter(
                IOUtils.getBufferedWriter(controlerIO.getOutputFilename("taxi_stats.txt")));
        pw.println("iter\t" + HEADER);
    }


    @Override
    public void notifyAfterMobsim(AfterMobsimEvent event)
    {
        TaxiStats ts = new TaxiStatsCalculator(taxiData.getVehicles().values()).getStats();
        pw.printf("%d\t%.1f\t%.1f\t%.1f\t%.0f\t%.3f\n", //
                event.getIteration(), //
                ts.passengerWaitTimes.getMean(), //
                ts.passengerWaitTimes.getPercentile(95), //
                ts.passengerWaitTimes.getMax(), //
                ts.getOccupiedDriveTimes().getMean(), //
                ts.getEmptyDriveRatio());
        pw.flush();
    }


    @Override
    public void notifyShutdown(ShutdownEvent event)
    {
        pw.close();
    }
}
