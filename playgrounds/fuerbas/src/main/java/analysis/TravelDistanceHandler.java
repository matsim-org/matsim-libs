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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

public class TravelDistanceHandler implements LinkLeaveEventHandler, LinkEnterEventHandler {
	
	
	HashMap<Id, Double> distance = new HashMap<Id, Double>();
	Network network = null;
	Config config = null;
	Scenario scen = null;
//	String networkfile = "/home01/sfuerbas/workspace/893.output_network.xml.gz";
//	String plansfile = "/home01/sfuerbas/workspace/893.2200.plans.xml.gz";
	
	String networkfile = "/home/soeren/workspace2/matsim-0.3.0/output/azores_small/output_network.xml.gz";
	String plansfile = "/home/soeren/workspace2/matsim-0.3.0/output/azores_small/ITERS/it.10/10.plans.xml.gz";
	
	public TravelDistanceHandler() {
		this.distance = distance;
	}
	
	public void createHandlerScenario() {
		Config config1 = ConfigUtils.createConfig();
		this.scen = (ScenarioImpl) ScenarioUtils.createScenario(config1);
		this.config = scen.getConfig();
		this.config.network().setInputFile(networkfile);
		this.config.plans().setInputFile(plansfile);
		ScenarioUtils.loadScenario(scen);
		this.network = scen.getNetwork();
	}
	
	
	public void returnDistances() throws IOException{
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File("/home/soeren/workspace2/matsim-0.3.0/output/azores_small/distances.txt")));
		
		for (Person person: scen.getPopulation().getPersons().values()) {
			bw.write(person.getId()+" : "+this.distance.get(person.getId()));
			bw.newLine();
		}
		bw.flush();
		bw.close();
		System.out.println("output written");
	}
	
	

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
						
		if (this.distance.containsKey(event.getPersonId())) {
			double distanceSoFar = this.distance.get(event.getPersonId());
			this.distance.put(event.getPersonId(), distanceSoFar+this.network.getLinks().get(event.getLinkId()).getLength());
		}
		else {
			this.distance.put(event.getPersonId(), this.network.getLinks().get(event.getLinkId()).getLength());
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		// TODO Auto-generated method stub
		
	}
	
}