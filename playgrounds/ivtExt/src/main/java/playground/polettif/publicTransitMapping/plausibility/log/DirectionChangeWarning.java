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

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import playground.polettif.publicTransitMapping.plausibility.PlausibilityCheck;
import playground.polettif.publicTransitMapping.tools.ScheduleTools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DirectionChangeWarning extends AbstractPlausibilityWarning {

	public static Map<TransitLine, Integer> lineStat = new HashMap<>();
	public static Map<TransitRoute, Integer> routeStat = new HashMap<>();

	private final double diff;

	public DirectionChangeWarning(TransitLine transitLine, TransitRoute transitRoute, Link link1, Link link2, double threshold, double diff) {
		super(PlausibilityCheck.DIRECTION_CHANGE_WARNING, transitLine, transitRoute);
		this.fromId = link1.getId().toString();
		this.toId = link2.getId().toString();
		this.diff = diff;

		expected = threshold;
		actual = threshold+diff;
		difference = diff;

		linkIdList = new ArrayList<>();
		linkIdList.add(link1.getId());
		linkIdList.add(link2.getId());

		MapUtils.addToInteger(transitLine, lineStat, 1, 1);
		MapUtils.addToInteger(transitRoute, routeStat, 1, 1);
	}

	@Override
	public String toString() {
		return "\tDIRECTION CHANGE\tlinks: "+fromId+"\t->\t"+toId+"\t\tdifference: "+diff*200/Math.PI +" gon";
	}
}
