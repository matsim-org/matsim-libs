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

        if (removeNonPassengers) {
            VrpPopulationUtils.removePersonsNotUsingMode(TaxiUtils.TAXI_MODE, launcher.scenario);

            if (endActivitiesAtTimeZero) {
                setEndTimeForFirstActivities(launcher.scenario, 0);
            }
        }

        if (useOTFVis) {
            OTFVisConfigGroup otfConfig = new OTFVisConfigGroup();
            launcher.scenario.getConfig().addModule(otfConfig);
            otfConfig.setLinkWidth(2);
        }

        launcher.initTravelTimeAndDisutility();
        launcher.simulateIteration();
    }


    static TaxiLauncherParams createParams()
    {
        TaxiLauncherParams params = new TaxiLauncherParams();

        //demands: 10, 15, 20, 25, 30, 35, 40
        //supplies: 25, 50
        //path pattern: mielec-2-peaks-new-$supply$-$demand$
        params.inputDir =
                "/Users/nagel/shared-svn/projects/maciejewski/input/2014_02/mielec-2-peaks-new-40-25/";
                //"d:/svn-vsp/maciejewski/input/2014_02/mielec-2-peaks-new-40-25/";

        params.netFile = params.inputDir + "../mielec-2-peaks-new/network.xml";
        params.plansFile = params.inputDir + "../mielec-2-peaks-new/output/ITERS/it.20/20.plans.xml.gz";

        params.taxiCustomersFile = params.inputDir + "taxiCustomers_40_pc.txt";
        params.taxisFile = params.inputDir + "taxis-25.xml";
        params.ranksFile = params.inputDir + "taxi_ranks-0.xml";

        params.eventsFile = params.inputDir + "../mielec-2-peaks-new/output/ITERS/it.20/20.events.xml.gz";
        params.changeEventsFile = null;

        params.algorithmConfig = AlgorithmConfig.FIFO_RES_TW_FF;

        params.nearestRequestsLimit = 0;
        params.nearestVehiclesLimit = 0;

        params.onlineVehicleTracker = Boolean.TRUE;
        params.advanceRequestSubmission = Boolean.FALSE;
        params.destinationKnown = Boolean.FALSE;
        params.vehicleDiversion = Boolean.FALSE;

        params.pickupDuration = 1.;
        params.dropoffDuration = 1.;

        params.batteryChargingDischarging = Boolean.FALSE;

        params.otfVis = Boolean.TRUE;

        params.outputDir = null;
        params.vrpOutDir = null;
        params.histogramOutDir = null;
        params.eventsOutFile = null;
        
        params.validate();

        return params;
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
        TaxiLauncherParams params = createParams();
        run(params, true, false, true);
    }
}
