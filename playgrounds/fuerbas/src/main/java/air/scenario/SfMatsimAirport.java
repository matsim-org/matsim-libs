/* *********************************************************************** *
 * project: org.matsim.*
 * SfAirScheduleBuilder
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package air.scenario;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;

/**
 * @author fuerbas
 * @author dgrether
 * 
 */
public class SfMatsimAirport {

	public static final double runwayLength = 1500.0;
	public static final double taxiwayLength = 500.0;
	public static final double taxiwayFreespeed = 20.0 / 3.6;
	public static final double runwayFreespeed = 220.0 / 2.6;
	public static final double TAXI_TOL_TIME = 3 * (taxiwayLength / taxiwayFreespeed) + // time for taxi out and taxi in
			2 * (runwayLength / runwayFreespeed); // time for take-off and landing (TOL)

	public Coord coord;
	public Id id, incomingFlightsNodeId, outgoingFlightsNodeId;
	private Id transitStopFacilityId;

	private List<Id> departureLinkIdList = new ArrayList<Id>();
	private List<Id> arrivalLinkIdList = new ArrayList<Id>();

	private double taxiOutboundTime = (taxiwayLength / taxiwayFreespeed)
			+ (runwayLength / runwayFreespeed); // time for take-off

	private double taxiInboundTime = 2 * (taxiwayLength / taxiwayFreespeed)
			+ (runwayLength / runwayFreespeed);

	private CoordImpl coordApronEnd;
	private CoordImpl coordTaxiInStart;
	private CoordImpl coordTaxiOutEnd;
	private CoordImpl coordRunwayInStart;
	private CoordImpl coordRunwayOutEnd;
	private Set<String> allowedModes;
	private Node nodeApronStartAirport;
	private Node nodeApronEnd;
	private Node nodeRunwayInStart;
	private Node nodeTaxiOutStart;
	private Node nodeTaxiInStart;
	private Link linkTaxiIn;
	private Link linkTaxiOut;
	private Link linkRunwayOutgoing;
	private Link linkRunwayIncoming;
	private Node nodeRunwayOutEnd;

	public SfMatsimAirport(Id id, Coord coord) {
		this.id = id;
		this.coord = coord;
		allowedModes = new HashSet<String>();
		allowedModes.add("pt");
		allowedModes.add("car");
	}

	public Id getIncomingFlightsNodeId() {
		return this.incomingFlightsNodeId;
	}

	public Id getOutgoingFlightsNodeId() {
		return this.outgoingFlightsNodeId;
	}

	private void createCoordsTwoRunways() {
		coordApronEnd = new CoordImpl(this.coord.getX(), this.coord.getY() + taxiwayLength); // shifting end of apron
		coordTaxiInStart = new CoordImpl(this.coord.getX() - taxiwayLength, this.coord.getY()
				- taxiwayLength); // shifting taxiway
		coordTaxiOutEnd = new CoordImpl(coordApronEnd.getX() - taxiwayLength, coordApronEnd.getY()
				+ taxiwayLength); // shifting taxiway
		coordRunwayInStart = new CoordImpl(coordTaxiInStart.getX() - runwayLength,
				coordTaxiInStart.getY()); // shifting runway
		coordRunwayOutEnd = new CoordImpl(coordTaxiOutEnd.getX() - runwayLength, coordTaxiOutEnd.getY()); // shifting runway
	}

	private void createCoordsOneRunway() {
		coordApronEnd = new CoordImpl(this.coord.getX() + runwayLength, this.coord.getY());
		coordTaxiOutEnd = new CoordImpl(coordApronEnd.getX() + runwayLength, this.coord.getY()
				+ taxiwayLength);
		coordRunwayInStart = coordTaxiOutEnd;
		coordTaxiInStart = new CoordImpl(this.coord.getX(), this.coord.getY() + taxiwayLength);
		coordRunwayOutEnd = coordTaxiInStart;

	}

