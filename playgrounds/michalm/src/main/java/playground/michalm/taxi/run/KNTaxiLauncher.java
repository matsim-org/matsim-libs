/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.michalm.taxi.run;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.dvrp.extensions.taxi.TaxiUtils;
import org.matsim.contrib.dvrp.run.VrpPopulationUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;


class KNTaxiLauncher
{
    /**
     * @param removeNonPassengers if {@code true}, only taxi traffic is simulated
 * @param endActivitiesAtTimeZero if {@code true}, everybody calls taxi at time 0
 * @param useOTFVis TODO
 * @param file path to the configuration file (e.g. param.in)
     */
    public static void run(TaxiLauncherParams params, boolean removeNonPassengers,
            boolean endActivitiesAtTimeZero, boolean useOTFVis)
    {

        SingleRunTaxiLauncher launcher = new SingleRunTaxiLauncher(params);
        
//        String file = "/Users/nagel/shared-svn/projects/maciejewski/input/2014_02/mielec-2-peaks-new-40-25/kaiparams.in" ;
//        SingleRunTaxiLauncher launcher = new SingleRunTaxiLauncher(TaxiLauncherParams.readParams(file));

        if (removeNonPassengers) {
            VrpPopulationUtils.removePersonsNotUsingMode(TaxiUtils.TAXI_MODE, launcher.scenario);

            if (endActivitiesAtTimeZero) {
                setEndTimeForFirstActivities(launcher.scenario, 0);
            }
        }

        if ( useOTFVis ) {
      	  OTFVisConfigGroup otfConfig = new OTFVisConfigGroup();
      	  launcher.scenario.getConfig().addModule(otfConfig);
      	  otfConfig.setLinkWidth(2);
        }

        launcher.initVrpPathCalculator();
        launcher.simulateIteration();
    }


    private static TaxiLauncherParams createParams()
    {
        Map<String, String> params = new HashMap<>();

        params.put("inputDir",
                "/Users/nagel/shared-svn/projects/maciejewski/input/2014_02/mielec-2-peaks-new-40-25/");

        params.put("netFile", "../mielec-2-peaks-new/network.xml");
        params.put("plansFile", "../mielec-2-peaks-new/output/ITERS/it.20/20.plans.xml.gz");
        params.put("eventsFile", "../mielec-2-peaks-new/output/ITERS/it.20/20.events.xml.gz");

        params.put("taxiCustomersFile", "taxiCustomers_40_pc.txt");
        params.put("taxisFile", "taxis-25.xml");
        params.put("ranksFile", "taxi_ranks-0.xml");

        params.put("algorithmConfig", "FIFO_RES_TW_FF");

        params.put("nearestRequestsLimit", "0");
        params.put("nearestVehiclesLimit", "0");

        params.put("destinationKnown", "!true");
        params.put("onlineVehicleTracker", "true");
        params.put("advanceRequestSubmission", "!true");
        params.put("pickupDuration", "1");
        params.put("dropoffDuration", "1");

        params.put("otfVis", "true");

        params.put("vrpOutFiles", "!true");
        params.put("vrpOutDirName", "vrp_output");

        params.put("outHistogram", "!true");
        params.put("histogramOutDirName", "histograms");

        params.put("writeSimEvents", "!true");
        
        return new TaxiLauncherParams(params);
    }


    private static void setEndTimeForFirstActivities(Scenario scenario, double time)
    {
        Map<Id<Person>, ? extends Person> persons = scenario.getPopulation().getPersons();
        for (Person p : persons.values()) {
            Activity activity = (Activity)p.getSelectedPlan().getPlanElements().get(0);
            activity.setEndTime(time);
        }
    }


    public static void main(String... args)
    {
        //demands: 10, 15, 20, 25, 30, 35, 40
        //supplies: 25, 50
        //path pattern: mielec-2-peaks-new-$supply$-$demand$
        //String file = "./src/main/resources/mielec-2-peaks_2014_02/params-gui.in";
        //String file = "/Users/nagel/shared-svn/projects/maciejewski/input/2014_02/mielec-2-peaks-new-40-50/params.in";
//        String file = "/Users/nagel/shared-svn/projects/maciejewski/input/2014_02/mielec-2-peaks-new-40-25/kaiparams.in";
//        TaxiLauncherParams params = TaxiLauncherParams.readParams(file);
        
        TaxiLauncherParams params = createParams();
        run(params, true, false, true);
    }
}
