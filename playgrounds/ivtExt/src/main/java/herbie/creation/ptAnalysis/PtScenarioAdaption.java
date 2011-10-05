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

import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.pt.transitSchedule.TransitRouteImpl;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;

public class PtScenarioAdaption {
	
	public static final double[] headwayClasses = new double[]{
	    4, 7, 10, 15, 30, 60};  // in [minutes]!!
	
	private final static Logger log = Logger.getLogger(PtScenarioAdaption.class);
	private String networkfilePath;
	private String outpath;
	private String transitScheduleFile;
	private String vehiclesFile;
	private ScenarioImpl scenario;
	private TransitScheduleFactory transitFactory = null;

	private TreeMap<Double, Departure> newDepartures;
	private double currentInterval;
	private long pastId;
	private Double pastDeparture;
	
	public static void main(String[] args) {
		if (args.length != 1) {
			log.info("Specify config path."); 
			return;
		}
		
		PtScenarioAdaption ptScenarioAdaption = new PtScenarioAdaption();
		ptScenarioAdaption.init(args[0]);
		ptScenarioAdaption.doubleHeadway();
		ptScenarioAdaption.writeScenario();
	}

	private void init(String file) {
		log.info("Initialization ...");
		
		Config config = new Config();
    	MatsimConfigReader matsimConfigReader = new MatsimConfigReader(config);
    	matsimConfigReader.readFile(file);
    	
		this.networkfilePath = config.findParam("ptScenarioAdaption", "networkfilePath");
		this.outpath = config.findParam("ptScenarioAdaption", "output");
		this.transitScheduleFile = config.findParam("ptScenarioAdaption", "transitScheduleFile");
		this.vehiclesFile = config.findParam("ptScenarioAdaption", "vehiclesFile");
		
		this.scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().scenario().setUseVehicles(true);
		scenario.getConfig().scenario().setUseTransit(true);
		NetworkImpl network = scenario.getNetwork();
		network.setCapacityPeriod(3600.0);
		new MatsimNetworkReader(scenario).parse(networkfilePath);
		
		TransitSchedule schedule = scenario.getTransitSchedule();
		this.transitFactory = schedule.getFactory();
		new TransitScheduleReaderV1(schedule, network, scenario).readFile(transitScheduleFile);
		
		log.info("Initialization ... done");
	}
	
	/**
	 * increase headway according to the values in headwayClasses!
	 */
	private void doubleHeadway() {
		log.info("Double headway ...");
		
		TransitSchedule schedule = this.scenario.getTransitSchedule();
		
		for (TransitLine line : schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				
				
				Map<Id, Departure> departures = route.getDepartures();
				
				if(departures == null || departures.size() < 2) continue;
				
				newDepartures = new TreeMap<Double, Departure>();
				TreeMap<Double, Departure> departuresTimes = new TreeMap<Double, Departure>();
				
				for(Departure departure : departures.values()){
					departuresTimes.put(departure.getDepartureTime(), departure);
				}
						
//				 test:
				for(Double depTime : departuresTimes.keySet()) System.out.println("depTime "+ depTime);
				System.out.println("End test.");
				
				pastId = Long.parseLong(departuresTimes.get(departuresTimes.firstKey()).getId().toString());
				pastDeparture = departuresTimes.firstKey();
				
				for(Double depTime : departuresTimes.keySet()){
					
					if(departuresTimes.lastKey() == depTime) continue;
					
					currentInterval = (departuresTimes.higherKey(depTime) - depTime) / 60d;
					
					if(departuresTimes.lastKey() != (depTime + currentInterval * 60) &&
							(departuresTimes.higherKey(depTime) - depTime) / 60d == currentInterval){
						continue;
					}
					System.out.println();
					addNewDepartures(departuresTimes.higherKey(depTime), departuresTimes.get(depTime));
					pastDeparture = depTime;
				}
				
				this.removeDepartures((TransitRouteImpl) route);
				this.addNewDepartures(route);
				
				System.out.println();
			}
		}
		
		log.info("Double headway ... done");
	}
	
	private void removeDepartures(TransitRouteImpl route) {
		
		Stack<Id> stackId = new Stack<Id>();
		
		for(Id id : route.getDepartures().keySet()){
			stackId.push(id);
		}
		
		while(stackId.size() > 0){
			route.removeDeparture(route.getDepartures().get(stackId.pop()));
		}
		System.out.println();
	}

	private void addNewDepartures(TransitRoute route) {
		for(Departure departure : newDepartures.values()){
			route.addDeparture(departure);
		}
		System.out.println();
	}
	
	private void addNewDepartures(Double upperThreshold, Departure departure) {
		
		// Copy first departure:
		IdImpl newId = new IdImpl(++pastId);
		Departure newDepImpl = this.transitFactory.createDeparture(newId, pastDeparture);
		newDepartures.put(pastDeparture, newDepImpl);
		
		// Add departures if headway is within the range:
		if(currentInterval > headwayClasses[0] && currentInterval <= headwayClasses[headwayClasses.length - 1]) 
		{
			double newInterval = getNewInterval();
			while(pastDeparture + newInterval  *60d < upperThreshold){
				
				newId = new IdImpl(++pastId);
				
				newDepImpl = this.transitFactory.createDeparture(newId, pastDeparture + newInterval * 60d);
				
				newDepartures.put(pastDeparture + newInterval *60d, newDepImpl);
				
				pastDeparture = pastDeparture + newInterval * 60d;
			}
		}
		else
		{
			copyOldDepartures(upperThreshold);
		}
	}
	

	private void copyOldDepartures(double upperThreshold) {
		while ((pastDeparture + currentInterval*60) <= upperThreshold){
			
			IdImpl newId = new IdImpl(++pastId);
			
			Departure newDepImpl = new DepartureImpl(newId, pastDeparture + currentInterval * 60d);
			
			newDepartures.put(pastDeparture + currentInterval *60d, newDepImpl);
			
			pastDeparture = pastDeparture + currentInterval * 60d;
		}
	}

	private double getNewInterval() {
		
		for (int i = 1; i < headwayClasses.length; i++) {
			if(headwayClasses[i] > currentInterval) return headwayClasses[(i-1)];
		}
		return headwayClasses[0];
	}

	private void writeScenario() {
		log.info("Writing new network file ...");
		
		new NetworkWriter(this.scenario.getNetwork()).write(this.outpath + "network.xml.gz");
		new TransitScheduleWriter(this.scenario.getTransitSchedule()).writeFile(this.outpath + "transitSchedule.xml.gz");
		
		log.info("Writing new network file ... done");
	}
}
