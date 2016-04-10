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
import java.util.*;

import org.matsim.contrib.taxi.data.TaxiData;
import org.matsim.core.controler.events.*;
import org.matsim.core.controler.listener.*;
import org.matsim.core.utils.io.IOUtils;


public class TaxiStatsDumper
    implements AfterMobsimListener, ShutdownListener
{
    private final TaxiData taxiData;
    private final String outputDir;
    private final List<TaxiStats> stats = new ArrayList<>();


    public TaxiStatsDumper(TaxiData taxiData, String outputDir)
    {
        this.taxiData = taxiData;
        this.outputDir = outputDir;
    }


    @Override
    public void notifyAfterMobsim(AfterMobsimEvent event)
    {
        stats.add(new TaxiStatsCalculator(taxiData.getVehicles().values()).getStats());
    }


    @Override
    public void notifyShutdown(ShutdownEvent event)
    {
        PrintWriter pw = new PrintWriter(IOUtils.getBufferedWriter(outputDir + "/taxi_stats.txt"));
        pw.println(TaxiStats.HEADER);
        for (TaxiStats s : stats) {
            pw.println(s);
        }
        pw.close();
    }
}
