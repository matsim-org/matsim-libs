/* *********************************************************************** *
 * project: org.matsim.*
 * CreatePseudoNetwork
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.polettif.multiModalMap.mapping;

import java.util.*;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import playground.polettif.multiModalMap.config.PublicTransportMapConfigGroup;

import static playground.polettif.multiModalMap.tools.NetworkTools.*;

/**
 * Creates a simple network for a transitschedule. The schedule is not
 * modified (i.e. stops are not referenced and no routing is applied).
 * <p>
 * Based on {@link org.matsim.pt.utils.CreatePseudoNetwork}
 */
public class IndependentNetworkCreator {

	protected static Logger log = Logger.getLogger(IndependentNetworkCreator.class);


	private static final double INDEPENDENT_FREESPEED = 0.1;
	private final TransitSchedule schedule;
	private final Network network;
	private final String prefix;

	private final Map<Tuple<Node, Node>, Link> links = new HashMap<Tuple<Node, Node>, Link>();
	private final Map<Tuple<Node, Node>, TransitStopFacility> stopFacilities = new HashMap<Tuple<Node, Node>, TransitStopFacility>();
	private final Map<TransitStopFacility, Node> nodes = new HashMap<TransitStopFacility, Node>();
	private final Map<TransitStopFacility, List<TransitStopFacility>> facilityCopies = new HashMap<TransitStopFacility, List<TransitStopFacility>>();
	private final NetworkFactory networkFactory;

	private Map<Coord, Node> borderNodes = new HashMap<>();


	private long artificialId = 0;
	private String borderNodePrefix = "border_";

	public IndependentNetworkCreator(final TransitSchedule schedule, final Network network, final PublicTransportMapConfigGroup config) {
		this.schedule = schedule;
		this.network = network;
		this.networkFactory = network.getFactory();
		this.prefix = config.getPrefixArtificialLinks();
	}

	public void createNetwork() {

		List<Tuple<TransitLine, TransitRoute>> toBeRemoved = new LinkedList<Tuple<TransitLine, TransitRoute>>();

		Set<TransitStopFacility> stopFacilitiesOutsideNetwork = new HashSet<>();

		for(TransitLine transitLine : this.schedule.getTransitLines().values()) {
			for(TransitRoute transitRoute : transitLine.getRoutes().values()) {
				TransitRouteStop prevStop = null;
				for(TransitRouteStop stop : transitRoute.getStops()) {
					createNetworkLink(prevStop, stop, Collections.singleton(transitRoute.getTransportMode()));
					prevStop = stop;
				}
			}
		}

		for(Tuple<TransitLine, TransitRoute> remove : toBeRemoved) {
			remove.getFirst().removeRoute(remove.getSecond());
		}
	}

	private Link createNetworkLink(final TransitRouteStop fromStop, final TransitRouteStop toStop, Set<String> transportMode) {
		TransitStopFacility fromFacility = (fromStop == null) ? toStop.getStopFacility() : fromStop.getStopFacility();
		TransitStopFacility toFacility = toStop.getStopFacility();

		Node fromNode = this.nodes.get(fromFacility);
		if(fromNode == null) {
			fromNode = this.network.getFactory().createNode(Id.create(this.prefix + toFacility.getId(), Node.class), fromFacility.getCoord());
			this.network.addNode(fromNode);
			this.nodes.put(toFacility, fromNode);
		}

		Node toNode = this.nodes.get(toFacility);
		if(toNode == null) {
			toNode = this.network.getFactory().createNode(Id.create(this.prefix + toFacility.getId(), Node.class), toFacility.getCoord());
			this.network.addNode(toNode);
			this.nodes.put(toFacility, toNode);
		}

		Tuple<Node, Node> connection = new Tuple<>(fromNode, toNode);
		Link link = this.links.get(connection);
		if(link == null) {
			link = createAndAddLink(fromNode, toNode, connection, transportMode);

			if(toFacility.getLinkId() == null) {
				toFacility.setLinkId(link.getId());
				this.stopFacilities.put(connection, toFacility);
			} else {
				List<TransitStopFacility> copies = this.facilityCopies.get(toFacility);
				if(copies == null) {
					copies = new ArrayList<>();
					this.facilityCopies.put(toFacility, copies);
				}
				Id<TransitStopFacility> newId = Id.create(toFacility.getId().toString() + "." + Integer.toString(copies.size() + 1), TransitStopFacility.class);
				TransitStopFacility newFacility = this.schedule.getFactory().createTransitStopFacility(newId, toFacility.getCoord(), toFacility.getIsBlockingLane());
				newFacility.setStopPostAreaId(toFacility.getId().toString());
				newFacility.setLinkId(link.getId());
				newFacility.setName(toFacility.getName());
				copies.add(newFacility);
				this.nodes.put(newFacility, toNode);
				this.schedule.addStopFacility(newFacility);
				toStop.setStopFacility(newFacility);
				this.stopFacilities.put(connection, newFacility);
			}
		} else {
			toStop.setStopFacility(this.stopFacilities.get(connection));
		}
		return link;
	}

