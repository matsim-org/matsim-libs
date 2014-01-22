/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.data.model.Vehicle;
import org.matsim.contrib.dvrp.data.schedule.Schedule;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.EnergyConsumptionModel;

import playground.michalm.taxi.TaxiData;
import playground.michalm.taxi.file.TaxiRankReader;
import playground.michalm.taxi.schedule.*;


public class TaxiLauncherUtils
{
    public static TaxiData initTaxiData(Scenario scenario, String ranksFileName,
            EnergyConsumptionModel ecm)
    {
        TaxiData taxiData = new TaxiData();
        new TaxiRankReader(scenario, taxiData, ecm).readFile(ranksFileName);

        for (Vehicle veh : taxiData.getVehicles()) {
            Schedule<TaxiTask> schedule = TaxiSchedules.getSchedule(veh);
            schedule.addTask(new TaxiWaitStayTask(veh.getT0(), veh.getT1(), veh.getDepot()
                    .getLink()));
        }

        return taxiData;
    }
}
