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
package playground.wrashid.parkingSearch.ppSim.jdepSim;

import java.util.LinkedList;

import org.matsim.analysis.LegHistogram;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.mobsim.framework.Mobsim;

import playground.wrashid.parkingSearch.ppSim.ttmatrix.TTMatrixFromStoredTable;

public class MainPPSimZurich30km {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// Todo change these three paths and try run.

		// String
		// plansFile="Z:/data/experiments/TRBAug2012/input/census2000v2_ZhCut8km_10pct_ivtch.xml/plans_1.xml";
		String plansFile = "c:/data/parkingSearch/psim/zurich/inputs/ktiRun24/1pml_plans_30km.xml.gz";
		String networkFile = "c:/data/parkingSearch/psim/zurich/inputs/ktiRun24/output_network.xml.gz";
		String facilititiesPath = "c:/data/parkingSearch/psim/zurich/inputs/ktiRun24/output_facilities.xml.gz";
		Scenario scenario = GeneralLib.readScenario(plansFile, networkFile, facilititiesPath);
		String outputFolder="C:/data/parkingSearch/psim/zurich/output/";
		
		EventsManager eventsManager = EventsUtils.createEventsManager();

		EventWriterXML eventsWriter = new EventWriterXML(outputFolder + "events.xml.gz");
		eventsManager.addHandler(eventsWriter);
		LegHistogram lh = new LegHistogram(300);
		eventsManager.addHandler(lh);

		eventsManager.resetHandlers(0);
		eventsWriter.init(outputFolder + "events.xml.gz");

		Message.ttMatrix = new TTMatrixFromStoredTable("C:/data/parkingSearch/psim/zurich/inputs/it.50.3600secBin.ttMatrix.txt", scenario.getNetwork());
		
		//TODO: set strategies initially at random
		
		LinkedList<AgentWithParking> agentsMessage=new LinkedList<AgentWithParking>();
		
		for (Person p:scenario.getPopulation().getPersons().values()){
			agentsMessage.add(new AgentWithParking(p));
		}
		
		// TODO: load parking infrastructure files from: 
		//Z:\data\experiments\TRBAug2011\parkings
		
		
		for (int iter=0;iter<10;iter++){
			Mobsim sim = new ParkingPSim(scenario, eventsManager,agentsMessage);
			sim.run();
			eventsManager.finishProcessing();
			
			lh.writeGraphic(outputFolder + "it." + iter + ".legHistogram_all.png");
			lh.writeGraphic(outputFolder + "it." + iter + ".legHistogram_car.png",TransportMode.car);
			lh.writeGraphic(outputFolder + "it." + iter + ".legHistogram_pt.png",TransportMode.pt);
			lh.writeGraphic(outputFolder + "it." + iter + ".legHistogram_ride.png",TransportMode.ride);
			lh.writeGraphic(outputFolder + "it." + iter + ".legHistogram_walk.png",TransportMode.walk);
			eventsWriter.reset(0);
		}
		
		

		
		
		
	}


}

