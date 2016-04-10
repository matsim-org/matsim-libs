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

import org.matsim.contrib.taxi.data.TaxiData;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;


public class DetailedTaxiStatsDumper
    implements AfterMobsimListener
{
    private final TaxiData taxiData;
    private final String outputDir;
    private final int hours;


    public DetailedTaxiStatsDumper(TaxiData taxiData, String outputDir, int hours)
    {
        this.taxiData = taxiData;
        this.outputDir = outputDir;
        this.hours = hours;
    }


    @Override
    public void notifyAfterMobsim(AfterMobsimEvent event)
    {
        String suffix = "_" + event.getIteration() + ".txt";
        HourlyTaxiStatsCalculator calculator = new HourlyTaxiStatsCalculator(
                taxiData.getVehicles().values(), hours);
        HourlyTaxiStats.printAllStats(calculator.getStats(),
                outputDir + "/hourly_stats_run" + suffix);
        HourlyHistograms.printAllHistograms(calculator.getHourlyHistograms(),
                outputDir + "/hourly_histograms_run" + suffix);
        calculator.getDailyHistograms()
                .printHistograms(outputDir + "/daily_histograms_run" + suffix);
    }
}
