/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.agarwalamit.fundamentalDiagrams;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

/**
 * Created by amit on 16/02/2017.
 */

final class FDNetworkGenerator {

	private final int noOfSides = 3;
	private final Id<Link> startLinkId = Id.createLinkId("home");
	private final Id<Link> endLinkId = Id.createLinkId("work");

	private Id<Link> firstLinkOfTrack; // base link
	private Id<Link> lastLinkOfBase; // same as firstLinkOfTrack if subdivision==1
	private Id<Link> firstLinkOfMiddleSide; // link just after lastLinkOfBase
	private Id<Link> lastLinkOfTrack; // left link

	private final RaceTrackLinkProperties linkProperties;

	/**
	 * @param linkProperties of the equilateral triangle; only properties of the link will be used.
	 */
	public FDNetworkGenerator(final RaceTrackLinkProperties linkProperties) {
		this.linkProperties = linkProperties;
	}

	public void createNetwork(final Scenario scenario) {
		createTriangularNetwork(scenario);
	}

	/**
	 * It will generate a triangular network.
	 * Each link is subdivided in number of sub division factor.
	 */
	private void createTriangularNetwork(final Scenario scenario) {
		Network network = scenario.getNetwork();
		int subdivisionFactor = linkProperties.getSubDivisionalFactor();
		double linkLength = linkProperties.getLinkLength();

		//nodes of the equilateral triangle base starting, left node at (0,0)
		for (int i = 0; i < subdivisionFactor + 1; i++) {
			double x=0., y = 0.;
			x = (linkLength / subdivisionFactor) * i;
			Coord coord = new Coord(x, y);
			Id<Node> id = Id.createNodeId(i);

			Node node = scenario.getNetwork().getFactory().createNode(id, coord);
			network.addNode(node);
		}
		//nodes of the triangle right side
		for (int i = 0; i < subdivisionFactor; i++) {
			double x = linkLength - ((linkLength / subdivisionFactor)) * Math.cos(Math.PI / 3) * (i + 1);
			double y = (linkLength / subdivisionFactor) * Math.sin(Math.PI / 3) * (i + 1);
			Coord coord = new Coord(x, y);
			Id<Node> id = Id.createNodeId(subdivisionFactor + i + 1);

			Node node = scenario.getNetwork().getFactory().createNode(id, coord);
			network.addNode(node);
		}
		//nodes of the triangle left side
		for (int i = 0; i < subdivisionFactor - 1; i++) {
			double x = linkLength / 2 - (linkLength / subdivisionFactor) * Math.cos(Math.PI / 3) * (i + 1);
			double y = Math.tan(Math.PI / 3) * x;
			Coord coord = new Coord(x, y);
			Id<Node> id = Id.createNodeId(2 * subdivisionFactor + i + 1);

			Node node = scenario.getNetwork().getFactory().createNode(id, coord);
			network.addNode(node);
		}
		//additional startNode and endNode for home and work activities
		double x = -50.0;
		Coord coord = new Coord(x, 0.0);
		Node startNode = scenario.getNetwork().getFactory().createNode(Id.createNodeId("home"), coord);
		network.addNode(startNode);

		coord = new Coord(linkLength + 50.0, 0.0);
		Id<Node> endNodeId = Id.createNodeId("work");
		Node endNode = scenario.getNetwork().getFactory().createNode(endNodeId, coord);
		network.addNode(endNode);

		// triangle links
		for (int i = 0; i < noOfSides * subdivisionFactor; i++) {
			Id<Node> idFrom = Id.createNodeId(i);
			Id<Node> idTo;
			if (i != noOfSides * subdivisionFactor - 1)
				idTo = Id.createNodeId(i + 1);
			else
				idTo = Id.createNodeId(0);
			Node from = network.getNodes().get(idFrom);
			Node to = network.getNodes().get(idTo);

			Link link = scenario.getNetwork().getFactory().createLink(Id.createLinkId(i+1), from, to);
			link.setCapacity(linkProperties.getLinkCapacity());
			link.setFreespeed(linkProperties.getLinkFreeSpeedMPS());
			link.setLength(linkProperties.getLinkLength());
			link.setNumberOfLanes(linkProperties.getNumberOfLanes());
			link.setAllowedModes(linkProperties.getAllowedModes());
			network.addLink(link);

			if (i==0) {
				firstLinkOfTrack = link.getId();
			}
			if (i == subdivisionFactor-1) {
				lastLinkOfBase = link.getId();
			}
			if (i ==  subdivisionFactor) {
				firstLinkOfMiddleSide = link.getId();
			}
			if (i== noOfSides * subdivisionFactor-1) {
				lastLinkOfTrack = link.getId();
			}
		}

		//additional startLink and endLink for home and work activities
		Link startLink = scenario.getNetwork()
								 .getFactory()
								 .createLink(startLinkId,
										 startNode,
										 scenario.getNetwork().getNodes().get(Id.createNodeId(0)));
		startLink.setCapacity(10 * linkProperties.getLinkCapacity());
		startLink.setFreespeed(linkProperties.getLinkFreeSpeedMPS());
		startLink.setLength(25.);
		startLink.setNumberOfLanes(1.);
		startLink.setAllowedModes(linkProperties.getAllowedModes());
		network.addLink(startLink);

		Link endLink = scenario.getNetwork()
							   .getFactory()
							   .createLink(endLinkId, scenario.getNetwork().getNodes().get(Id.createNodeId(
									   subdivisionFactor)), endNode);
		endLink.setCapacity(10 * linkProperties.getLinkCapacity());
		endLink.setFreespeed(linkProperties.getLinkFreeSpeedMPS());
		endLink.setLength(25.);
		endLink.setNumberOfLanes(1.);
		endLink.setAllowedModes(linkProperties.getAllowedModes());
		network.addLink(endLink);
	}

	public RaceTrackLinkProperties getLinkProperties(){
		return this.linkProperties;
	}

	public double getLengthOfTrack(){
		return linkProperties.getLinkLength()* noOfSides;
	}

	Id<Link> getFirstLinkIdOfTrack() {
		return firstLinkOfTrack;
	}

	Id<Link> getLastLinkIdOfBase () {
		return lastLinkOfBase;
	}

	Id<Link> getFirstLinkIdOfMiddleLinkOfTrack () {
		return firstLinkOfMiddleSide;
	}

	Id<Link> getLastLinkIdOfTrack() {
		return lastLinkOfTrack;
	}

	Id<Link> getTripDepartureLinkId(){
		return this.startLinkId;
	}

	Id<Link> getTripArrivalLinkId(){
		return this.endLinkId;
	}
}