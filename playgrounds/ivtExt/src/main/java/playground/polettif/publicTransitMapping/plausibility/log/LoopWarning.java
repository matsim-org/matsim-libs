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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import playground.polettif.publicTransitMapping.plausibility.PlausibilityCheck;
import playground.polettif.publicTransitMapping.tools.ScheduleTools;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class LoopWarning extends AbstractPlausibilityWarning {

	public static Map<TransitLine, Integer> lineStat = new HashMap<>();
	public static Map<TransitRoute, Integer> routeStat = new HashMap<>();

	private Node node;

	public LoopWarning(TransitLine transitLine, TransitRoute transitRoute, Node node, Link firstLoopLink, Link lastLoopLink) {
		super(PlausibilityCheck.LOOP_WARNING, transitLine, transitRoute);
		this.node = node;

		linkIdList = ScheduleTools.getLoopSubRouteLinkIds(transitRoute, firstLoopLink.getId(), lastLoopLink.getId());

		fromId = firstLoopLink.getId().toString();
		toId = lastLoopLink.getId().toString();
		expected = 0;
		actual = 0;

		MapUtils.addToInteger(transitLine, lineStat, 1, 1);
		MapUtils.addToInteger(transitRoute, routeStat, 1, 1);
	}

	public LoopWarning(TransitLine transitLine, TransitRoute transitRoute, List<Id<Link>> loop) {
		super(PlausibilityCheck.LOOP_WARNING, transitLine, transitRoute);

		linkIdList = loop;

		fromId = loop.get(0).toString();
		toId = loop.get(loop.size()-1).toString();
		expected = 0;
		actual = 0;

		MapUtils.addToInteger(transitLine, lineStat, 1, 1);
		MapUtils.addToInteger(transitRoute, routeStat, 1, 1);
	}

	@Override
	public String toString() {
		return "\tLOOP            \tnode: "+node.getId();
	}
}
