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

package playground.johannes.gsv.synPop.mid;

import playground.johannes.gsv.synPop.ProxyPlanTask;
import playground.johannes.synpop.data.Element;
import playground.johannes.synpop.data.Episode;

import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author johannes
 *
 */
public class SortLegsTask implements ProxyPlanTask {

	@Override
	public void apply(Episode plan) {
		SortedMap<Integer, Element> map = new TreeMap<Integer, Element>();
		
		for(Element leg : plan.getLegs()) {
			Integer idx = Integer.parseInt(leg.getAttribute(MIDKeys.LEG_INDEX));
			map.put(idx, leg);
		}
		
		plan.getLegs().clear();
		for(Entry<Integer, Element> entry : map.entrySet()) {
			plan.addLeg(entry.getValue());
		}
	}
}
