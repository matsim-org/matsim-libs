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

import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.transEnergySim.controllers.EventHandlerGroup;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.*;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.ricardoFaria2012.EnergyConsumptionModelRicardoFaria2012;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.qsim.QSim;

import playground.jbischoff.energy.charging.RankArrivalDepartureCharger;
import playground.jbischoff.energy.vehicles.BatteryElectricVehicleImpl;
import playground.jbischoff.taxi.evaluation.*;
import playground.jbischoff.taxi.optimizer.rank.NOSRankTaxiOptimizer;
import playground.jbischoff.taxi.sim.ElectricTaxiSimEngine;
import playground.michalm.taxi.TaxiData;
import playground.michalm.taxi.model.TaxiRank;


/**
 * @author jbischoff
 */

public class ElectroCabLaunchUtils
{

    private EnergyConsumptionTracker energyConsumptionTracker;
    // private ChargeUponRankArrival chargeUponRankArrival;
    private RankArrivalDepartureCharger rankArrivalDepartureCharger;
    private TravelDistanceTimeEvaluator travelDistanceEvaluator;
    private TaxiCustomerWaitTimeAnalyser taxiCustomerWaitTimeAnalyser;


    /**
     * Mandatory
     */
    public ElectricTaxiSimEngine initVrpSimEngine(QSim qSim, MatsimVrpContext context,
            NOSRankTaxiOptimizer optimizer)
    {
        Scenario scenario = context.getScenario();

        optimizer.setRankMode(false);
        optimizer.setIdleRankMode(true);
        boolean ALLCARSELECTRIC = true;

        EventsManager events = qSim.getEventsManager();

        EventHandlerGroup handlerGroup = new EventHandlerGroup();

        EnergyConsumptionModel ecm = new EnergyConsumptionModelRicardoFaria2012();

        HashMap<Id, org.matsim.contrib.transEnergySim.vehicles.api.Vehicle> elvehicles = new HashMap<Id, org.matsim.contrib.transEnergySim.vehicles.api.Vehicle>();

        travelDistanceEvaluator = new TravelDistanceTimeEvaluator(scenario.getNetwork());

        if (ALLCARSELECTRIC) {

            for (Vehicle v : context.getVrpData().getVehicles()) {
                Id aid = v.getId();
                elvehicles.put(aid, new BatteryElectricVehicleImpl(ecm, 20 * 1000 * 3600));
                travelDistanceEvaluator.addAgent(aid);
            }
        }

        for (Vehicle v : context.getVrpData().getVehicles()) {
            travelDistanceEvaluator.addAgent(v.getId());
        }

        energyConsumptionTracker = new EnergyConsumptionTracker(elvehicles, scenario.getNetwork());
        rankArrivalDepartureCharger = new RankArrivalDepartureCharger(elvehicles);
        taxiCustomerWaitTimeAnalyser = new TaxiCustomerWaitTimeAnalyser(scenario);

        handlerGroup.addHandler(travelDistanceEvaluator);
        handlerGroup.addHandler(energyConsumptionTracker);
        handlerGroup.addHandler(rankArrivalDepartureCharger);
        handlerGroup.addHandler(taxiCustomerWaitTimeAnalyser);

        List<Id> rankLinkIds = new ArrayList<Id>();
        for (TaxiRank r : ((TaxiData)context.getVrpData()).getTaxiRanks()) {
            rankLinkIds.add(r.getLink().getId());
        }

        rankArrivalDepartureCharger.setRankLocations(rankLinkIds);
        events.addHandler(handlerGroup);

        optimizer.setRankArrivalCharger(rankArrivalDepartureCharger);

        // chargeUponRankArrival = new ChargeUponRankArrival(elvehicles);
        // chargeUponRankArrival.setRankLocations(this.rankReader.getRankLinks());

        // handlerGroup.addHandler(chargeUponRankArrival);

        ElectricTaxiSimEngine taxiSimEngine = new ElectricTaxiSimEngine(optimizer,
                rankArrivalDepartureCharger);
        qSim.addMobsimEngine(taxiSimEngine);

        return taxiSimEngine;
    }


    public void printStatisticsToConsole()
    {
        System.out.println("energy consumption stats");
        rankArrivalDepartureCharger.getSoCLog().printToConsole();
        System.out.println("===");

    }


    public String writeStatisticsToFiles(String dirname)
    {
        System.out.println("writing energy consumption stats directory to " + dirname);
        rankArrivalDepartureCharger.getSoCLog().writeToFiles(dirname);
        rankArrivalDepartureCharger.getChargeLog().writeToFiles(dirname);
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
