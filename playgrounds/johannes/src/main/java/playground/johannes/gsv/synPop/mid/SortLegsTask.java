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

import playground.johannes.synpop.processing.EpisodeTask;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.Segment;
import playground.johannes.synpop.source.mid2008.MiDKeys;

import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author johannes
 *
 */
public class SortLegsTask implements EpisodeTask {

	@Override
	public void apply(Episode plan) {
		SortedMap<Integer, Segment> map = new TreeMap<Integer, Segment>();
		
		for(Segment leg : plan.getLegs()) {
			Integer idx = Integer.parseInt(leg.getAttribute(MiDKeys.LEG_INDEX));
			map.put(idx, leg);
		}
		
		plan.getLegs().clear();
		for(Entry<Integer, Segment> entry : map.entrySet()) {
			plan.addLeg(entry.getValue());
		}
	}
}
