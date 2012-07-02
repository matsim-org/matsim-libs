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
package playground.ikaddoura.parkAndRide.prepare;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;

import playground.ikaddoura.parkAndRide.pR.ParkAndRideFacility;

/**
 * @author Ihab
 *
 */

public class PRFacilityCreator {
	
	private double capacity;
	private double freeSpeed;
	private double length;
	private double nrOfLanes;
	
	private List<ParkAndRideFacility> parkAndRideFacilities = new ArrayList<ParkAndRideFacility>();
	
	public double getCapacity() {
		return capacity;
	}
	public void setCapacity(double capacity) {
		this.capacity = capacity;
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
	
	public List<ParkAndRideFacility> getParkAndRideFacilities() {
		return parkAndRideFacilities;
	}
	
	public void createPRFacility(Id id, Node node, Scenario scenario, String stopName) {
		
		ParkAndRideFacility prFacility = new ParkAndRideFacility();
		prFacility.setId(id);
		prFacility.setStopFacilityName(stopName);
		
		Id pRnodeId1 = new IdImpl("PR1_"+id);
		Id pRnodeId2 = new IdImpl("PR2_"+id);
		Id pRnodeId3 = new IdImpl("PR3_"+id);
		
		Coord coord1 = scenario.createCoord(node.getCoord().getX(), node.getCoord().getY() - this.length);
		Coord coord2 = scenario.createCoord(node.getCoord().getX() + 10, node.getCoord().getY() - this.length);
		Coord coord3 = scenario.createCoord(node.getCoord().getX(), node.getCoord().getY());

		Node prNode1 = scenario.getNetwork().getFactory().createNode(pRnodeId1, coord1);
		Node prNode2 = scenario.getNetwork().getFactory().createNode(pRnodeId2, coord2);
		Node prNode3 = scenario.getNetwork().getFactory().createNode(pRnodeId3, coord3);

		scenario.getNetwork().addNode(prNode1);
		scenario.getNetwork().addNode(prNode2);
		scenario.getNetwork().addNode(prNode3);
		
		Link prLink1in = scenario.getNetwork().getFactory().createLink(new IdImpl("prLink1in_" + id), node, prNode1);
		scenario.getNetwork().addLink(setPRLink(prLink1in));
		Link prLink1out = scenario.getNetwork().getFactory().createLink(new IdImpl("prLink1out_" + id), prNode1, node);
		scenario.getNetwork().addLink(setPRLink(prLink1out));	
		
		Link prLink2in = scenario.getNetwork().getFactory().createLink(new IdImpl("prLink2in_" + id), prNode1, prNode2);
		scenario.getNetwork().addLink(setPseudePRLink(prLink2in));
		Link prLink2out = scenario.getNetwork().getFactory().createLink(new IdImpl("prLink2out_" + id), prNode2, prNode1);
		scenario.getNetwork().addLink(setPseudePRLink(prLink2out));
		
		Link prLink3in = scenario.getNetwork().getFactory().createLink(new IdImpl("prLink3in_" + id), prNode2, prNode3);
		scenario.getNetwork().addLink(setPseudePRLink(prLink3in));
		Link prLink3out = scenario.getNetwork().getFactory().createLink(new IdImpl("prLink3out_" + id), prNode3, prNode2);
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
		link.setAllowedModes((Set<String>) modes);
		link.setCapacity(capacity);
		link.setFreespeed(freeSpeed);
		link.setLength(length);
		link.setNumberOfLanes(nrOfLanes);
		return link;
	}
	
	private Link setPseudePRLink(Link link) {
		Set<String> modes = new HashSet<String>();
		modes.add("car");		
		link.setAllowedModes((Set<String>) modes);
		link.setCapacity(capacity);
		link.setFreespeed(100.0);
		link.setLength(1.0);
		link.setNumberOfLanes(40.0);
		return link;
	}
	
}
