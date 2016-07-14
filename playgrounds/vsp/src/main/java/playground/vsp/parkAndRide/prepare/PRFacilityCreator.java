/* *********************************************************************** *
 * project: org.matsim.*
 * PRFacilityCreator.java
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

/**
 * 
 */
package playground.vsp.parkAndRide.prepare;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.scenario.MutableScenario;

import playground.vsp.parkAndRide.PRFacility;


/**
 * @author Ihab
 *
 */

public class PRFacilityCreator {
	
	private MutableScenario scenario;
	
	private double linkCapacity;
	private double freeSpeed;
	private double length;
	private double nrOfLanes;
	
	private List<PRFacility> parkAndRideFacilities = new ArrayList<PRFacility>();
	
	public PRFacilityCreator(MutableScenario scenario) {
		this.scenario = scenario;
	}
	
	public double getLinkCapacity() {
		return linkCapacity;
	}
	public void setLinkCapacity(double linkCapacity) {
		this.linkCapacity = linkCapacity;
	}
	public double getFreeSpeed() {
		return freeSpeed;
	}
	public void setFreeSpeed(double freeSpeed) {
		this.freeSpeed = freeSpeed;
	}
	public double getLength() {
		return length;
	}
	public void setLength(double length) {
		this.length = length;
	}
	public double getNrOfLanes() {
		return nrOfLanes;
	}
	public void setNrOfLanes(double nrOfLanes) {
		this.nrOfLanes = nrOfLanes;
	}
	
	public List<PRFacility> getParkAndRideFacilities() {
		return parkAndRideFacilities;
	}
	
	public void createPRFacility(Id<PRFacility> id, Node node, String stopName, int capacity) {
		
		PRFacility prFacility = new PRFacility();
		prFacility.setId(id);
		prFacility.setStopFacilityName(stopName);
		prFacility.setCapacity(capacity);
		
		Id<Node> pRnodeId1 = Id.create("PR1_"+id, Node.class);
		Id<Node> pRnodeId2 = Id.create("PR2_"+id, Node.class);
		Id<Node> pRnodeId3 = Id.create("PR3_"+id, Node.class);

		Coord coord1 = new Coord(node.getCoord().getX(), node.getCoord().getY() - this.length);
		Coord coord2 = new Coord(node.getCoord().getX() + 10, node.getCoord().getY() - this.length);
		Coord coord3 = new Coord(node.getCoord().getX(), node.getCoord().getY());

		Node prNode1 = scenario.getNetwork().getFactory().createNode(pRnodeId1, coord1);
		Node prNode2 = scenario.getNetwork().getFactory().createNode(pRnodeId2, coord2);
		Node prNode3 = scenario.getNetwork().getFactory().createNode(pRnodeId3, coord3);

		scenario.getNetwork().addNode(prNode1);
		scenario.getNetwork().addNode(prNode2);
		scenario.getNetwork().addNode(prNode3);
		
		Link prLink1in = scenario.getNetwork().getFactory().createLink(Id.create("prLink1in_" + id, Link.class), node, prNode1);
		scenario.getNetwork().addLink(setPRLink(prLink1in));
		Link prLink1out = scenario.getNetwork().getFactory().createLink(Id.create("prLink1out_" + id, Link.class), prNode1, node);
		scenario.getNetwork().addLink(setPRLink(prLink1out));	
		
		Link prLink2in = scenario.getNetwork().getFactory().createLink(Id.create("prLink2in_" + id, Link.class), prNode1, prNode2);
		scenario.getNetwork().addLink(setPseudePRLink(prLink2in));
		Link prLink2out = scenario.getNetwork().getFactory().createLink(Id.create("prLink2out_" + id, Link.class), prNode2, prNode1);
		scenario.getNetwork().addLink(setPseudePRLink(prLink2out));
		
		Link prLink3in = scenario.getNetwork().getFactory().createLink(Id.create("prLink3in_" + id, Link.class), prNode2, prNode3);
		scenario.getNetwork().addLink(setPseudePRLink(prLink3in));
		Link prLink3out = scenario.getNetwork().getFactory().createLink(Id.create("prLink3out_" + id, Link.class), prNode3, prNode2);
		scenario.getNetwork().addLink(setPseudePRLink(prLink3out));	
		
		prFacility.setPrLink1in(prLink1in.getId());
		prFacility.setPrLink1out(prLink1out.getId());
		prFacility.setPrLink2in(prLink2in.getId());
		prFacility.setPrLink2out(prLink2out.getId());
		prFacility.setPrLink3in(prLink3in.getId());
		prFacility.setPrLink3out(prLink3out.getId());

		this.parkAndRideFacilities.add(prFacility);
	}
	
	private Link setPRLink(Link link) {
		Set<String> modes = new HashSet<String>();
		modes.add("car");		
		link.setAllowedModes(modes);
		link.setCapacity(linkCapacity);
		link.setFreespeed(freeSpeed);
		link.setLength(length);
		link.setNumberOfLanes(nrOfLanes);
		return link;
	}
	
	private Link setPseudePRLink(Link link) {
		Set<String> modes = new HashSet<String>();
		modes.add("car");		
		link.setAllowedModes(modes);
		link.setCapacity(linkCapacity);
		link.setFreespeed(100.0);
		link.setLength(1.0);
		link.setNumberOfLanes(40.0);
		return link;
	}
	
}