	private Link createAndAddLink(Node fromNode, Node toNode, Tuple<Node, Node> connection, Set<String> scheduleTransportMode) {
		Link link;
		link = this.network.getFactory().createLink(Id.create(this.prefix + this.artificialId++, Link.class), fromNode, toNode);
		if(fromNode == toNode) {
			link.setLength(50);
		} else {
			link.setLength(1000 * CoordUtils.calcEuclideanDistance(fromNode.getCoord(), toNode.getCoord()));
		}
		link.setFreespeed(INDEPENDENT_FREESPEED);
		link.setCapacity(500);
		link.setNumberOfLanes(1);
		this.network.addLink(link);
		link.setAllowedModes(scheduleTransportMode);
		this.links.put(connection, link);
		return link;
	}

	public Link getLinkBetweenStops(final TransitStopFacility fromStop, final TransitStopFacility toStop) {
		Node fromNode = this.nodes.get(fromStop);
		Node toNode = this.nodes.get(toStop);
		Tuple<Node, Node> connection = new Tuple<Node, Node>(fromNode, toNode);
		return this.links.get(connection);
	}

	public Map<Coord, Node> removeLinksWithinAOI(Coord SWcut, Coord NEcut) {
		// todo only remove links with set modes

		Set<Node> removeNodes = new HashSet<>();
		Set<Link> removeLinks = new HashSet<>();

		for(Node node : network.getNodes().values()) {
			if(getCompassQuarter(SWcut, node.getCoord()) == 1 && getCompassQuarter(NEcut, node.getCoord()) == 3)
				removeNodes.add(node);
		}

		Map<Link, Integer> crossingBorderIn = new HashMap<>();

		for(Node node : removeNodes) {
			// Links into AOI
			for(Link inLink : node.getInLinks().values()) {
				crossingBorderIn.put(inLink, getBorderCrossType(SWcut, NEcut, inLink.getFromNode().getCoord(), inLink.getToNode().getCoord()));
			}
			for(Link outLink : node.getOutLinks().values()) {
				crossingBorderIn.put(outLink, getBorderCrossType(SWcut, NEcut, outLink.getFromNode().getCoord(), outLink.getToNode().getCoord()));
			}
		}

		for(Map.Entry<Link, Integer> c :crossingBorderIn.entrySet()) {
			int crossingBorderType = c.getValue();
			Link link = c.getKey();
			Coord fromCoord = link.getFromNode().getCoord();
			Coord toCoord= link.getToNode().getCoord();

			if(link.getId().toString().equals("pt_2205"))
				log.debug("break");

			switch (crossingBorderType) {
				// north out->in
				case 10: {
					double deltaY = NEcut.getY() - fromCoord.getY();
					double deltaX = Math.tan(getAzimuth(fromCoord, toCoord)) * deltaY;

					Coord borderNodeCoord = new Coord(fromCoord.getX() + deltaX, NEcut.getY());
					Node borderNode = getBorderNode(borderNodeCoord);
					shortenLink(link, borderNode);
					break;
				}
				// north in->out
				case 17: {
					double deltaY = toCoord.getY() - NEcut.getY();
					double deltaX = Math.tan(getAzimuth(fromCoord, toCoord)) * deltaY;

					Coord borderNodeCoord = new Coord(fromCoord.getX() + deltaX, NEcut.getY());
					Node borderNode = getBorderNode(borderNodeCoord);
					shortenLink(borderNode, link);
					break;
				}
				// east out->in
				case 20: {
					double deltaX = fromCoord.getX() - NEcut.getX();
					double deltaY = Math.tan(Math.abs(getAzimuth(fromCoord, toCoord)) - Math.PI / 2) * deltaX;

					Coord borderNodeCoord = new Coord(NEcut.getX(), fromCoord.getY() + deltaY);
					Node borderNode = getBorderNode(borderNodeCoord);
					shortenLink(link, borderNode);
					break;
				}
				// east in->out
				case 27: {
					double deltaX = NEcut.getX() - fromCoord.getX();
					double deltaY = Math.tan(Math.abs(getAzimuth(fromCoord, toCoord) - Math.PI / 2)) * deltaX;

					Coord borderNodeCoord = new Coord(NEcut.getX(), fromCoord.getY() - deltaY);
					Node borderNode = getBorderNode(borderNodeCoord);
					shortenLink(borderNode, link);
					break;
				}
				// south in->out
				case 30: {
					double deltaY = SWcut.getY() - fromCoord.getY();
					double deltaX = Math.tan(getAzimuth(fromCoord, toCoord)) * deltaY;

					Coord borderNodeCoord = new Coord(fromCoord.getX() + deltaX, SWcut.getY());
					Node borderNode = getBorderNode(borderNodeCoord);
					shortenLink(link, borderNode);
					break;
				}
				// south out->in
				case 37: {
					double deltaY = fromCoord.getY() - SWcut.getY();
					double deltaX = Math.tan(getAzimuth(fromCoord, toCoord)) * deltaY;

					Coord borderNodeCoord = new Coord(fromCoord.getX() + deltaX, SWcut.getY());
					Node borderNode = getBorderNode(borderNodeCoord);
					shortenLink(borderNode, link);
					break;
				}
				// west in->out
				case 40: {
					double deltaX = SWcut.getX() - fromCoord.getX();
					double deltaY = Math.tan(Math.abs(getAzimuth(fromCoord, toCoord) - Math.PI / 2)) * deltaX;

					Coord borderNodeCoord = new Coord(SWcut.getX(), fromCoord.getY() + deltaY);
					Node borderNode = getBorderNode(borderNodeCoord);
					shortenLink(link, borderNode);
					break;
				}
				// west-out->in
				case 47: {
					double deltaX = fromCoord.getX() - SWcut.getX();
					double deltaY = Math.tan(Math.abs(getAzimuth(fromCoord, toCoord)-Math.PI/2)) * deltaX;

					Coord borderNodeCoord = new Coord(SWcut.getX(), fromCoord.getY() - deltaY);
					Node borderNode = getBorderNode(borderNodeCoord);
					shortenLink(borderNode, link);
					break;
				}
				case 0: {
					removeLinks.add(link);
					break;
				}
			}
		}

		for(Link link : removeLinks) {
			network.removeLink(link.getId());
		}

		/*
		for(Node node : removeNodes) {
			network.removeNode(node.getId());
		}
		*/

		return borderNodes;
	}

