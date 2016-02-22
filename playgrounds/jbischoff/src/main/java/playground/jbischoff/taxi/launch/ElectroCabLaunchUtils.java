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

package playground.jbischoff.taxi.launch;

import java.util.HashMap;

import org.matsim.api.core.v01.*;
import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.transEnergySim.controllers.EventHandlerGroup;
import org.matsim.contrib.transEnergySim.vehicles.api.BatteryElectricVehicle;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.*;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.ricardoFaria2012.EnergyConsumptionModelRicardoFaria2012;
//import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.ricardoFaria2012.EnergyConsumptionModelRicardoFaria2012factorised;
import org.matsim.contrib.transEnergySim.vehicles.impl.BatteryElectricVehicleImpl;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.qsim.QSim;

import playground.jbischoff.energy.charging.taxi.*;
import playground.jbischoff.taxi.evaluation.*;
import playground.jbischoff.taxi.optimizer.rank.*;
//import playground.jbischoff.taxi.optimizer.rank.TaxiRankHandler;
import playground.jbischoff.taxi.sim.ElectricTaxiSimEngine;
import playground.jbischoff.taxi.vehicles.ElectricTaxi;
import playground.michalm.taxi.data.*;


/**
 * @author jbischoff
 */

public class ElectroCabLaunchUtils
{

    private EnergyConsumptionTracker energyConsumptionTracker;
    private TravelDistanceTimeEvaluator travelDistanceEvaluator;
    private TaxiCustomerWaitTimeAnalyser taxiCustomerWaitTimeAnalyser;
    private ElectricTaxiChargingHandler ecabhandler;
    private TaxiRankHandler rankhandler;


    /**
     * Mandatory
     */
    public ElectricTaxiSimEngine initVrpSimEngine(QSim qSim, MatsimVrpContext context,
            NOSRankTaxiOptimizer optimizer)
    {
        Scenario scenario = context.getScenario();
        optimizer.setRankMode(false);
        optimizer.setIdleRankMode(true);

        EventsManager events = qSim.getEventsManager();

        EventHandlerGroup handlerGroup = new EventHandlerGroup();

        EnergyConsumptionModel ecm = new EnergyConsumptionModelRicardoFaria2012();

        travelDistanceEvaluator = new TravelDistanceTimeEvaluator(scenario.getNetwork(), Double.MAX_VALUE);
        ecabhandler = new ElectricTaxiChargingHandler(events);
        rankhandler = new TaxiRankHandler();
        HashMap<Id<org.matsim.contrib.transEnergySim.vehicles.api.Vehicle>, org.matsim.contrib.transEnergySim.vehicles.api.Vehicle> bevs = new HashMap<Id<org.matsim.contrib.transEnergySim.vehicles.api.Vehicle>, org.matsim.contrib.transEnergySim.vehicles.api.Vehicle>();
        for (Vehicle v : context.getVrpData().getVehicles().values()) {
            Id aid = v.getId();
            rankhandler.addVehicle(v);
            if (aid.toString().startsWith("et")) {
                BatteryElectricVehicle bev = new BatteryElectricVehicleImpl(
                        ecm,
                        20 * 1000 * 3600,
                        Id.create(aid, org.matsim.contrib.transEnergySim.vehicles.api.Vehicle.class));
                bevs.put(bev.getId(), bev);
                ecabhandler.addVehicle(new ElectricTaxi(bev, v));

            }

        }
        System.out.println(context.getVrpData().getVehicles().size() + " taxis in total, of which "
                + ecabhandler.getVehicles().size() + " are electric.");

        for (Vehicle v : context.getVrpData().getVehicles().values()) {
            travelDistanceEvaluator.addAgent(v.getId());
        }

        energyConsumptionTracker = new EnergyConsumptionTracker(bevs, scenario.getNetwork());

        taxiCustomerWaitTimeAnalyser = new TaxiCustomerWaitTimeAnalyser(scenario, Double.MAX_VALUE);

        handlerGroup.addHandler(travelDistanceEvaluator);
        handlerGroup.addHandler(energyConsumptionTracker);
        handlerGroup.addHandler(ecabhandler);
        handlerGroup.addHandler(rankhandler);
        handlerGroup.addHandler(taxiCustomerWaitTimeAnalyser);

        for (TaxiRank r : ((ETaxiData)context.getVrpData()).getTaxiRanks().values()) {
            rankhandler.addRank(r);
            //            ecabhandler.addCharger(new TaxiCharger(1000, 50, r.getLink().getId()));
            if (r.getId().toString().equals("249"))
                ecabhandler.addCharger(new TaxiCharger(2, 50, r.getLink().getId()));
            if (r.getId().toString().equals("246"))
                ecabhandler.addCharger(new TaxiCharger(2, 50, r.getLink().getId()));

        }

        events.addHandler(handlerGroup);
        optimizer.setRankHandler(rankhandler);
        optimizer.setEcabhandler(ecabhandler);
        optimizer.createNearestRankDb();
        optimizer.createNearestChargerDb();

        ElectricTaxiSimEngine taxiSimEngine = new ElectricTaxiSimEngine(optimizer, ecabhandler);

        qSim.addMobsimEngine(taxiSimEngine);
        qSim.getScenario().getConfig().qsim().setEndTime(36 * 3600);
        return taxiSimEngine;
    }


    public void printStatisticsToConsole()
    {
        System.out.println("energy consumption stats");
        ecabhandler.getSoCLog().printToConsole();
        System.out.println("===");

    }


    public String writeStatisticsToFiles(String dirname)
    {
        System.out.println("writing energy consumption stats directory to " + dirname);
        ecabhandler.getSoCLog().writeToFiles(dirname);
        ecabhandler.getChargerLog().writeToFiles(dirname);
        String dist = travelDistanceEvaluator.writeTravelDistanceStatsToFiles(dirname
                + "travelDistanceStats.txt");
        String wait = taxiCustomerWaitTimeAnalyser.writeCustomerWaitStats(dirname
                + "customerWaitStatistic.txt");
        System.out.println("...done");
        travelDistanceEvaluator.printTravelDistanceStatistics();
        taxiCustomerWaitTimeAnalyser.printTaxiCustomerWaitStatistics();

        return wait + "\t" + dist;
    }

}
