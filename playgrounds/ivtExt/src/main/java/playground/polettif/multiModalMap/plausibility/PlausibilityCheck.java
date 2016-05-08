/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.polettif.multiModalMap.plausibility;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import playground.polettif.multiModalMap.plausibility.log.DirectionChangeMessage;
import playground.polettif.multiModalMap.plausibility.log.LogMessage;
import playground.polettif.multiModalMap.plausibility.log.LoopMessage;
import playground.polettif.multiModalMap.plausibility.log.TravelTimeMessage;
import playground.polettif.multiModalMap.tools.NetworkTools;
import playground.polettif.multiModalMap.tools.ScheduleTools;

import java.util.*;

import static playground.polettif.multiModalMap.tools.CoordTools.getAzimuth;
import static playground.polettif.multiModalMap.tools.ScheduleTools.getLinkIds;

/**
 * Performs a plausibility check on the given schedule
 * and network.
 *
 * @author polettif
 */
public class PlausibilityCheck {

	private List<LogMessage> log = new ArrayList<>();

	private double uTurnAngleThreshold = Math.PI/8;

	private TransitSchedule schedule;
	private Network network;

	public PlausibilityCheck(TransitSchedule schedule, Network network) {
		this.schedule = schedule;
		this.network = network;
	}

	public static void main(final String[] args) {
		TransitSchedule schedule = ScheduleTools.loadTransitSchedule(args[0]);
		Network network = NetworkTools.loadNetwork(args[1]);

		PlausibilityCheck check = new PlausibilityCheck(schedule, network);
		check.run();

		check.printLineSummary();
	}

	public void run() {
		for(TransitLine transitLine : this.schedule.getTransitLines().values()) {
			for(TransitRoute transitRoute : transitLine.getRoutes().values()) {
				Iterator<TransitRouteStop> stopsIterator = transitRoute.getStops().iterator();

				List<Link> links = NetworkTools.getLinksFromIds(network, getLinkIds(transitRoute));
				Set<Node> nodesInRoute = new HashSet<>();

				TransitRouteStop previousStop = stopsIterator.next();
				TransitRouteStop nextStop = stopsIterator.next();
				double ttActual = 0;
				double departTime = nextStop.getDepartureOffset();

				for(int i=0; i<links.size()-2; i++) {
					Link linkFrom = links.get(i);
					Link linkTo = links.get(i+1);

					// travel time check
					ttActual += linkFrom.getLength() / linkFrom.getFreespeed();

					if(nextStop.getStopFacility().getLinkId().equals(linkTo.getId())) {
						double ttSchedule = nextStop.getArrivalOffset() - departTime;
						if(ttActual > ttSchedule) {
							log.add(new TravelTimeMessage(transitLine, transitRoute, previousStop, nextStop, ttActual, ttSchedule));
						}
						// reset
						ttActual = 0;
						previousStop = nextStop;
						departTime = previousStop.getDepartureOffset();
						nextStop = stopsIterator.next();
					}

					// loopcheck
					if(!nodesInRoute.add(linkFrom.getFromNode())) {
						log.add(new LoopMessage(transitLine, transitRoute, linkFrom.getFromNode()));
					}

					// angle check
					double angleDiff = getAzimuthDiff(linkFrom, linkTo);
					if(Math.abs(angleDiff) > uTurnAngleThreshold) {
						log.add(new DirectionChangeMessage(transitLine, transitRoute, linkFrom, linkTo, angleDiff));
					}
				}
			}
		}
	}

	public void printResult() {
		for(LogMessage entry : log) {
			System.out.println(entry);
		}
	}

	public void printSummary() {
		for(TransitLine transitLine : this.schedule.getTransitLines().values()) {
			for(TransitRoute transitRoute : transitLine.getRoutes().values()) {
				System.out.println(transitRoute.getId());
				System.out.println("    tt:   " + TravelTimeMessage.routeStat.get(transitRoute));
				System.out.println("    loop: " + LoopMessage.routeStat.get(transitRoute));
				System.out.println("    dir:  " + DirectionChangeMessage.routeStat.get(transitRoute));
			}
		}
	}

	public void printLineSummary() {
		for(TransitLine transitLine : this.schedule.getTransitLines().values()) {
				System.out.println(transitLine.getId());
				System.out.println("    tt:   " + TravelTimeMessage.lineStat.get(transitLine));
				System.out.println("    loop: " + LoopMessage.lineStat.get(transitLine));
				System.out.println("    dir:  " + DirectionChangeMessage.lineStat.get(transitLine));
			}
	}

	private double getAzimuthDiff(Link link1, Link link2) {
		double az1 = getAzimuth(link1.getFromNode().getCoord(), link1.getToNode().getCoord());
		double az2 = getAzimuth(link2.getFromNode().getCoord(), link2.getToNode().getCoord());

		return az2-az1;
	}
}