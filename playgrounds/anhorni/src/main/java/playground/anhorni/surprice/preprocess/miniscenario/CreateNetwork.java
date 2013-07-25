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

package playground.anhorni.surprice.preprocess.miniscenario;

import java.io.File;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.facilities.OpeningTimeImpl;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.anhorni.surprice.Surprice;

public class CreateNetwork {
	private ScenarioImpl scenario = null;	
	private Config config = null;
	
	private final static Logger log = Logger.getLogger(CreateNetwork.class);		
	
	public void createNetwork(ScenarioImpl scenario, Config config) {
		this.scenario = scenario;
		this.config = config;
		NetworkFactoryImpl networkFactory = new NetworkFactoryImpl(this.scenario.getNetwork());
		
		this.addNodes(networkFactory);
		this.addLinks(networkFactory);
		
		this.write(config.findParam(Surprice.SURPRICE_PREPROCESS, "outPath"));
	}
			
	private void addLinks(NetworkFactoryImpl networkFactory) {		
		int linkCnt = 0;
		int facilityCnt = 0;
		double freeSpeed = 35.0 / 3.6;
		
		double sideLength = Double.parseDouble(config.findParam(Surprice.SURPRICE_PREPROCESS, "sideLength"));
		double spacing = Double.parseDouble(config.findParam(Surprice.SURPRICE_PREPROCESS, "spacing"));
		double linkCapacity = Double.parseDouble(config.findParam(Surprice.SURPRICE_PREPROCESS, "linkCapacity"));
		
		int stepsPerSide = (int)(sideLength / spacing);
		
		for (int i = 0; i <= stepsPerSide ; i++) {
			for (int j = 0; j <= stepsPerSide; j++) {
				Id fromNodeId = new IdImpl(Integer.toString(i * (stepsPerSide + 1) + j));
				Node fromNode = this.scenario.getNetwork().getNodes().get(fromNodeId);
							
				if (j > 0) {
					// create backward link
					Id toNodeId = new IdImpl(Integer.toString(i * (stepsPerSide + 1) + j - 1));
					Node toNode = this.scenario.getNetwork().getNodes().get(toNodeId);
					
					Link l0 = networkFactory.createLink(new IdImpl(Integer.toString(linkCnt)), fromNode, toNode);
					l0.setCapacity(linkCapacity);
					l0.setFreespeed(freeSpeed);				
					l0.setLength(((CoordImpl)fromNode.getCoord()).calcDistance(toNode.getCoord()));
					this.scenario.getNetwork().addLink(l0);
					linkCnt++;
					
					this.addFacility(l0, facilityCnt);
					facilityCnt++;						
					
					Link l1 = networkFactory.createLink(new IdImpl(Integer.toString(linkCnt)), toNode, fromNode);
					l1.setCapacity(linkCapacity);
					l1.setFreespeed(freeSpeed);
					l1.setLength(((CoordImpl)toNode.getCoord()).calcDistance(fromNode.getCoord()));
					this.scenario.getNetwork().addLink(l1);
					linkCnt++;
				}				
				
				if (i > 0) {
					// create downward link
					Id toNodeId = new IdImpl(Integer.toString((i - 1) * (stepsPerSide + 1) + j));
					Node toNode = this.scenario.getNetwork().getNodes().get(toNodeId);
					
					Link l0 = networkFactory.createLink(new IdImpl(Integer.toString(linkCnt)), fromNode, toNode);
					l0.setCapacity(linkCapacity);
					l0.setFreespeed(freeSpeed);
					l0.setLength(((CoordImpl)fromNode.getCoord()).calcDistance(toNode.getCoord()));
					this.scenario.getNetwork().addLink(l0);
					linkCnt++;
					
					this.addFacility(l0, facilityCnt);						
					facilityCnt++;
					
					Link l1 = networkFactory.createLink(new IdImpl(Integer.toString(linkCnt)), toNode, fromNode);
					l1.setCapacity(linkCapacity);
					l1.setFreespeed(freeSpeed);
					l1.setLength(((CoordImpl)fromNode.getCoord()).calcDistance(toNode.getCoord()));
					this.scenario.getNetwork().addLink(l1);
					linkCnt++;
				}
				
			}
		}
		log.info("Created " + linkCnt + " links");
		log.info("Created " + facilityCnt + " facilities");
	}
	
