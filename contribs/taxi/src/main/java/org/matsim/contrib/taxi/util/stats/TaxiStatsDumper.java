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

import java.io.*;

import org.matsim.contrib.taxi.data.TaxiData;
import org.matsim.core.controler.events.*;
import org.matsim.core.controler.listener.*;


public class TaxiStatsDumper
    implements AfterMobsimListener, ShutdownListener
{
    private final TaxiData taxiData;
    private final PrintWriter pw;


    public TaxiStatsDumper(TaxiData taxiData, String outputDir)
    {
        this.taxiData = taxiData;

        try {
            pw = new PrintWriter(outputDir + "/taxi_stats.txt");
            pw.println(TaxiStats.HEADER);
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void notifyAfterMobsim(AfterMobsimEvent event)
    {
        pw.println(new TaxiStatsCalculator(taxiData.getVehicles().values()).getStats());
    }


    @Override
    public void notifyShutdown(ShutdownEvent event)
    {
        pw.close();
    }
}
