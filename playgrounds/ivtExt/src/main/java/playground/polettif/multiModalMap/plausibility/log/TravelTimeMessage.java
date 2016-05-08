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

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;

import java.util.HashMap;
import java.util.Map;

public class TravelTimeMessage implements LogMessage {

	public static Map<TransitLine, Integer> lineStat = new HashMap<>();
	public static Map<TransitRoute, Integer> routeStat = new HashMap<>();

	private final TransitLine transitLine;
	private final TransitRoute transitRoute;
	private final TransitRouteStop fromStop;
	private final TransitRouteStop toStop;
	private final double ttActual;
	private double ttSchedule;

	public TravelTimeMessage(TransitLine transitLine, TransitRoute transitRoute, TransitRouteStop fromStop, TransitRouteStop toStop, double ttActual, double ttSchedule) {
		this.transitLine = transitLine;
		this.transitRoute = transitRoute;
		this.fromStop = fromStop;
		this.toStop = toStop;
		this.ttActual = ttActual;
		this.ttSchedule = ttSchedule;

		MapUtils.addToInteger(transitLine, lineStat, 1, 1);
		MapUtils.addToInteger(transitRoute, routeStat, 1, 1);
	}

	@Override
	public String toString() {
		return "TRAVEL TIME INCONSISTENT\tline " +transitLine.getId()+"\troute "+transitRoute.getId()+"\t"+ttActual+" "+ttSchedule;
	}

}