	private void createApron(Network network) {
		Id idApron = this.id; // Id for apron link and central airport node
		Id idApronEnd = new IdImpl(this.id + "apron"); // Id for end of apron node
		nodeApronStartAirport = network.getFactory().createNode(idApron, this.coord); // central node of any airport
		nodeApronEnd = network.getFactory().createNode(idApronEnd, coordApronEnd); // end of apron node, apron is used for
																																								// parking and as transit stop
		network.addNode(nodeApronStartAirport);
		network.addNode(nodeApronEnd);
		Link linkApron = network.getFactory().createLink(idApron, nodeApronStartAirport, nodeApronEnd);
		this.transitStopFacilityId = linkApron.getId();
		linkApron.setAllowedModes(allowedModes);
		linkApron.setCapacity(10. * network.getCapacityPeriod());
		linkApron.setLength(taxiwayLength);
		linkApron.setFreespeed(taxiwayFreespeed);
		network.addLink(linkApron);
	}

	private void createStar(Network network) {
		Id idStar = new IdImpl(this.id.toString() + "star"); // Id for STAR route
		this.arrivalLinkIdList.add(0, idStar);
		Link linkStarIn = null;
		DgStarinfo starInfo = DgCreateSfFlightScenario.stars.get(this.id.toString());
		if (starInfo == null) {
			starInfo = DgCreateSfFlightScenario.DEFAULTSTAR;
		}
		Coord coordStar = new CoordImpl(coordRunwayInStart.getX() - starInfo.getLength(),
				coordRunwayInStart.getY());
		Node nodeStar = network.getFactory().createNode(idStar, coordStar); // start of STAR
		network.addNode(nodeStar);
		linkStarIn = network.getFactory().createLink(idStar, nodeStar, nodeRunwayInStart);
		linkStarIn.setAllowedModes(allowedModes);
		linkStarIn.setLength(starInfo.getLength());
		linkStarIn.setCapacity(network.getCapacityPeriod() * starInfo.getCapacity());
		System.out.println("HOLE STAR MUC.................................................");
		linkStarIn.setFreespeed(starInfo.getFreespeed());
		network.addLink(linkStarIn);
		this.incomingFlightsNodeId = linkStarIn.getFromNode().getId();
	}

	private void createTaxiWays(Network network) {
		Id idTaxiIn = new IdImpl(this.id + "taxiInbound"); // Id for taxiway link and end of taxiway node
		Id idTaxiOut = new IdImpl(this.id + "taxiOutbound"); // Id for taxiway link and end of taxiway node
		nodeTaxiInStart = network.getFactory().createNode(idTaxiIn, coordTaxiInStart); // taxiway inbound start = runway
																																										// inbound end
		nodeTaxiOutStart = network.getFactory().createNode(idTaxiOut, coordTaxiOutEnd); // taxiway outbound end = runway
																																										// outbound start
		network.addNode(nodeTaxiInStart);
		network.addNode(nodeTaxiOutStart);
		linkTaxiIn = network.getFactory().createLink(idTaxiIn, nodeTaxiInStart, nodeApronStartAirport);
		linkTaxiOut = network.getFactory().createLink(idTaxiOut, nodeApronEnd, nodeTaxiOutStart);
		linkTaxiIn.setAllowedModes(allowedModes);
		linkTaxiOut.setAllowedModes(allowedModes);
		linkTaxiIn.setCapacity(10. * network.getCapacityPeriod());
		// linkTaxiIn.setNumberOfLanes(0.015);
		linkTaxiIn.setNumberOfLanes(4.0);
		// linkTaxiOut.setCapacity((1./60.)*network.getCapacityPeriod());
		linkTaxiOut.setCapacity(10. * network.getCapacityPeriod());
		linkTaxiIn.setLength(taxiwayLength);
		linkTaxiOut.setLength(taxiwayLength);
		linkTaxiIn.setFreespeed(taxiwayFreespeed);
		linkTaxiOut.setFreespeed(taxiwayFreespeed);
		network.addLink(linkTaxiIn);
		network.addLink(linkTaxiOut);
	}

