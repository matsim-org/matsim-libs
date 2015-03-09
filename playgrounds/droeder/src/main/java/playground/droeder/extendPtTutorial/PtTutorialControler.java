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
package playground.droeder.extendPtTutorial;

import java.io.File;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;

/**
 * @author droeder
 *
 */
class PtTutorialControler {

	@SuppressWarnings("unused")
	private static final Logger log = Logger
			.getLogger(PtTutorialControler.class);

	/**
	 * uses an extended pt-tutorial and calculates accessibility for car 
	 * @param args
	 */
	public static void main(String[] args) {
		if(! new File("../../org.matsim/examples/pt-tutorial/configExtended.xml").exists()){
			ExtendPtTutorial.main(null);
		}
		
		Config config = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(config, "../../org.matsim/examples/pt-tutorial/configExtended.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		Controler controler = new Controler(scenario);
		
		
//		ActivityFacilitiesImpl workLocations = createWorkFacilities(scenario.getPopulation());
//		Network net =  removeNonCarLinks(scenario.getNetwork());
//		
//		GridBasedAccessibilityControlerListenerV3 accessibilityListener = 
//				new GridBasedAccessibilityControlerListenerV3(
//						workLocations,//opportunities, 
//						null, // ptMatrix,
//						controler.getConfig(), 
//						net);
//		accessibilityListener.setComputingAccessibilityForMode(Modes4Accessibility.freeSpeed, true) ;
//		accessibilityListener.setComputingAccessibilityForMode(Modes4Accessibility.car, true);
//		accessibilityListener.generateGridsAndMeasuringPointsByNetwork(251);
//		controler.addControlerListener(accessibilityListener);
		
		controler.setOverwriteFiles(true);
		controler.run();
	}
	
	private static Network removeNonCarLinks(Network network){
		Network net =  NetworkImpl.createNetwork();
		for(Node n: network.getNodes().values()){
			for(Link l: n.getOutLinks().values()){
				if(l.getAllowedModes().contains(TransportMode.car)){
					if(!net.getNodes().containsKey(n.getId())){
						net.addNode(net.getFactory().createNode(n.getId(), n.getCoord()));
						continue;
					}
				}
			}
			for(Link l: n.getInLinks().values()){
				if(l.getAllowedModes().contains(TransportMode.car)){
					if(!net.getNodes().containsKey(n.getId())){
						net.addNode(net.getFactory().createNode(n.getId(), n.getCoord()));
						continue;
					}
				}
			}
		}
		for(Link l: network.getLinks().values()){
			if(l.getAllowedModes().contains(TransportMode.car)){
				Link newLink = net.getFactory().createLink(l.getId(), 
						net.getNodes().get(l.getFromNode().getId()), 
						net.getNodes().get(l.getToNode().getId()));
				newLink.setAllowedModes(l.getAllowedModes());
				newLink.setCapacity(l.getCapacity());
				newLink.setFreespeed(l.getFreespeed());
				newLink.setLength(l.getLength());
				newLink.setNumberOfLanes(l.getNumberOfLanes());
				net.addLink(newLink);
			}
		}
		return net;
	}
	
	private static ActivityFacilitiesImpl createWorkFacilities(Population population){
		ActivityFacilitiesImpl workLocations = new ActivityFacilitiesImpl();
		int i = 0;
		for(Person p: population.getPersons().values()){
			for(PlanElement pe: p.getSelectedPlan().getPlanElements()){
				if( pe instanceof Activity){
					if(((Activity) pe).getType().equals("w")){
						workLocations.createAndAddFacility(Id.create("w" + i++, ActivityFacility.class), ((Activity) pe).getCoord());
					}
				}
			}
		}
		return workLocations;
	}
}

