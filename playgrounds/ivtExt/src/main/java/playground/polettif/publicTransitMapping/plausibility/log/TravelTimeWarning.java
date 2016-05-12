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

package playground.polettif.publicTransitMapping.plausibility.log;

import org.matsim.core.utils.collections.MapUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import playground.polettif.publicTransitMapping.plausibility.PlausibilityCheck;
import playground.polettif.publicTransitMapping.tools.ScheduleTools;

import java.util.*;

public class TravelTimeWarning extends AbstractPlausibilityWarning {

	public static Map<TransitLine, Integer> lineStat = new HashMap<>();
	public static Map<TransitRoute, Integer> routeStat = new HashMap<>();

	private final TransitRouteStop fromStop;
	private final TransitRouteStop toStop;
	private final double ttActual;
	private double ttSchedule;

	public TravelTimeWarning(TransitLine transitLine, TransitRoute transitRoute, TransitRouteStop fromStop, TransitRouteStop toStop, double ttActual, double ttSchedule) {
		super(PlausibilityCheck.TRAVEL_TIME_WARNING, transitLine, transitRoute);
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

		linkIdList = ScheduleTools.getSubRouteLinkIds(transitRoute, fromStop.getStopFacility().getLinkId(), toStop.getStopFacility().getLinkId());

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
