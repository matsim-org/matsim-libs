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
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * @author fuerbas
 * @author dgrether
 * 
 */
public class SfMatsimAirport {

	public static final double runwayLength = 1500.0;
	public static final double taxiwayLength = 500.0;
	public static final double taxiwayFreespeed = 20.0 / 3.6;
	public static final double runwayFreespeed = 220.0 / 3.6;
	public static final double TAXI_TOL_TIME = 3 * (taxiwayLength / taxiwayFreespeed) + // time for taxi out and taxi in
			2 * (runwayLength / runwayFreespeed); // time for take-off and landing (TOL)

	public Id<Node> id;
	public Id<Node> incomingFlightsNodeId, outgoingFlightsNodeId;
	private Id<TransitStopFacility> transitStopFacilityId;

	private List<Id<Link>> departureLinkIdList = new ArrayList<Id<Link>>();
	private List<Id<Link>> arrivalLinkIdList = new ArrayList<Id<Link>>();

	private double taxiOutboundTime = (taxiwayLength / taxiwayFreespeed)
			+ (runwayLength / runwayFreespeed); // time for take-off

	private double taxiInboundTime = 2 * (taxiwayLength / taxiwayFreespeed)
			+ (runwayLength / runwayFreespeed);

	private Set<String> allowedModes;
	public Coord coordApronEnd;
	private Coord coordApronStart;
	private Coord coordTaxiInboundStart;
	private Coord coordTaxiOutboundEnd;
	private Coord coordRunwayInboundStart;
	private Coord coordRunwayOutboundEnd;
	private Node nodeApronStart;
	private Node nodeApronEnd;
	private Node nodeRunwayInboundStart;
	private Node nodeRunwayOutboundEnd;
	private Node nodeTaxiOutboundEnd;
	private Node nodeTaxiInboundStart;
	private Link linkTaxiInbound;
	private Link linkTaxiOutbound;
	private Link linkRunwayOutbound;
	private Link linkRunwayInbound;
	private Coord coord;
	private DgAirportCapacity capacityData;

	public SfMatsimAirport(Id<Node> id, Coord coord, DgAirportCapacity capacityData) {
		this.id = id;
		this.coord = coord;
		this.capacityData = capacityData;
		allowedModes = new HashSet<String>();
		allowedModes.add("pt");
		allowedModes.add("car");
	}

	public Id<Node> getIncomingFlightsNodeId() {
		return this.incomingFlightsNodeId;
	}

	public Id<Node> getOutgoingFlightsNodeId() {
		return this.outgoingFlightsNodeId;
	}

	private void createCoordsTwoRunways() {
		coordApronEnd = this.coord;
		coordApronStart = new CoordImpl(this.coordApronEnd.getX(), this.coordApronEnd.getY() + taxiwayLength); 
		coordTaxiInboundStart = new CoordImpl(this.coordApronEnd.getX() - taxiwayLength, 
				this.coordApronEnd.getY() - taxiwayLength); 
		coordTaxiOutboundEnd = new CoordImpl(coordApronStart.getX() - taxiwayLength, 
				coordApronStart.getY()	+ taxiwayLength); 
		
		coordRunwayInboundStart = new CoordImpl(coordTaxiInboundStart.getX() - runwayLength,
				coordTaxiInboundStart.getY()); 
		coordRunwayOutboundEnd = new CoordImpl(coordTaxiOutboundEnd.getX() - runwayLength, coordTaxiOutboundEnd.getY()); 
	}

	private void createCoordsOneRunway() {
		coordApronEnd = this.coord;
		coordApronStart = new CoordImpl(this.coordApronEnd.getX() + taxiwayLength, this.coordApronEnd.getY());
		coordTaxiOutboundEnd = new CoordImpl(coordApronEnd.getX() + runwayLength/2, this.coordApronEnd.getY() - taxiwayLength);
		coordRunwayInboundStart = coordTaxiOutboundEnd;
		coordTaxiInboundStart = new CoordImpl(this.coordApronEnd.getX() - runwayLength/2, this.coordApronEnd.getY() - taxiwayLength);
		coordRunwayOutboundEnd = coordTaxiInboundStart;

	}