	private Node getBorderNode(Coord borderNodeCoord) {
		Node borderNode;
		if(borderNodes.containsKey(borderNodeCoord)) {
			borderNode = borderNodes.get(borderNodeCoord);
		} else {
			borderNode = networkFactory.createNode(
					Id.createNodeId(this.prefix + this.borderNodePrefix + artificialId++),
					borderNodeCoord);
			network.addNode(borderNode);
			borderNodes.put(borderNode.getCoord(), borderNode);
		}
		return borderNode;
	}

	private void shortenLink(Link link, Node toNode) {
		link.setToNode(toNode);
		link.setLength(CoordUtils.calcEuclideanDistance(link.getFromNode().getCoord(), toNode.getCoord()));
	}

	private void shortenLink(Node fromNode, Link link) {
		link.setFromNode(fromNode);
		link.setLength(CoordUtils.calcEuclideanDistance(link.getFromNode().getCoord(), fromNode.getCoord()));
	}

	/**
	 * @return which border of a rectangular area of interest a line fromCoord-toCoord crosses. One coord has to be
	 * inside area of interest<br/>
	 * [10] north->inside<br/>
	 * [17] inside->north<br/>
	 * [20] east->inside<br/>
	 * [27] inside->east<br/>
	 * [30] south->inside<br/>
	 * [37] inside->south<br/>
	 * [40] west->inside<br/>
	 * [47] inside->west<br/>
	 * [0] line does not cross any border
	 */
	public int getBorderCrossType(Coord SWcut, Coord NEcut, Coord fromCoord, Coord toCoord) {
		int fromSector = getAreaOfInterestSector(SWcut, NEcut, fromCoord);
		int toSector = getAreaOfInterestSector(SWcut, NEcut, toCoord);

		if(fromSector == toSector) {
			return 0;
		}

		double azFromTo = getAzimuth(fromCoord, toCoord);
		double azFromSW = getAzimuth(fromCoord, SWcut);
		double azFromNE = getAzimuth(fromCoord, NEcut);

		double azToFrom = getAzimuth(toCoord, fromCoord);
		double azToSW = getAzimuth(toCoord, SWcut);
		double azToNE = getAzimuth(toCoord, NEcut);

		if(fromSector != 0 ) {
			switch (fromSector) {
				case 1:
					return 10;
				case 2: {
					if(azFromTo > azFromNE)
						return 10;
					else
						return 20;
				}
				case 3:
					return 20;
				case 4: {
					if(azFromTo > azFromNE)
						return 20;
					else
						return 30;
				}
				case 5:
					return 30;
				case 6: {
					if(azFromTo > azFromSW)
						return 30;
					else
						return 40;
				}
				case 7:
					return 40;
				case 8: {
					if(azFromTo > azFromSW)
						return 40;
					else
						return 10;
				}
			}
		}

		if(toSector != 0) {
			switch (toSector) {
				case 1:
					return 17;
				case 2: {
					if(azToFrom < azToNE)
						return 17;
					else
						return 27;
				}
				case 3:
					return 27;
				case 4: {
					if(azToFrom < azToNE)
						return 27;
					else
						return 37;
				}
				case 5:
					return 37;
				case 6: {
					if(azToFrom < azToSW)
						return 37;
					else
						return 47;
				}
				case 7:
					return 47;
				case 8: {
					if(azToFrom< azToSW)
						return 47;
					else
						return 17;
				}
			}
		}

		return 0;
	}

	private int getAreaOfInterestSector(Coord SWcut, Coord NEcut, Coord c) {
		int qSW = getCompassQuarter(SWcut, c);
		int qNE = getCompassQuarter(NEcut, c);

		if(qSW == 1 && qNE == 3) {
			return 0;
		} else if(qSW == 1 && qNE == 4) {
			return 1;
		} else if(qSW == 1 && qNE == 1) {
			return 2;
		} else if(qSW == 1 && qNE == 2) {
			return 3;
		} else if(qSW == 2 && qNE == 2) {
			return 4;
		} else if(qSW == 2 && qNE == 3) {
			return 5;
		} else if(qSW == 3 && qNE == 3) {
			return 6;
		} else if(qSW == 4 && qNE == 3) {
			return 7;
		} else if(qSW == 4 && qNE == 4) {
			return 8;
		}

		return 0;
	}

}
