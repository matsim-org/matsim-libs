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
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.PopulationReader;
import org.matsim.population.PopulationReaderMatsimV4;

import playground.wrashid.DES.Road;
import playground.wrashid.DES.Scheduler;
import playground.wrashid.DES.SimulationParameters;
import playground.wrashid.DES.Vehicle;
import playground.wrashid.DES.utils.Timer;

public class JavaDEQSim {

	Population population;
	NetworkLayer network;
	
	public JavaDEQSim(final NetworkLayer network, final Population population, final Events events) {
		// constructor
		


		
		this.population = population;
		this.network = network;
		
		
		
		// initialize Simulation parameters
		SimulationParameters.linkCapacityPeriod=network.getCapacityPeriod();
		SimulationParameters.events=events;
		SimulationParameters.stuckTime= Double.parseDouble(Gbl.getConfig().getParam("simulation", "stuckTime"));
		SimulationParameters.flowCapacityFactor= Double.parseDouble(Gbl.getConfig().getParam("simulation", "flowCapacityFactor"));
		SimulationParameters.storageCapacityFactor = Double.parseDouble(Gbl.getConfig().getParam("simulation", "storageCapacityFactor"));
		
		
		// allowed testing to hook in here
		if (SimulationParameters.testEventHandler!=null){
			events.addHandler(SimulationParameters.testEventHandler);
		}
		
		if (SimulationParameters.testPlanPath!=null){
			// read population
			Population pop=new Population(Population.NO_STREAMING);;
			PopulationReader plansReader = new MatsimPopulationReader(pop);
			plansReader.readFile(SimulationParameters.testPlanPath);
			
			this.population=pop;
			
		}
		
		if (SimulationParameters.testPopulationModifier!=null){
			this.population=SimulationParameters.testPopulationModifier.modifyPopulation(this.population);
		}
		
	}
	
	public void run() {
		//System.out.println("JavaDEQSim.run");
		Timer t=new Timer();
		t.startTimer();
		
		
		Scheduler scheduler=new Scheduler();
		Road.allRoads=new HashMap<String,Road>();

		
		// initialize network
		Road road=null;
		for (Link link: network.getLinks().values()){
			road= new Road(scheduler,link);
			Road.allRoads.put(link.getId().toString(), road);
		}
		
		// initialize vehicles
		
		
		Vehicle vehicle=null;
		for (Person person : this.population.getPersons().values()) {
			vehicle =new Vehicle(scheduler,person);
		}
		
		

		
		scheduler.startSimulation();
		
		

		
		
		t.endTimer();
		t.printMeasuredTime("Time needed for one iteration (only DES part): ");
		
		
		// print output
		//for(int i=0;i<SimulationParameters.eventOutputLog.size();i++) {
		//	SimulationParameters.eventOutputLog.get(i).print();
		//}
		
	}
}
