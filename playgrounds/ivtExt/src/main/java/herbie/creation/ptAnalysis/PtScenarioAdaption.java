/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package herbie.creation.ptAnalysis;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.TransitRouteImpl;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.*;
import utils.Bins;

import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

public class PtScenarioAdaption {
	
	public static final double[] relHeadwayClasses = new double[]{
	    4, 7, 10, 15, 30, 60};  // in [minutes] and ascending order!!
	private static double[] relevantBinInterval = new double[]{0.0, 61.0}; // relevant interval graphical output
	
	private final static Logger log = Logger.getLogger(PtScenarioAdaption.class);
	private String networkfilePath;
	private String outpath;
	private String transitScheduleFile;
	private String transitVehicleFile;
	private MutableScenario scenario;
	private TransitScheduleFactory transitFactory = null;

	private TreeMap<Double, Departure> departuresTimes;
	private TreeMap<Double, Departure> newDepartures;
	private double currentInterval;
	private Double implDeparture;
	
	TreeMap<Id, Vehicle> newVehiclesMap;
	int vehicleNumber;
	private Vehicle currentVehicle;
	
	private Bins old_hdwy_distrib;
	private Bins new_hdwy_distrib;
	
	private int old_NrOfDepartures;
	private int new_NrOfDepartures;
	
	public static void main(String[] args) {
		if (args.length != 1) {
			log.info("Specify config path."); 
			return;
		}
		
		PtScenarioAdaption ptScenarioAdaption = new PtScenarioAdaption();
		ptScenarioAdaption.initConfig(args[0]);
		ptScenarioAdaption.initScenario();
		ptScenarioAdaption.doubleHeadway();
		ptScenarioAdaption.writeScenario();
		ptScenarioAdaption.evaluateSchedule();
	}

	private void initConfig(String file) {
		log.info("InitConfig ...");
		
		Config config = new Config();
    	ConfigReader configReader = new ConfigReader(config);
    	configReader.readFile(file);
    	
		this.networkfilePath = config.findParam("ptScenarioAdaption", "networkfilePath");
		this.outpath = config.findParam("ptScenarioAdaption", "output");
		this.transitScheduleFile = config.findParam("ptScenarioAdaption", "transitScheduleFile");
		this.transitVehicleFile = config.findParam("ptScenarioAdaption", "transitVehicleFile");
		
		this.old_hdwy_distrib = new Bins(1, relevantBinInterval[1], "Old Headway Distribution");
		this.new_hdwy_distrib = new Bins(1, relevantBinInterval[1], "New Headway Distribution");
		
		log.info("InitConfig ... done");
	}

	private void initScenario() {
		log.info("Initialization ...");
		
		this.scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().scenario().setUseVehicles(true);
		scenario.getConfig().transit().setUseTransit(true);
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		network.setCapacityPeriod(3600.0);
		new MatsimNetworkReader(scenario.getNetwork()).parse(networkfilePath);
		
		TransitSchedule schedule = scenario.getTransitSchedule();
		this.transitFactory = schedule.getFactory();
		new TransitScheduleReaderV1(schedule, network).readFile(transitScheduleFile);
		
		Vehicles vehicles = scenario.getTransitVehicles();
		new VehicleReaderV1(vehicles).readFile(transitVehicleFile);
		
		log.info("Initialization ... done");
	}
	
	/**
	 * increase headway according to the values in headwayClasses!
	 */
	private void doubleHeadway() {
		log.info("DoubleHeadway ...");
		
		TransitSchedule schedule = this.scenario.getTransitSchedule();
		
		newVehiclesMap = new TreeMap<Id, Vehicle>();
		vehicleNumber = 0;
		
		for (TransitLine line : schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				
				Map<Id<Departure>, Departure> departures = route.getDepartures();
				
				if(departures == null) continue;
				
				newDepartures = new TreeMap<Double, Departure>();
				
				departuresTimes = new TreeMap<Double, Departure>();
				for(Departure departure : departures.values()){
					departuresTimes.put(departure.getDepartureTime(), departure);
					old_NrOfDepartures++;
				}
				
				implDeparture = departuresTimes.firstKey();
				Id vehicleId = departuresTimes.get(implDeparture).getVehicleId();
				currentVehicle = scenario.getTransitVehicles().getVehicles().get(vehicleId);
				
				copyFirstDeparture();
				
				if(departures.size() >= 2) {
					
					considerHeadwayAdaption();
				}
				
				this.removeDepartures((TransitRouteImpl) route);
				this.copyNewDepartures(route);
				
			}
		}
		this.removeVehicles();
		this.copyNewVehicles();
		
