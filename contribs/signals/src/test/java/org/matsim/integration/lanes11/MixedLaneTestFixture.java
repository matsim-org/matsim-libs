/* *********************************************************************** *
 * project: org.matsim.*
 * MixedLaneTestFixture
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
package org.matsim.integration.lanes11;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.data.v11.*;
import org.matsim.lanes.data.v20.Lane;

import java.util.ArrayList;
import java.util.List;


/**
 * @author dgrether
 *
 */
public class MixedLaneTestFixture {

	public final Scenario sc;
	public final Id<Node> nid0;
	public final Id<Node> nid1;
	public final Id<Node> nid2;
	public final Id<Node> nid3;
	public final Id<Node> nid4;
	public final Id<Link> id0;
	public final Id<Link> id1;
	public final Id<Link> id2;
	public final Id<Link> id3;
	public final Id<Link> id4;
	public final Id<Lane> laneId1;
	public final Id<Lane> link1FirstLaneId;
	public final Id<Person> pid1;
	public final Id<Person> pid2;
	
	public MixedLaneTestFixture(){
		Config config = ConfigUtils.createConfig();
		config.qsim().setUseLanes(true);

		sc = ScenarioUtils.createScenario(config);
		id0 = Id.create("0", Link.class);
		id1 = Id.create("1", Link.class);
		laneId1 = Id.create("1", Lane.class);
		id2 = Id.create("2", Link.class);
		id3 = Id.create("3", Link.class);
		id4 = Id.create("4", Link.class);
		link1FirstLaneId = Id.create("1.ol", Lane.class);

		nid0 = Id.create("0", Node.class);
		nid1 = Id.create("1", Node.class);
		nid2 = Id.create("2", Node.class);
		nid3 = Id.create("3", Node.class);
		nid4 = Id.create("4", Node.class);

		pid1 = Id.create("1", Person.class);
		pid2 = Id.create("2", Person.class);

		init();
	}

	public MixedLaneTestFixture(double timeStepSize){
		Config config = ConfigUtils.createConfig();
		config.qsim().setUseLanes(true);

		config.qsim().setTimeStepSize(timeStepSize);

		sc = ScenarioUtils.createScenario(config);
		
		nid0 = Id.create("0", Node.class);
		nid1 = Id.create("1", Node.class);
		nid2 = Id.create("2", Node.class);
		nid3 = Id.create("3", Node.class);
		nid4 = Id.create("4", Node.class);
		
		id0 = Id.create("0", Link.class);
		id1 = Id.create("1", Link.class);
		id2 = Id.create("2", Link.class);
		id3 = Id.create("3", Link.class);
		id4 = Id.create("4", Link.class);
		
		laneId1 = Id.create("1", Lane.class);
		link1FirstLaneId = Id.create("1.ol", Lane.class);

		pid1 = Id.create("1", Person.class);
		pid2 = Id.create("2", Person.class);

		init();
	}

