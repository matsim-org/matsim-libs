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
     * @param file path to the configuration file (e.g. param.in)
     * @param removeNonPassengers if {@code true}, only taxi traffic is simulated
     * @param endActivitiesAtTimeZero if {@code true}, everybody calls taxi at time 0 
     */
    public static void run(String file, boolean removeNonPassengers, boolean endActivitiesAtTimeZero)
    {
        SingleRunTaxiLauncher launcher = new SingleRunTaxiLauncher(TaxiLauncherParams.readParams(file));

        if (removeNonPassengers) {
            VrpPopulationUtils
                    .removePersonsNotUsingMode(TaxiUtils.TAXI_MODE, launcher.scenario);

            if (endActivitiesAtTimeZero) {
                setEndTimeForFirstActivities(launcher.scenario, 0);
            }
        }
        
        OTFVisConfigGroup otfConfig = new OTFVisConfigGroup() ;
        launcher.scenario.getConfig().addModule( otfConfig );
        otfConfig.setLinkWidth(2);

        launcher.initVrpPathCalculator();
        launcher.simulateIteration();
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
        String file = "/Users/nagel/shared-svn/projects/maciejewski/input/2014_02/mielec-2-peaks-new-40-25/kaiparams.in";
        run(file, true, false);
    }
}
