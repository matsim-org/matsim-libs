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

package playground.michalm.taxi;

import java.util.concurrent.*;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.taxi.data.TaxiData;
import org.matsim.contrib.taxi.schedule.reconstruct.ScheduleReconstructor;
import org.matsim.contrib.taxi.util.stats.*;
import org.matsim.core.network.*;


public class RecalcStatsAudiAVFlowPaper
{
    public static void main(String[] args)
    {
        String networkFile = "../../../shared-svn/projects/audi_av/scenario/flowpaper/prep-runs/input/networkc.xml.gz";
        String path = "../../../runs-svn/avsim/flowpaper_0.15fc/";

        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(networkFile);

        ExecutorService service = Executors
                .newFixedThreadPool(Runtime.getRuntime().availableProcessors() / 2);

        for (String fleet : HourlyTaxiStatsExtractor.FLEETS) {
            for (String av : HourlyTaxiStatsExtractor.AVS) {
                String id = HourlyTaxiStatsExtractor.getId(fleet, av);

                service.execute(() -> {
                TaxiData taxiData = ScheduleReconstructor.reconstructFromFile(network,
                        path + id + "/" + id + ".output_events.xml.gz");

                TaxiStatsCalculator calculator = new TaxiStatsCalculator(
                        taxiData.getVehicles().values());
                String prefix = path + id + "/ITERS/it.50/" + id + ".50.";

                new TaxiStatsWriter(calculator.getTaxiStats())
                        .write(prefix + "hourly_stats_new_stats.txt");
                new TaxiHistogramsWriter(calculator.getTaxiStats())
                        .write(prefix + "hourly_histograms_new_stats.txt");
                });
            }
        }
        service.shutdown();
    }
}