	private void createApron(Network network) {
		Id<Node> idApron = this.id; 
		Id<Node> idApronEnd = Id.create(this.id + "apron", Node.class); 
		nodeApronStart = network.getFactory().createNode(idApron, this.coordApronEnd); 
		nodeApronEnd = network.getFactory().createNode(idApronEnd, coordApronStart); 
		network.addNode(nodeApronStart);
		network.addNode(nodeApronEnd);
		Link linkApron = network.getFactory().createLink(Id.create(idApron, Link.class), nodeApronStart, nodeApronEnd);
		this.transitStopFacilityId = Id.create(linkApron.getId(), TransitStopFacility.class);
		linkApron.setAllowedModes(allowedModes);
		linkApron.setCapacity(this.capacityData.getApronFlowCapacityCarEquivPerHour());
		linkApron.setLength(taxiwayLength);
		linkApron.setFreespeed(taxiwayFreespeed);
		network.addLink(linkApron);
	}

	private void createStar(Network network) {
		Id<Link> idStar = Id.create(this.id.toString() + "star", Link.class); // Id for STAR route
		this.arrivalLinkIdList.add(0, idStar);
		Link linkStarIn = null;
		DgStarinfo starInfo = DgCreateSfFlightScenario.stars.get(this.id.toString());
		if (starInfo == null) {
			starInfo = DgCreateSfFlightScenario.DEFAULTSTAR;
		}
		Coord coordStar = new CoordImpl(coordRunwayInboundStart.getX() - starInfo.getLength(),
				coordRunwayInboundStart.getY());
		Node nodeStar = network.getFactory().createNode(Id.create(idStar, Node.class), coordStar); // start of STAR
		network.addNode(nodeStar);
		linkStarIn = network.getFactory().createLink(idStar, nodeStar, nodeRunwayInboundStart);
		linkStarIn.setAllowedModes(allowedModes);
		linkStarIn.setLength(starInfo.getLength());
		linkStarIn.setCapacity(network.getCapacityPeriod() * starInfo.getCapacity());
		linkStarIn.setFreespeed(starInfo.getFreespeed());
		network.addLink(linkStarIn);
		this.incomingFlightsNodeId = linkStarIn.getFromNode().getId();
	}

	private void createTaxiWays(Network network) {
		Id<Node> idNodeTaxiInStart = Id.create(this.id + "taxiInbound", Node.class); 
		Id<Node> idNodeTaxiOutStart = Id.create(this.id + "taxiOutbound", Node.class);
		nodeTaxiInboundStart = network.getFactory().createNode(idNodeTaxiInStart, coordTaxiInboundStart); 
		nodeTaxiOutboundEnd = network.getFactory().createNode(idNodeTaxiOutStart, coordTaxiOutboundEnd); 
		network.addNode(nodeTaxiInboundStart);
		network.addNode(nodeTaxiOutboundEnd);
		linkTaxiInbound = network.getFactory().createLink(Id.create(idNodeTaxiInStart, Link.class), nodeTaxiInboundStart, nodeApronStart);
		linkTaxiOutbound = network.getFactory().createLink(Id.create(idNodeTaxiOutStart, Link.class), nodeApronEnd, nodeTaxiOutboundEnd);
		linkTaxiInbound.setAllowedModes(allowedModes);
		linkTaxiOutbound.setAllowedModes(allowedModes);
		linkTaxiInbound.setCapacity(this.capacityData.getInboundTaxiwayFlowCapacityCarEquivPerHour());
		// linkTaxiIn.setNumberOfLanes(0.015);
		linkTaxiInbound.setNumberOfLanes(1.0);
		// linkTaxiOut.setCapacity((1./60.)*network.getCapacityPeriod());
		linkTaxiOutbound.setCapacity(this.capacityData.getOutboundTaxiwayFlowCapacityCarEquivPerHour());
		linkTaxiInbound.setLength(taxiwayLength);
		linkTaxiOutbound.setLength(taxiwayLength);
		linkTaxiInbound.setFreespeed(taxiwayFreespeed);
		linkTaxiOutbound.setFreespeed(taxiwayFreespeed);
		network.addLink(linkTaxiInbound);
		network.addLink(linkTaxiOutbound);
	}