		log.info("DoubleHeadway ... done");
	}
	
	private void considerHeadwayAdaption() {
		
		currentInterval = (departuresTimes.higherKey(implDeparture) - implDeparture) / 60d;
		
		for(Double depTime : departuresTimes.keySet()){
			
			Id vehicleId = departuresTimes.get(depTime).getVehicleId();
			currentVehicle = scenario.getTransitVehicles().getVehicles().get(vehicleId);
			
			if(departuresTimes.lastKey() != depTime &&
					(departuresTimes.higherKey(depTime) - depTime) / 60d == currentInterval){
				continue;
			}
			
			if(departuresTimes.lastKey() == depTime) {
				addNewDepartures(depTime + currentInterval * 60d - getNewInterval() * 60d);
				
				if(isElementOfConsideredInterval(currentInterval)) old_hdwy_distrib.addVal(currentInterval, 1d);
			}
			else {
				
				addNewDepartures((depTime));
				currentInterval = (departuresTimes.higherKey(depTime) - depTime) / 60d;
				
				if(isElementOfConsideredInterval(currentInterval)) old_hdwy_distrib.addVal(currentInterval, 1d);
			}
		}
	}

	private void copyFirstDeparture() {
		
		setNewDeparture();
	}

	private void removeVehicles() {
		
		scenario.getTransitVehicles().getVehicles().clear();
	}
	
	private void copyNewVehicles() {
		
		for(Id id : newVehiclesMap.keySet()){
			
			scenario.getTransitVehicles().addVehicle( newVehiclesMap.get(id));
		}
	}

	private void removeDepartures(TransitRouteImpl route) {
		
		Stack<Id> stackId = new Stack<Id>();
		
		while(stackId.size() > 0){
			route.removeDeparture(route.getDepartures().get(stackId.pop()));
		}
	}

	private void copyNewDepartures(TransitRoute route) {
		
		
		for(Departure departure : newDepartures.values()){
			
			route.addDeparture(departure);
			new_NrOfDepartures++;
		}
	}
	
	private void addNewDepartures(Double upperThreshold) {
		
		if(currentIntervalIsRelevant())
		{
			double newInterval = getNewInterval();

			while(implDeparture  < upperThreshold){
				
				implDeparture = implDeparture + newInterval * 60d;
				
				setNewDeparture();
			}
			if(isElementOfConsideredInterval(newInterval)) new_hdwy_distrib.addVal(newInterval, 1d);
		}
		else
		{	
			copyExistingDepartures(upperThreshold);
			if(isElementOfConsideredInterval(currentInterval)) new_hdwy_distrib.addVal(currentInterval, 1d);
		}
	}

	private boolean currentIntervalIsRelevant() {
		 return (currentInterval > relHeadwayClasses[0] && 
				 currentInterval <= relHeadwayClasses[relHeadwayClasses.length - 1]);
	}

	private void copyExistingDepartures(double upperThreshold) {
		
		while(implDeparture  < upperThreshold){
			
			implDeparture = implDeparture + currentInterval * 60d;
			
			setNewDeparture();
		}
	}

	private void setNewDeparture() {
		
		Id<Departure> newId = Id.create(vehicleNumber, Departure.class);
		
		Departure newDepImpl = this.transitFactory.createDeparture(newId, implDeparture);
		
		Id<Vehicle> newVehicleId = Id.create("tr_" + vehicleNumber, Vehicle.class);
		
		newDepImpl.setVehicleId(newVehicleId);
		newDepartures.put(implDeparture, newDepImpl);
		
		currentVehicle = new VehicleImpl(newVehicleId, currentVehicle.getType());
		
		
		newVehiclesMap.put(newVehicleId, currentVehicle);
		
		vehicleNumber++;
	}

	private boolean isElementOfConsideredInterval(double headway) {
		
		if(headway > relevantBinInterval[0] 
				&& headway < relevantBinInterval[1]) return true;
		return false;
	}

	private double getNewInterval() {
		
		if(relHeadwayClasses[0] > currentInterval) return currentInterval;
		
		for (int i = 1; i < relHeadwayClasses.length; i++) {
			if(relHeadwayClasses[i] >= currentInterval) return relHeadwayClasses[(i-1)];
		}
		return currentInterval;
	}

	private void writeScenario() {
		log.info("Writing new network file ...");
		
		new TransitScheduleWriter(this.scenario.getTransitSchedule()).writeFile(this.outpath + "newTransitSchedule.xml.gz");
		
		new VehicleWriterV1(this.scenario.getTransitVehicles()).writeFile(this.outpath + "newTransitVehicles.xml.gz");
		
		log.info("Writing new network file ... done");
	}
	
	private void evaluateSchedule() {
		log.info("Statistics ...");
		
		old_hdwy_distrib.plotBinnedDistribution(this.outpath + "HeadwayDistribution", "Headway", "#", "Number of departures with same headway");
		new_hdwy_distrib.plotBinnedDistribution(this.outpath + "HeadwayDistribution", "Headway", "#", "Number of departures with same headway");
		
		log.info("Number of departures in the old schedule: " + old_NrOfDepartures);
		log.info("Number of departures in the new schedule: " + new_NrOfDepartures);
		
		log.info("Statistics ... done");
	}
}
