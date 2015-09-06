/* *********************************************************************** *
 * project: org.matsim.*
 * CreateNetwork.java
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

package playground.staheale.miniscenario;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.ActivityOptionImpl;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.OpeningTimeImpl;


public class CreateNetwork {
	private ScenarioImpl scenario = null;	
	private Config config = null;
	public static ArrayList<String> days = new ArrayList<String>(Arrays.asList("mon", "tue", "wed", "thu", "fri", "sat", "sun"));
	public static ArrayList<String> modes = new ArrayList<String>(Arrays.asList("car", "pt", "bike", "walk"));
	public static final String AGENT_INTERACTION_RUN = "agent_interaction_run";
	public static final String AGENT_INTERACTION_PREPROCESS = "agent_interaction_preprocess";

	private final static Logger log = Logger.getLogger(CreateNetwork.class);		

	public void createNetwork(ScenarioImpl scenario, Config config) {
		this.scenario = scenario;
		this.config = config;
		NetworkFactoryImpl networkFactory = new NetworkFactoryImpl(this.scenario.getNetwork());

		this.addNodes(networkFactory);
		this.addLinks(networkFactory);

		this.write(config.findParam(AGENT_INTERACTION_PREPROCESS, "outPath"));
	}

	private void addLinks(NetworkFactoryImpl networkFactory) {		
		int linkCnt = 0;
		int facilityCnt = 0;
		double freeSpeed = 35.0 / 3.6;

		double sideLength = Double.parseDouble(config.findParam(AGENT_INTERACTION_PREPROCESS, "sideLength"));
		double spacing = Double.parseDouble(config.findParam(AGENT_INTERACTION_PREPROCESS, "spacing"));
		double linkCapacity = Double.parseDouble(config.findParam(AGENT_INTERACTION_PREPROCESS, "linkCapacity"));

		int stepsPerSide = (int)(sideLength / spacing);

		for (int i = 0; i <= stepsPerSide ; i++) {

			for (int j = 0; j <= stepsPerSide; j++) {
				Id<Node> fromNodeId = Id.create(Integer.toString(i * (stepsPerSide + 1) + j), Node.class);
				Node fromNode = this.scenario.getNetwork().getNodes().get(fromNodeId);

				if (j > 0) {
					// create backward link
					Id<Node> toNodeId = Id.create(Integer.toString(i * (stepsPerSide + 1) + j - 1), Node.class);
					Node toNode = this.scenario.getNetwork().getNodes().get(toNodeId);

					Link l0 = networkFactory.createLink(Id.create(Integer.toString(linkCnt), Link.class), fromNode, toNode);
					l0.setCapacity(linkCapacity);
					l0.setFreespeed(freeSpeed);
					l0.setLength(CoordUtils.calcDistance(fromNode.getCoord(), toNode.getCoord()));
					this.scenario.getNetwork().addLink(l0);
					linkCnt++;
					this.addFacility(l0, facilityCnt);
					facilityCnt++;						

					Link l1 = networkFactory.createLink(Id.create(linkCnt, Link.class), toNode, fromNode);
					l1.setCapacity(linkCapacity);
					l1.setFreespeed(freeSpeed);
					l1.setLength(CoordUtils.calcDistance(toNode.getCoord(), fromNode.getCoord()));
					this.scenario.getNetwork().addLink(l1);
					linkCnt++;
				}				

				if (i > 0) {
					// create downward link
					Id<Node> toNodeId = Id.create(Integer.toString((i - 1) * (stepsPerSide + 1) + j), Node.class);
					Node toNode = this.scenario.getNetwork().getNodes().get(toNodeId);

					Link l0 = networkFactory.createLink(Id.create(Integer.toString(linkCnt), Link.class), fromNode, toNode);
					l0.setCapacity(linkCapacity);
					l0.setFreespeed(freeSpeed);
					l0.setLength(CoordUtils.calcDistance(fromNode.getCoord(), toNode.getCoord()));
					this.scenario.getNetwork().addLink(l0);
					linkCnt++;

					this.addFacility(l0, facilityCnt);						
					facilityCnt++;

					Link l1 = networkFactory.createLink(Id.create(Integer.toString(linkCnt), Link.class), toNode, fromNode);
					l1.setCapacity(linkCapacity);
					l1.setFreespeed(freeSpeed);
					l1.setLength(CoordUtils.calcDistance(fromNode.getCoord(), toNode.getCoord()));
					this.scenario.getNetwork().addLink(l1);
					linkCnt++;
				}

			}
		}
		log.info("Created " + linkCnt + " links");
		log.info("Created " + facilityCnt + " facilities");
		log.info("number of work facilities: " +scenario.getActivityFacilities().getFacilitiesForActivityType("work").size());
		log.info("number of shop retail facilities: " +scenario.getActivityFacilities().getFacilitiesForActivityType("shop_retail").size());
		log.info("number of shop service facilities: " +scenario.getActivityFacilities().getFacilitiesForActivityType("shop_service").size());
		log.info("number of leisure_sports & fun facilities: " +scenario.getActivityFacilities().getFacilitiesForActivityType("leisure_sports_fun").size());
		log.info("number of leisure_gastro & culture facilities: " +scenario.getActivityFacilities().getFacilitiesForActivityType("leisure_gastro_culture").size());

	}

	private void addFacility(Link l, int facilityId) {
		int idnumber = facilityId;
		Id<ActivityFacility> id = Id.create(Integer.toString(facilityId), ActivityFacility.class);
		this.scenario.getActivityFacilities().addActivityFacility(this.scenario.getActivityFacilities().getFactory().createActivityFacility(id, l.getCoord()));

		Random random = new Random(4711+idnumber);
		ActivityFacilityImpl facility = (ActivityFacilityImpl)(this.scenario.getActivityFacilities().getFacilities().get(id));
		facility.createActivityOption("home");
		facility.createActivityOption("work");
		if (random.nextDouble()<0.0003){
			facility.createActivityOption("shop_retail");
			facility.getActivityOptions().remove("home");
		}
		if (random.nextDouble()<0.001){
			facility.createActivityOption("shop_service");
			facility.getActivityOptions().remove("home");
			//log.info("created shop service facility");
		}
		if (random.nextDouble()<0.004){
			facility.createActivityOption("leisure_sports_fun");
			facility.getActivityOptions().remove("home");
		}
		if (random.nextDouble()<0.004){
			facility.createActivityOption("leisure_gastro_culture");
			facility.getActivityOptions().remove("home");
		}
		//		
		//    	if (facility.getActivityOptions().containsKey("home")){
		//    		ActivityOptionImpl actOptionHome = (ActivityOptionImpl)facility.getActivityOptions().get("home");
		//    		OpeningTimeImpl opentimeHome = new OpeningTimeImpl(DayType.wk, 0.0 * 3600.0, 24.0 * 3600);
		//    		actOptionHome.addOpeningTime(opentimeHome);
		//    	}

		ActivityOptionImpl actOptionWork = (ActivityOptionImpl)facility.getActivityOptions().get("work");
		OpeningTimeImpl opentimeWork = new OpeningTimeImpl(6.0 * 3600.0, 20.0 * 3600);
		actOptionWork.addOpeningTime(opentimeWork);

		if (facility.getActivityOptions().containsKey("shop_retail")){
			ActivityOptionImpl actOptionShopRetail = (ActivityOptionImpl)facility.getActivityOptions().get("shop_retail");
			OpeningTimeImpl opentimeShopRetail = new OpeningTimeImpl(7.5 * 3600.0, 19.0 * 3600);
			actOptionShopRetail.addOpeningTime(opentimeShopRetail);
			double cap = 2+random.nextInt(200);
			actOptionShopRetail.setCapacity(cap);
		}
		if (facility.getActivityOptions().containsKey("shop_service")){
			ActivityOptionImpl actOptionShopService = (ActivityOptionImpl)facility.getActivityOptions().get("shop_service");
			OpeningTimeImpl opentimeShopService = new OpeningTimeImpl(8.0 * 3600.0, 19.0 * 3600);
			actOptionShopService.addOpeningTime(opentimeShopService);
			double cap = 2+random.nextInt(29);
			actOptionShopService.setCapacity(cap);
			//log.info("shop service opentimes added");
		}
		if (facility.getActivityOptions().containsKey("leisure_sports_fun")){
			ActivityOptionImpl actOptionSportsFun = (ActivityOptionImpl)facility.getActivityOptions().get("leisure_sports_fun");
			OpeningTimeImpl opentimeSportsFun = new OpeningTimeImpl(9.0 * 3600.0, 24.0 * 3600);
			actOptionSportsFun.addOpeningTime(opentimeSportsFun);
			double cap = 2+random.nextInt(44);
			actOptionSportsFun.setCapacity(cap);
		}
		if (facility.getActivityOptions().containsKey("leisure_gastro_culture")){
			ActivityOptionImpl actOptionGastroCulture = (ActivityOptionImpl)facility.getActivityOptions().get("leisure_gastro_culture");
			OpeningTimeImpl opentimeGastroCulture = new OpeningTimeImpl(9.0 * 3600.0, 24.0 * 3600);
			actOptionGastroCulture.addOpeningTime(opentimeGastroCulture);
			double cap = 2+random.nextInt(61);
			actOptionGastroCulture.setCapacity(cap);
		}
		facility.setLinkId(l.getId());
	}

	private void addNodes(NetworkFactoryImpl networkFactory) {		
		double sideLength = Double.parseDouble(config.findParam(AGENT_INTERACTION_PREPROCESS, "sideLength"));
		double spacing = Double.parseDouble(config.findParam(AGENT_INTERACTION_PREPROCESS, "spacing"));

		int nodeCnt = 0;
		int stepsPerSide = (int)(sideLength/ spacing);
		for (int i = 0; i <= stepsPerSide ; i++) {
			for (int j = 0; j <= stepsPerSide; j++) {
				Node n = networkFactory.createNode(Id.create(nodeCnt, Node.class), new Coord(i * spacing, j * spacing));
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
