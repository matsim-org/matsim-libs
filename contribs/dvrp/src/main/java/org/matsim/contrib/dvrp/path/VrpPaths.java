/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.path;

import java.util.ArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch.PathData;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelTime;

import com.google.common.base.Preconditions;

public class VrpPaths {
	/**
	 * ASSUMPTION: A vehicle enters and exits links at their ends (link.getToNode())
	 */
	public static VrpPathWithTravelData calcAndCreatePath(Link fromLink, Link toLink, double departureTime,
			LeastCostPathCalculator router, TravelTime travelTime) {
		Path path = fromLink == toLink ?
				null :
				router.calcLeastCostPath(fromLink.getToNode(), toLink.getFromNode(), departureTime + FIRST_LINK_TT,
						null, null);
		return VrpPaths.createPath(fromLink, toLink, departureTime, path, travelTime);
	}

	public static VrpPathWithTravelData calcAndCreatePathForDiversion(LinkTimePair diversionPoint, Link toLink,
			LeastCostPathCalculator router, TravelTime travelTime) {
		Path path = diversionPoint.link == toLink ?
				null :
				router.calcLeastCostPath(diversionPoint.link.getToNode(), toLink.getFromNode(), diversionPoint.time,
						null, null);
		return VrpPaths.createPathForDiversion(diversionPoint, toLink, path, travelTime);
	}

	public static VrpPathWithTravelData createZeroLengthPath(Link fromTolink, double departureTime,
			boolean calculateForDiversion) {
		// To make sure that arrival time stays correct although no special diversion
		// will be implemented after calling OnlienDriveTaskTracker.divert
		double travelTime = calculateForDiversion ? -NODE_TRANSITION_TIME : 0.0;
		return new VrpPathWithTravelDataImpl(departureTime, travelTime, new Link[] { fromTolink }, new double[] { 0 });
	}

	public static VrpPathWithTravelData createZeroLengthPathForDiversion(LinkTimePair diversionPoint) {
		return createZeroLengthPath(diversionPoint.link, diversionPoint.time, true);
	}

	public static VrpPathWithTravelData createPath(Link fromLink, Link toLink, double departureTime, PathData pathData,
			TravelTime travelTime) {
		return createPath(fromLink, toLink, departureTime, pathData.getPath(), travelTime);
	}

	public static VrpPathWithTravelData createPathForDiversion(LinkTimePair diversionPoint, Link toLink,
			PathData pathData, TravelTime travelTime) {
		return createPathForDiversion(diversionPoint, toLink, pathData.getPath(), travelTime);
	}

	public static VrpPathWithTravelData createPath(Link fromLink, Link toLink, double departureTime, Path path,
			TravelTime travelTime) {
		return createPath(fromLink, toLink, departureTime, path, travelTime, true, false);
	}

	public static VrpPathWithTravelData createPathForDiversion(LinkTimePair diversionPoint, Link toLink, Path path,
			TravelTime travelTime) {
		return createPath(diversionPoint.link, toLink, diversionPoint.time, path, travelTime, true, true);
	}

	public static VrpPathWithTravelData createPath(Link fromLink, Link toLink, double departureTime, Path path,
			TravelTime travelTime, boolean considerZeroLengthPath, boolean calculateForDiversion) {
		if (considerZeroLengthPath && fromLink == toLink) {
			return createZeroLengthPath(fromLink, departureTime, calculateForDiversion);
		}

		int count = path.links.size();
		if (count > 0) {
			Preconditions.checkArgument(fromLink.getToNode() == path.links.get(0).getFromNode(),
					"fromLink and path are not connected. From link: %s. First link in path: %s", fromLink,
					path.links.get(0));
			Preconditions.checkArgument(path.links.get(count - 1).getToNode() == toLink.getFromNode(),
					"path and toLink are not connected. To link: %s. Last link in path: %s", toLink,
					path.links.get(count - 1));
		}

		Link[] links = new Link[count + 2];
		double[] linkTTs = new double[count + 2];

		// we start at the end of fromLink
		// actually, in QSim, it usually takes 1 second to move over the first node
		// (when INSERTING_WAITING_VEHICLES_BEFORE_DRIVING_VEHICLES is ON;
		// otherwise it can take much longer)
		double currentTime = departureTime;
		links[0] = fromLink;
		double linkTT = !calculateForDiversion ? FIRST_LINK_TT : 0.0;
		linkTTs[0] = linkTT;
		currentTime += linkTT;

		for (int i = 1; i <= count; i++) {
			Link link = path.links.get(i - 1);
			links[i] = link;
			linkTT = travelTime.getLinkTravelTime(link, currentTime, null, null);
			linkTTs[i] = linkTT;
			currentTime += linkTT;
		}

		// there is no extra time spent on queuing at the end of the last link
		links[count + 1] = toLink;
		linkTT = travelTime.getLinkTravelTime(toLink, currentTime, null, null);
		linkTTs[count + 1] = linkTT;

		double totalTT = (!calculateForDiversion ? FIRST_LINK_TT : 0.0) + path.travelTime + linkTT;
		totalTT -= NODE_TRANSITION_TIME;

		return new VrpPathWithTravelDataImpl(departureTime, totalTT, links, linkTTs);
	}

	public static final double FIRST_LINK_TT = 2;
	public static final double NODE_TRANSITION_TIME = 1;

	/**
	 * It would make sense to replace these calls with TravelTime.getTravelTime(...) whereever it is used /sh nov 21
	 */
	@Deprecated
	public static double getLastLinkTT(Link lastLink, double time) {
		// XXX imprecise if qsimCfg.timeStepSize != 1
		return Math.floor(lastLink.getLength() / lastLink.getFreespeed(time)) - NODE_TRANSITION_TIME;
	}

	public static double getLastLinkTT(TravelTime travelTime, Link lastLink, double time) {
		return travelTime.getLinkTravelTime(lastLink, time, null, null) - NODE_TRANSITION_TIME;
	}

	/**
	 * Used for OTFVis. Does not contain info on timing, distance and cost. Can be extended...
	 *
	 * @param path
	 * @param routeFactories
	 * @return
	 */
	public static NetworkRoute createNetworkRoute(VrpPath path, RouteFactories routeFactories) {
		Id<Link> fromLinkId = path.getFromLink().getId();
		Id<Link> toLinkId = path.getToLink().getId();
		NetworkRoute route = routeFactories.createRoute(NetworkRoute.class, fromLinkId, toLinkId);

		int length = path.getLinkCount();
		if (length >= 2) {// means: fromLink != toLink
			// all except the first and last ones (== fromLink and toLink)
			ArrayList<Id<Link>> linkIdList = new ArrayList<>(length - 2);
			for (int i = 1; i < length - 1; i++) {
				linkIdList.add(path.getLink(i).getId());
			}
			route.setLinkIds(fromLinkId, linkIdList, toLinkId);
		}

		return route;
	}

	/**
	 * @return The distance of a VRP path Includes the to link, but not the from link
	 */
	public static double calcDistance(VrpPath path) {
		double distance = 0.0;
		for (int i = 1; i < path.getLinkCount(); i++) {
			distance += path.getLink(i).getLength();
		}
		return distance;
	}
}
