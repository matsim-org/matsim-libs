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

package playground.polettif.multiModalMap.plausibility.log;

import com.vividsolutions.jts.geom.Coordinate;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import playground.polettif.multiModalMap.plausibility.PlausibilityCheck;
import playground.polettif.multiModalMap.tools.ScheduleTools;

import java.util.*;

public class TravelTimeWarning extends PlausibilityWarningAbstract {

	public static Map<TransitLine, Integer> lineStat = new HashMap<>();
	public static Map<TransitRoute, Integer> routeStat = new HashMap<>();

	private final TransitRouteStop fromStop;
	private final TransitRouteStop toStop;
	private final double ttActual;
	private double ttSchedule;

	public TravelTimeWarning(TransitLine transitLine, TransitRoute transitRoute, TransitRouteStop fromStop, TransitRouteStop toStop, double ttActual, double ttSchedule) {
		super(1, "tt", transitLine, transitRoute);
		this.fromStop = fromStop;
		this.toStop = toStop;
		this.ttActual = ttActual;
		this.ttSchedule = ttSchedule;

		pair = new Tuple<>(fromStop, toStop);
		fromId = fromStop.getStopFacility().getId().toString();
		toId = toStop.getStopFacility().getId().toString();
		expected = ttSchedule;
		actual = ttActual;
		difference = ttActual - ttSchedule;

		// create coordinates
		List<Coordinate> coordList = new ArrayList<>();
		linkList = ScheduleTools.getSubRouteLinkIds(transitRoute, fromStop.getStopFacility().getLinkId(), toStop.getStopFacility().getLinkId());
		for(Id<Link> linkId : linkList) {
			coordList.add(MGC.coord2Coordinate(net.getLinks().get(linkId).getFromNode().getCoord()));
		}
		coordList.add(MGC.coord2Coordinate(net.getLinks().get(linkList.get(linkList.size()-1)).getToNode().getCoord()));
		coordinates = new Coordinate[coordList.size()];
		coordList.toArray(coordinates);

		MapUtils.addToInteger(transitLine, lineStat, 1, 1);
		MapUtils.addToInteger(transitRoute, routeStat, 1, 1);
	}

	@Override
	public String toString() {
		return "\tTT INCONSISTENT \tstops: "+fromStop.getStopFacility().getId()+" -> "+toStop.getStopFacility().getId()+"\n" +
			   "\t                \t\tdifference: "+String.format("%.1f", (ttActual-ttSchedule))+"\ttt network: "+String.format("%.1f",ttActual)+"\ttt schedule: "+String.format("%.1f",ttSchedule);
	}

	public String linkMessage() {
		return 	"\tstops: "+fromStop.getStopFacility().getId()+" -> "+toStop.getStopFacility().getId()+"\n" +
				"\tdifference: "+String.format("%.1f", (ttActual-ttSchedule))+"\ttt network: "+String.format("%.1f",ttActual)+"\ttt schedule: "+String.format("%.1f",ttSchedule);

	}
}
