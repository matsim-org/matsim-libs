/* *********************************************************************** *
 * project: org.matsim.*
 * FhMain.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.fhuelsmann.emission;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;

import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
public class Main {
	
	public static void main (String[] args) throws Exception{

	
		String eventsFile = "../../detailedEval/teststrecke/sim/inputEmissions/events_0807_it.0.txt";
		String netfile ="../../detailedEval/teststrecke/sim/input/network.xml";
//		String visumRoadFile = "../../detailedEval/teststrecke/sim/inputEmissions/visumnetzlink.txt";
		String visumRoadHebefaRoadFile = "../../detailedEval/teststrecke/sim/inputEmissions/road_types.txt";
		String Hbefa_Traffic = "../../detailedEval/teststrecke/sim/inputEmissions/hbefa_emission_factors.txt";

		//create an event object
		EventsManager events = new EventsManagerImpl();	
		
		
		ScenarioImpl scenario = new ScenarioImpl();
		Network network = scenario.getNetwork(); 
		new MatsimNetworkReader(scenario).readFile(netfile);
		
		//create the handler
		DataStructureOfSingleEventAttributes handler = new DataStructureOfSingleEventAttributes(network);
		
		//	TravelTimeCalculation handler = new TravelTimeCalculation();
	
		
		//add the handler
		//events.addHandler(handler);
		events.addHandler(handler);
//		System.out.println(handler.getTravelTimes());

		//create the reader and read the file
		MatsimEventsReader matsimEventReader = new MatsimEventsReader(events);
		
		matsimEventReader.readFile(eventsFile);
		//System.out.println(handler2.getTravelTimes(.);
		//for attributes of the network
		
		
		//here the constructor gets the data structure of the class TravelTimeCalculation
	//	VelocityAverageCalculate vac = new VelocityAverageCalculate(handler.getTravelTimes(),visumRoadFile);
	//	vac.calculateAverageVelocity1();
		
		//vac.printVelocities();
		//System.out.println();
		//System.out.println(vac.getmapOfSingleEvent().get("592536888").get("23933testVehicle").getFirst().getLinkLength());
		
		HbefaTable hbefaTable = new HbefaTable();
		hbefaTable.makeHabefaTable(Hbefa_Traffic);
		HbefaVisum hbefaVisum = new HbefaVisum(handler.getTravelTimes());
		hbefaVisum.creatRoadTypes(visumRoadHebefaRoadFile);
		hbefaVisum.printHbefaVisum();
		hbefaVisum.createMapWithHbefaRoadTypeNumber();	
		
		EmissionFactor emissionFactor = new EmissionFactor(hbefaVisum.map,hbefaTable.getHbefaTableWithSpeedAndEmissionFactor());
		emissionFactor.createEmissionTables();
	//	emissionFactor.printEmissionTable();
		
		
		}
	}	
	