	private void create2Runways(Network network) {
		Id idRunwayIn = new IdImpl(this.id + "runwayInbound"); // Id for runway link and end of runway node
		Id idRunwayOut = new IdImpl(this.id + "runwayOutbound"); // Id for runway link and end of runway node
		nodeRunwayInStart = network.getFactory().createNode(idRunwayIn, coordRunwayInStart); // start of inbound runway
		nodeRunwayOutEnd = network.getFactory().createNode(idRunwayOut, coordRunwayOutEnd); // end of outbound runway
		network.addNode(nodeRunwayInStart);
		network.addNode(nodeRunwayOutEnd);

		linkRunwayIncoming = network.getFactory().createLink(idRunwayIn, nodeRunwayInStart,
				nodeTaxiInStart);
		linkRunwayIncoming.setAllowedModes(allowedModes);
		linkRunwayIncoming.setFreespeed(runwayFreespeed);
		linkRunwayIncoming.setLength(runwayLength);
		linkRunwayIncoming.setCapacity(10. * network.getCapacityPeriod());

		linkRunwayOutgoing = network.getFactory().createLink(idRunwayOut, nodeTaxiOutStart,
				nodeRunwayOutEnd);
		linkRunwayOutgoing.setAllowedModes(allowedModes);
		linkRunwayOutgoing.setLength(runwayLength);
		linkRunwayOutgoing.setFreespeed(runwayFreespeed);
		linkRunwayOutgoing.setCapacity((10.) * network.getCapacityPeriod());
		linkRunwayOutgoing.setNumberOfLanes(4.0);

		network.addLink(linkRunwayIncoming);
		network.addLink(linkRunwayOutgoing);

		this.incomingFlightsNodeId = linkRunwayIncoming.getFromNode().getId();
		this.outgoingFlightsNodeId = linkRunwayOutgoing.getToNode().getId();
	}

	private void create1Runway(Network network) {
		Id idRunwayIn = new IdImpl(this.id + "runwayInOutbound"); // Id for runway link and end of runway node
		nodeRunwayInStart = nodeTaxiOutStart;
		nodeRunwayOutEnd = nodeTaxiInStart;
		Link linkRunway = network.getFactory()
				.createLink(idRunwayIn, nodeTaxiOutStart, nodeTaxiInStart);
		linkRunway.setAllowedModes(allowedModes);
		linkRunway.setCapacity((1.) * network.getCapacityPeriod());
		linkRunway.setLength(runwayLength);
		linkRunway.setFreespeed(runwayFreespeed);
		network.addLink(linkRunway);
		this.incomingFlightsNodeId = linkRunway.getFromNode().getId();
		this.outgoingFlightsNodeId = linkRunway.getToNode().getId();

	}

	private void createDepartureLinkIdList() {
		this.departureLinkIdList.add(linkTaxiIn.getId());
		this.departureLinkIdList.add(linkRunwayOutgoing.getId());
		this.arrivalLinkIdList.add(linkRunwayIncoming.getId());
		this.arrivalLinkIdList.add(linkTaxiIn.getId());
	}

	public void createTwoRunways(Network network) {
		this.createCoordsTwoRunways();
		this.createApron(network);
		this.createTaxiWays(network);
		this.create2Runways(network);
		this.createDepartureLinkIdList();
		if (DgCreateSfFlightScenario.doCreateStars) {
			this.createStar(network);
		}
	}

	public void createOneRunway(Network network) {
		this.createCoordsOneRunway();
		this.createApron(network);
		this.createTaxiWays(network);
		this.create1Runway(network);
		this.createDepartureLinkIdList();
		if (DgCreateSfFlightScenario.doCreateStars) {
			this.createStar(network);
		}
	}

	public Id getId() {
		return this.id;
	}

	public Id getStopFacilityLinkId() {
		return this.transitStopFacilityId;
	}

	public List<Id> getDepartureLinkIdList() {
		return departureLinkIdList;
	}

	public List<Id> getArrivalLinkIdList() {
		return arrivalLinkIdList;
	}

	public double getTaxiTimeOutbound() {
		return this.taxiOutboundTime;
	}

	public double getTaxiTimeInbound() {
		return this.taxiInboundTime;
	}

}