	/**
	 * Separate init method so we can also construct this without lanes (for comparison purposes)
	 */
	private void init() {

		Network n = sc.getNetwork();
		NetworkFactoryImpl nb = (NetworkFactoryImpl) n.getFactory();

		// create network
		Node node = null;
		Coord coord = sc.createCoord(0.0, 0.0);
		node = nb.createNode(nid0, coord);
		n.addNode(node);
		coord = sc.createCoord(100.0, 0.0);
		node = nb.createNode(nid1, coord);
		n.addNode(node);
		coord = sc.createCoord(200.0, 0.0);
		node = nb.createNode(nid2, coord);
		n.addNode(node);
		coord = sc.createCoord(200.0, 100.0);
		node = nb.createNode(nid3, coord);
		n.addNode(node);
		coord = sc.createCoord(200.0, -100.0);
		node = nb.createNode(nid4, coord);
		n.addNode(node);

		Link link0 = nb.createLink(id0, n.getNodes().get(nid0) , n.getNodes().get(nid1));
		link0.setLength(100.1);
		link0.setFreespeed(10.0);
		link0.setCapacity(7200.0);
		link0.setNumberOfLanes(2.0);
		n.addLink(link0);
		Link link1 = nb.createLink(id1, n.getNodes().get(nid1), n.getNodes().get(nid2));
		link1.setLength(100.1);
		link1.setFreespeed(10.0);
		link1.setCapacity(7200.0); 
		link1.setNumberOfLanes(2.0);
		n.addLink(link1);
		Link link2 = nb.createLink(id2, n.getNodes().get(nid2), n.getNodes().get(nid3));
		link2.setLength(100.1);
		link2.setFreespeed(10.0);
		link2.setCapacity(7200.0);
		link2.setNumberOfLanes(2.0);
		n.addLink(link2);
		Link link3 = nb.createLink(id3, n.getNodes().get(nid2), n.getNodes().get(nid4));
		link3.setLength(100.1);
		link3.setFreespeed(10.0);
		link3.setCapacity(7200.0);
		link3.setNumberOfLanes(2.0);
		n.addLink(link3);
		//create lanes
		LaneDefinitions11 lanes = new LaneDefinitions11Impl();
		LaneDefinitionsFactory11 lb = lanes.getFactory();
		LaneData11 lane = lb.createLane(Id.create(id1, Lane.class));
		lane.setNumberOfRepresentedLanes(2.0);
		lane.setStartsAtMeterFromLinkEnd(50.0);
		lane.addToLinkId(id2);
		lane.addToLinkId(id3);
		LanesToLinkAssignment11 l2l = lb.createLanesToLinkAssignment(id1);
		l2l.addLane(lane);
		lanes.addLanesToLinkAssignment(l2l);
		LaneDefinitionsV11ToV20Conversion.convertTo20(lanes, this.sc.getLanes(), this.sc.getNetwork());
	}
	
	public void create2PersonPopulation(){
		//create population
		Population pop = sc.getPopulation();
		PopulationFactory pb = pop.getFactory();
		//first person
		Person p = pb.createPerson(Id.create(1, Person.class));
		Plan plan = pb.createPlan();
		Activity act = pb.createActivityFromLinkId("h", id0);
		act.setEndTime(3600.0);
		plan.addActivity(act);
		Leg leg = pb.createLeg(TransportMode.car);
		LinkNetworkRouteImpl route = new LinkNetworkRouteImpl(id0, id2);
		List<Id<Link>> routeList = new ArrayList<Id<Link>>();
		routeList.add(id1);
		route.setLinkIds(id0, routeList, id2);
		leg.setRoute(route);
		plan.addLeg(leg);
		plan.addActivity(pb.createActivityFromLinkId("h", id2));
		p.addPlan(plan);
		pop.addPerson(p);
		//second person
		p = pb.createPerson(Id.create(2, Person.class));
		plan = pb.createPlan();
		act = pb.createActivityFromLinkId("h", id0);
		act.setEndTime(3600.0);
		plan.addActivity(act);
		leg = pb.createLeg(TransportMode.car);
		route = new LinkNetworkRouteImpl(id0, id3);
		route.setLinkIds(id3, routeList, id3);
		leg.setRoute(route);
		plan.addLeg(leg);
		plan.addActivity(pb.createActivityFromLinkId("h", id3));
		p.addPlan(plan);
		pop.addPerson(p);
	}

	
	public void create1PersonFromLink1Population(){
		//create population
		Population pop = sc.getPopulation();
		PopulationFactory pb = pop.getFactory();
		//first person
		Person p = pb.createPerson(Id.create(1, Person.class));
		Plan plan = pb.createPlan();
		Activity act = pb.createActivityFromLinkId("h", id1);
		act.setEndTime(3600.0);
		plan.addActivity(act);
		Leg leg = pb.createLeg(TransportMode.car);
		LinkNetworkRouteImpl route = new LinkNetworkRouteImpl(id1, id2);
		List<Id<Link>> routeList = new ArrayList<Id<Link>>();
//		routeList.add(id1);
		route.setLinkIds(id1, routeList, id2);
		leg.setRoute(route);
		plan.addLeg(leg);
		plan.addActivity(pb.createActivityFromLinkId("h", id2));
		p.addPlan(plan);
		pop.addPerson(p);
	}
	
	
	
}