	private void create2Runways(Network network) {
		Id<Node> idRunwayIn = Id.create(this.id + "runwayInbound", Node.class); // Id for runway link and end of runway node
		Id<Node> idRunwayOut = Id.create(this.id + "runwayOutbound", Node.class); // Id for runway link and end of runway node
		nodeRunwayInboundStart = network.getFactory().createNode(idRunwayIn, coordRunwayInboundStart); // start of inbound runway
		nodeRunwayOutboundEnd = network.getFactory().createNode(idRunwayOut, coordRunwayOutboundEnd); // end of outbound runway
		network.addNode(nodeRunwayInboundStart);
		network.addNode(nodeRunwayOutboundEnd);

		linkRunwayInbound = network.getFactory().createLink(Id.create(idRunwayIn, Link.class), nodeRunwayInboundStart,
				nodeTaxiInboundStart);
		linkRunwayInbound.setAllowedModes(allowedModes);
		linkRunwayInbound.setFreespeed(this.capacityData.getInboundRunwayFreespeedForStorageRestriction(runwayLength));
		linkRunwayInbound.setLength(runwayLength);
		linkRunwayInbound.setCapacity(this.capacityData.getRunwayInboundFlowCapacityCarEquivPerHour());
		// c_s = link_length * nr_lanes / 7.5 -> nr_lanes = c_s /link_length * 7.5, using c_s = 1
		linkRunwayInbound.setNumberOfLanes(1.0 / runwayLength * 7.5);

		linkRunwayOutbound = network.getFactory().createLink(Id.create(idRunwayOut, Link.class), nodeTaxiOutboundEnd,
				nodeRunwayOutboundEnd);
		linkRunwayOutbound.setAllowedModes(allowedModes);
		linkRunwayOutbound.setLength(runwayLength);
		linkRunwayOutbound.setFreespeed(this.capacityData.getOutboundRunwayFreespeedForStorageRestriction(runwayLength));
		linkRunwayOutbound.setCapacity(this.capacityData.getRunwayOutboundFlowCapacity_CarEquivPerHour());
		// c_s = link_length * nr_lanes / 7.5 -> nr_lanes = c_s /link_length * 7.5, using c_s = 1
		linkRunwayOutbound.setNumberOfLanes(1.0 / runwayLength * 7.5);
		
		
		network.addLink(linkRunwayInbound);
		network.addLink(linkRunwayOutbound);

		this.incomingFlightsNodeId = linkRunwayInbound.getFromNode().getId();
		this.outgoingFlightsNodeId = linkRunwayOutbound.getToNode().getId();
	}

	private void create1Runway(Network network) {
		Id<Link> idRunwayIn = Id.create(this.id + "runwayInOutbound", Link.class); // Id for runway link and end of runway node
		nodeRunwayInboundStart = nodeTaxiOutboundEnd;
		nodeRunwayOutboundEnd = nodeTaxiInboundStart;
		linkRunwayInbound = network.getFactory()
				.createLink(idRunwayIn, nodeTaxiOutboundEnd, nodeTaxiInboundStart);
		linkRunwayInbound.setAllowedModes(allowedModes);
		linkRunwayInbound.setCapacity(this.capacityData.getRunwayInboundFlowCapacityCarEquivPerHour() 
				+	this.capacityData.getRunwayOutboundFlowCapacity_CarEquivPerHour());
		linkRunwayInbound.setLength(runwayLength);
		linkRunwayInbound.setFreespeed(runwayFreespeed);
		// c_s = link_length * nr_lanes / 7.5 -> nr_lanes = c_s /link_length * 7.5, using c_s = 1
		linkRunwayInbound.setNumberOfLanes(1.0 / runwayLength * 7.5);
		network.addLink(linkRunwayInbound);
		this.incomingFlightsNodeId = linkRunwayInbound.getFromNode().getId();
		this.outgoingFlightsNodeId = linkRunwayInbound.getToNode().getId();
		linkRunwayOutbound = linkRunwayInbound;
	}

	private void createDepartureLinkIdList() {
		this.departureLinkIdList.add(linkTaxiOutbound.getId());
		this.departureLinkIdList.add(linkRunwayOutbound.getId());
		this.arrivalLinkIdList.add(linkRunwayInbound.getId());
		this.arrivalLinkIdList.add(linkTaxiInbound.getId());
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

	public Id<Node> getId() {
		return this.id;
	}

	public Id<Link> getStopFacilityLinkId() {
		return Id.create(this.transitStopFacilityId, Link.class);
	}

	public List<Id<Link>> getDepartureLinkIdList() {
		return departureLinkIdList;
	}

	public List<Id<Link>> getArrivalLinkIdList() {
		return arrivalLinkIdList;
	}

	public double getTaxiTimeOutbound() {
		return this.taxiOutboundTime;
	}

	public double getTaxiTimeInbound() {
		return this.taxiInboundTime;
	}

}