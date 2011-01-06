/* *********************************************************************** *
 * project: org.matsim.*
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

package analysis;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.ScenarioFactoryImpl;
import org.matsim.core.api.experimental.ScenarioLoader;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;

import playground.benjamin.analysis.BkAnalysis;

public class TravelDistanceHandler implements LinkLeaveEventHandler, LinkEnterEventHandler {
	
	
	HashMap<Id, Double> distance = new HashMap<Id, Double>();
	Network network = null;
//	String networkfile = "/home01/sfuerbas/workspace/893.output_network.xml.gz";
//	String plansfile = "/home01/sfuerbas/workspace/893.2200.plans.xml.gz";
	
	String networkfile = "893.output_network.xml.gz";
	String plansfile = "893.2200.plans.xml.gz";
	
	public TravelDistanceHandler() {
		this.distance = distance;
	}
	
	public void createHandlerScenario() {
		Scenario scen = new ScenarioFactoryImpl().createScenario();
		Config config = scen.getConfig();
		config.network().setInputFile(networkfile);
		config.plans().setInputFile(plansfile);
		ScenarioLoader sl = new ScenarioLoaderImpl(scen);
		sl.loadScenario();
		network = scen.getNetwork();
	}
	
	
	public void returnDistances(){
		System.out.println(this.distance.toString());
	}
	
	

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
				
		if (this.distance.containsKey(event.getPersonId())) {
			double distanceSoFar = this.distance.get(event.getPersonId());
			this.distance.put(event.getPersonId(), distanceSoFar+network.getLinks().get(event.getLinkId()).getLength());
		}
		else
			this.distance.put(event.getPersonId(), network.getLinks().get(event.getLinkId()).getLength());
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		// TODO Auto-generated method stub
		
	}
	
}