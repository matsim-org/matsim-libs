/* *********************************************************************** *
 * project: org.matsim.*
 * JavaDEQSim.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.wrashid.deqsim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.basic.v01.Id;
import org.matsim.events.Events;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;

import playground.wrashid.PDES2.Road;
import playground.wrashid.PDES2.Scheduler;
import playground.wrashid.PDES2.SimulationParameters;
import playground.wrashid.PDES2.Vehicle;
import playground.wrashid.DES.utils.Timer;

public class JavaPDEQSim2 {

	final Population population;
	final NetworkLayer network;
	
	public JavaPDEQSim2(final NetworkLayer network, final Population population, final Events events) {
		// constructor
		


		
		this.population = population;
		this.network = network;
		
		
		
		// initialize Simulation parameters
		SimulationParameters.linkCapacityPeriod=network.getCapacityPeriod();
		SimulationParameters.events=events;
		SimulationParameters.stuckTime= Double.parseDouble(Gbl.getConfig().getParam("simulation", "stuckTime"));
		SimulationParameters.flowCapacityFactor= Double.parseDouble(Gbl.getConfig().getParam("simulation", "flowCapacityFactor"));
		SimulationParameters.flowCapacityFactor= Double.parseDouble(Gbl.getConfig().getParam("simulation", "storageCapacityFactor"));
		
	}
	
	public void run() {
		//System.out.println("JavaDEQSim.run");
		Timer t=new Timer();
		t.startTimer();
		
		
		Scheduler scheduler=new Scheduler();
		Road.allRoads=new HashMap<String,Road>();

		
		// initialize network (roads)
		Road road=null;
		for (Link link: network.getLinks().values()){
			road= new Road(scheduler,link);
			double xCoordinate= road.getXCoordinate();
			
			if (xCoordinate<SimulationParameters.minXCoodrinate){
				SimulationParameters.minXCoodrinate=xCoordinate;
			} else if (xCoordinate>SimulationParameters.maxXCoodrinate){
				SimulationParameters.maxXCoodrinate=xCoordinate;
			}
			
			Road.allRoads.put(link.getId().toString(), road);
		}
		
		SimulationParameters.xZoneDistance=(SimulationParameters.maxXCoodrinate-SimulationParameters.minXCoodrinate)/SimulationParameters.numberOfZones;

		
		for (int i=1;i<=SimulationParameters.numberOfZones;i++){
			System.out.println(i+"-th boundry:" + i*SimulationParameters.xZoneDistance);
		}
		
		
		// assign a zone to each road
		for (Road r:Road.allRoads.values()){
			r.initializeZoneIdEquiXDistance();
		}
		
		// 
		scheduler.inititZoneMessageQueues();
		
		
		
		
		
		// initialize vehicles
		
		
		Vehicle vehicle=null;
		for (Person person : this.population.getPersons().values()) {
			vehicle =new Vehicle(scheduler,person);
		}
		
		

		
		scheduler.startSimulation();
		
		

		
		
		t.endTimer();
		t.printMeasuredTime("Time needed for one iteration (only PDES part): ");
		
		
		// print output
		//for(int i=0;i<SimulationParameters.eventOutputLog.size();i++) {
		//	SimulationParameters.eventOutputLog.get(i).print();
		//}
		
	}
}
