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

import java.io.*;
import java.util.*;

import org.matsim.analysis.LegHistogram;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;

import org.matsim.contrib.transEnergySim.controllers.EventHandlerGroup;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.EnergyConsumptionModel;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.EnergyConsumptionTracker;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.ricardoFaria2012.EnergyConsumptionModelRicardoFaria2012;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.*;
import org.matsim.core.mobsim.qsim.agents.*;
import org.matsim.core.mobsim.qsim.qnetsimengine.*;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.router.util.*;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;


import pl.poznan.put.util.lang.TimeDiscretizer;
import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.network.ArcFactory;
import playground.jbischoff.energy.charging.DepotArrivalDepartureCharger;
import playground.jbischoff.energy.vehicles.BatteryElectricVehicleImpl;
import playground.jbischoff.taxi.evaluation.TaxiCustomerWaitTimeAnalyser;
import playground.jbischoff.taxi.evaluation.TravelDistanceTimeEvaluator;
import playground.jbischoff.taxi.optimizer.rank.NOSRankTaxiOptimizer;
import playground.jbischoff.taxi.sim.ElectricTaxiSimEngine;
import playground.michalm.demand.ODDemandGenerator;
import playground.michalm.vrp.data.MatsimVrpData;
import playground.michalm.vrp.data.file.DepotReader;
import playground.michalm.vrp.data.network.*;
import playground.michalm.vrp.data.network.router.*;
import playground.michalm.vrp.data.network.shortestpath.MatsimArcFactories;
import playground.michalm.vrp.run.VrpConfigUtils;
import playground.michalm.vrp.taxi.*;
/**
 * 
 * 
 * 
 * @author jbischoff
 *
 */

public class ElectroCabLaunchUtils
{
    public enum TravelTimeSource
    {
        FREE_FLOW_SPEED(24 * 60 * 60), // no eventsFileName
        EVENTS_24_H(24 * 60 * 60), // based on eventsFileName, with 24-hour time interval
        EVENTS_15_MIN(15 * 60); // based on eventsFileName, with 15-minute time interval

        /*package*/final int travelTimeBinSize;
        /*package*/final int numSlots;


        private TravelTimeSource(int travelTimeBinSize)
        {
            this.travelTimeBinSize = travelTimeBinSize;
            this.numSlots = 24 * 60 * 60 / travelTimeBinSize;// to cover 24 hours
        }
    }


    public enum TravelCostSource
    {
        TIME, // travel time
        DISTANCE; // travel distance
    }
    private EnergyConsumptionTracker energyConsumptionTracker ;
//    private ChargeUponDepotArrival chargeUponDepotArrival;
    private DepotReader depotReader;
    private DepotArrivalDepartureCharger depotArrivalDepartureCharger;
    private TravelDistanceTimeEvaluator travelDistanceEvaluator;
    private TaxiCustomerWaitTimeAnalyser taxiCustomerWaitTimeAnalyser;