	private void addFacility(Link l, int facilityId) {				
		IdImpl id = new IdImpl(Integer.toString(facilityId));
		ActivityFacility facility = this.scenario.getActivityFacilities().getFactory().createActivityFacility(id, l.getCoord());
		this.scenario.getActivityFacilities().addActivityFacility(facility);
		facility.addActivityOption(this.scenario.getActivityFacilities().getFactory().createActivityOption("home"));
		facility.addActivityOption(this.scenario.getActivityFacilities().getFactory().createActivityOption("work"));
		facility.addActivityOption(this.scenario.getActivityFacilities().getFactory().createActivityOption("shop"));
		facility.addActivityOption(this.scenario.getActivityFacilities().getFactory().createActivityOption("leisure"));
								
		ActivityOptionImpl actOptionHome = (ActivityOptionImpl)facility.getActivityOptions().get("home");
		OpeningTimeImpl opentimeHome = new OpeningTimeImpl(0.0 * 3600.0, 24.0 * 3600);
		actOptionHome.addOpeningTime(opentimeHome);
		
		ActivityOptionImpl actOptionWork = (ActivityOptionImpl)facility.getActivityOptions().get("work");
		OpeningTimeImpl opentimeWork = new OpeningTimeImpl(6.0 * 3600.0, 20.0 * 3600);
		actOptionWork.addOpeningTime(opentimeWork);
		
		ActivityOptionImpl actOptionShop = (ActivityOptionImpl)facility.getActivityOptions().get("shop");
		OpeningTimeImpl opentimeShop = new OpeningTimeImpl(7.5 * 3600.0, 18.5 * 3600);
		actOptionShop.addOpeningTime(opentimeShop);
		
		ActivityOptionImpl actOptionLeisure = (ActivityOptionImpl)facility.getActivityOptions().get("leisure");
		OpeningTimeImpl opentimeLeisure = new OpeningTimeImpl(0.0 * 3600.0, 24.0 * 3600);
		actOptionLeisure.addOpeningTime(opentimeLeisure);
		
		((ActivityFacilityImpl) facility).setLinkId(l.getId());
	}
			
	private void addNodes(NetworkFactoryImpl networkFactory) {		
		double sideLength = Double.parseDouble(config.findParam(Surprice.SURPRICE_PREPROCESS, "sideLength"));
		double spacing = Double.parseDouble(config.findParam(Surprice.SURPRICE_PREPROCESS, "spacing"));
		
		int nodeCnt = 0;
		int stepsPerSide = (int)(sideLength/ spacing);
		for (int i = 0; i <= stepsPerSide ; i++) {
			for (int j = 0; j <= stepsPerSide; j++) {
				Node n = networkFactory.createNode(new IdImpl(Integer.toString(nodeCnt)), new CoordImpl(i * spacing, j * spacing));
				this.scenario.getNetwork().addNode(n);
				nodeCnt++;
			}
		}
		log.info("Created " + nodeCnt + " nodes");
	}
	
	public void write(String path) {
		new File(path).mkdirs();
		this.writeNetwork(path);
		this.writeFacilities(path);
	}
			
	private void writeNetwork(String path) {
		log.info("Writing network ...");
		new NetworkWriter(this.scenario.getNetwork()).write(path + "network.xml");
	}
	
	private void writeFacilities(String path) {
		log.info("Writing facilities ...");
		new FacilitiesWriter(this.scenario.getActivityFacilities()).write(path + "facilities.xml");
	}
}
