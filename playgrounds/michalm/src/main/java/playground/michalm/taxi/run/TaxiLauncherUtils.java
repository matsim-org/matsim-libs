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

import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.run.VrpLauncherUtils.*;
import org.matsim.contrib.dvrp.util.TimeDiscretizer;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.util.TravelTime;

import playground.michalm.ev.*;
import playground.michalm.taxi.data.*;
import playground.michalm.taxi.data.file.*;


public class TaxiLauncherUtils
{
    public static ETaxiData initTaxiData(Scenario scenario, String taxisFile, String ranksFile)
    {
        ETaxiData taxiData = new ETaxiData();

        new ETaxiReader(scenario, taxiData).parse(taxisFile);
        new TaxiRankReader(scenario, taxiData).parse(ranksFile);

        return taxiData;
    }


    public static TimeDiscretizer getTimeDiscretizer(Scenario scenario,
            TravelTimeSource ttimeSource, TravelDisutilitySource tdisSource)
    {
        if (tdisSource == TravelDisutilitySource.DISTANCE) {
            return TimeDiscretizer.CYCLIC_24_HOURS;
        }

        //else if TravelDisutilitySource.Time:
        if (ttimeSource == TravelTimeSource.FREE_FLOW_SPEED && //
                !scenario.getConfig().network().isTimeVariantNetwork()) {
            return TimeDiscretizer.CYCLIC_24_HOURS;
        }

        return TimeDiscretizer.CYCLIC_15_MIN;
    }


    public static void initChargersAndVehicles(ETaxiData taxiData)
    {
        // TODO reduce charging speed in winter
        for (Charger c : taxiData.getChargers()) {
            new PartialFastChargingWithQueueingLogic(c);
        }

        // TODO variable AUX -- depends on weather etc...
        // TODO add the Leaf consumption model for driving 

        double driveRate = 150. * 3.6; //15 kWh / 100 km == 150 Wh/km; converted into J/m
        double auxPower = 500; //0.5 kW 

        for (ETaxi t : taxiData.getETaxis()) {
            t.setDriveEnergyConsumption(
                    EnergyConsumptions.createFixedDriveEnergyConsumption(t, driveRate));
            t.setAuxEnergyConsumption(new ETaxiAuxEnergyConsumption(t, auxPower));
        }
    }


    //no need to simulate with 1-second time step
    private static final int CHARGE_TIME_STEP = 5; //5 s ==> 0.35% SOC (fast charging, 50 kW) 
    private static final int AUX_DISCHARGE_TIME_STEP = 60;// 1 min ==> 0.25% SOC (3 kW aux power)


    public static void initChargingAndDischargingHandlers(ETaxiData taxiData, Network network,
            QSim qSim, TravelTime travelTime)
    {
        Map<Id<Person>, ETaxi> driverToTaxi = new HashMap<>();
        for (ETaxi t : taxiData.getETaxis()) {
            driverToTaxi.put(Id.createPersonId(t.getId()), t);
        }

        qSim.getEventsManager()
                .addHandler(new DriveDischargingHandler(driverToTaxi, network, travelTime));

        qSim.addQueueSimulationListeners(new ChargingAuxDischargingHandler(taxiData.getChargers(),
                CHARGE_TIME_STEP, taxiData.getETaxis(), AUX_DISCHARGE_TIME_STEP));
    }

}