    /**
     * Mandatory
     */
    public static Scenario initMatsimData(String netFileName, String plansFileName,
            String taxiCustomersFileName)
    {
        Scenario scenario = ScenarioUtils.createScenario(VrpConfigUtils.createConfig());

        new MatsimNetworkReader(scenario).readFile(netFileName);
        new MatsimPopulationReader(scenario).readFile(plansFileName);

        List<String> taxiCustomerIds;
        try {
            taxiCustomerIds = ODDemandGenerator.readTaxiCustomerIds(taxiCustomersFileName);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (String id : taxiCustomerIds) {
            Person person = scenario.getPopulation().getPersons().get(scenario.createId(id));
            Leg leg = (Leg)person.getSelectedPlan().getPlanElements().get(1);
            leg.setMode(TaxiModeDepartureHandler.TAXI_MODE);
        }

        return scenario;
    }


    /**
     * Mandatory
     */
    public  MatsimVrpData initMatsimVrpData(Scenario scenario, TravelTimeSource ttimeSource,
            TravelCostSource tcostSource, String eventsFileName, String depotsFileName)
    {
        int travelTimeBinSize = ttimeSource.travelTimeBinSize;
        int numSlots = ttimeSource.numSlots;

        scenario.getConfig().travelTimeCalculator().setTraveltimeBinSize(travelTimeBinSize);

        TravelTime ttimeCalc;
        TravelDisutility tcostCalc;

        switch (ttimeSource) {
            case FREE_FLOW_SPEED:
                ttimeCalc = new FreeSpeedTravelTime();
                break;

            case EVENTS_15_MIN:
            case EVENTS_24_H:
                ttimeCalc = TravelTimeCalculators.createTravelTimeFromEvents(eventsFileName,
                        scenario);
                break;

            default:
                throw new IllegalArgumentException();
        }

        switch (tcostSource) {
            case DISTANCE:
                tcostCalc = new DistanceAsTravelDisutility();
                break;

            case TIME:
                tcostCalc = new TimeAsTravelDisutility(ttimeCalc);
                break;

            default:
                throw new IllegalArgumentException();
        }

        Network network = scenario.getNetwork();
        TimeDiscretizer timeDiscretizer = new TimeDiscretizer(travelTimeBinSize, numSlots);
        ArcFactory arcFactory = MatsimArcFactories.createArcFactory(network, ttimeCalc, tcostCalc,
                timeDiscretizer, false);

        MatsimVrpGraph graph;
        try {
            graph = MatsimVrpGraphCreator.create(network, arcFactory, false);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        VrpData vrpData = new VrpData();
        vrpData.setVrpGraph(graph);
        vrpData.setCustomers(new ArrayList<Customer>());
        vrpData.setRequests(new ArrayList<Request>());
        this.depotReader =  new DepotReader(scenario, vrpData);
        this.depotReader.readFile(depotsFileName);
       
        return new MatsimVrpData(vrpData, scenario);
    }


    /**
     * Mandatory
     */
    public  QSim initQSim(MatsimVrpData data, NOSRankTaxiOptimizer optimizer)
    {
    	optimizer.setRankMode(true);
        optimizer.setIdleRankMode(false); 
    	boolean ALLCARSELECTRIC = true;
    	
    	Scenario scenario = data.getScenario();
        EventsManager events = EventsUtils.createEventsManager();
		EventHandlerGroup handlerGroup = new EventHandlerGroup();
		
		EnergyConsumptionModel ecm = new EnergyConsumptionModelRicardoFaria2012();

		HashMap<Id, org.matsim.contrib.transEnergySim.vehicles.api.Vehicle> elvehicles=new HashMap<Id, org.matsim.contrib.transEnergySim.vehicles.api.Vehicle>();
		
		travelDistanceEvaluator = new TravelDistanceTimeEvaluator(scenario.getNetwork());
		
		if (ALLCARSELECTRIC){

		for (Vehicle v : data.getVrpData().getVehicles()){
			Id aid = new IdImpl(v.getName());
			elvehicles.put(aid, new BatteryElectricVehicleImpl(ecm,20*1000*3600));
			travelDistanceEvaluator.addAgent(aid);
		}
		}
		
		for (Vehicle v : data.getVrpData().getVehicles()){
			Id aid = new IdImpl(v.getName());
			travelDistanceEvaluator.addAgent(aid);
		}
		
		energyConsumptionTracker = new EnergyConsumptionTracker(elvehicles, scenario.getNetwork());
		depotArrivalDepartureCharger =new DepotArrivalDepartureCharger(elvehicles);
		taxiCustomerWaitTimeAnalyser = new TaxiCustomerWaitTimeAnalyser();
		
		handlerGroup.addHandler(travelDistanceEvaluator);
		handlerGroup.addHandler(energyConsumptionTracker);
		handlerGroup.addHandler(depotArrivalDepartureCharger);
		handlerGroup.addHandler(taxiCustomerWaitTimeAnalyser);
		depotArrivalDepartureCharger.setDepotLocations(this.depotReader.getDepotLinks());
		events.addHandler(handlerGroup);
		
		
		
        QSim qSim = new QSim(scenario, events);

        ActivityEngine activityEngine = new ActivityEngine();
        qSim.addMobsimEngine(activityEngine);
        qSim.addActivityHandler(activityEngine);
        QNetsimEngine netsimEngine = new DefaultQSimEngineFactory().createQSimEngine(qSim);
        qSim.addMobsimEngine(netsimEngine);
        qSim.addDepartureHandler(netsimEngine.getDepartureHandler());

        TeleportationEngine teleportationEngine = new TeleportationEngine();
        qSim.addMobsimEngine(teleportationEngine);
        
        
        optimizer.addDepotArrivalCharger(depotArrivalDepartureCharger);
        
        TaxiSimEngine taxiSimEngine = new ElectricTaxiSimEngine(qSim, data, optimizer,depotArrivalDepartureCharger);
        qSim.addMobsimEngine(taxiSimEngine);

        qSim.addAgentSource(new PopulationAgentSource(scenario.getPopulation(),
                new DefaultAgentFactory(qSim), qSim));
        qSim.addAgentSource(new TaxiAgentSource(data, taxiSimEngine, false));
        qSim.addDepartureHandler(new TaxiModeDepartureHandler(taxiSimEngine, data));

        
        
//    	chargeUponDepotArrival = new ChargeUponDepotArrival(elvehicles);
//    	chargeUponDepotArrival.setDepotLocations(this.depotReader.getDepotLinks());
    	
//      handlerGroup.addHandler(chargeUponDepotArrival);
        return qSim;
    }
    
    


    /**
     * Optional
     */
    public static void writeHistograms(LegHistogram legHistogram, String histogramOutDirName)
    {
        new File(histogramOutDirName).mkdir();
        legHistogram.write(histogramOutDirName + "legHistogram_all.txt");
        legHistogram.writeGraphic(histogramOutDirName + "legHistogram_all.png");
        for (String legMode : legHistogram.getLegModes()) {
            legHistogram.writeGraphic(histogramOutDirName + "legHistogram_" + legMode + ".png",
                    legMode);
        }
    }
    
	public void printStatisticsToConsole() {
		System.out.println("energy consumption stats");
		depotArrivalDepartureCharger.getSoCLog().printToConsole();
		System.out.println("===");

	}

	
	public void writeStatisticsToFiles(String dirname) {
		System.out.println("writing energy consumption stats directory to "+ dirname);
		depotArrivalDepartureCharger.getSoCLog().writeToFiles(dirname);
		travelDistanceEvaluator.writeTravelDistanceStatsToFiles(dirname + "travelDistanceStats.txt");
		taxiCustomerWaitTimeAnalyser.writeCustomerWaitStats(dirname+"customerWaitStatistic.txt");
		System.out.println("...done");
		travelDistanceEvaluator.printTravelDistanceStatistics();
		taxiCustomerWaitTimeAnalyser.printTaxiCustomerWaitStatistics();

	
	}
	
}
