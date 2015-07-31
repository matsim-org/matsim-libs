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

package playground.johannes.gsv.synPop.sim3;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.socialnetworks.utils.CollectionUtils;
import playground.johannes.synpop.data.Element;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.PlainPerson;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author johannes
 *
 */
public class DistancePopSegmenter implements PopulationSegmenter {

//	private static final Logger logger = Logger.getLogger(DistancePopSegmenter.class);
	
	private final double threshold;
	
	public DistancePopSegmenter(double threshold) {
		this.threshold = threshold;
	}
	
	@Override
	public List<PlainPerson>[] split(Collection<PlainPerson> persons, int segments) {
		List<PlainPerson> shortDist = new ArrayList<>(persons.size());
		List<PlainPerson> longDist = new ArrayList<>(persons.size());
		
		for(PlainPerson person : persons) {
			double max = 0;
			for(Episode plan : person.getPlans()) {
				for(Element leg : plan.getLegs()) {
					String val = leg.getAttribute(CommonKeys.LEG_ROUTE_DISTANCE);
					if(val != null) {
						max = Math.max(Double.parseDouble(val), max);
					}
				}
			}
			
			if(max > threshold) {
				longDist.add(person);
			} else {
				shortDist.add(person);
			}
		}
		
		int n = (int) Math.ceil(segments/2.0);
		List<PlainPerson>[] shortSegements = CollectionUtils.split(shortDist, n);
		List<PlainPerson>[] longSegments = CollectionUtils.split(longDist, segments - n);
		
		List<PlainPerson>[] list = new List[segments];
		for(int i = 0; i < shortSegements.length; i++) {
			list[i] = shortSegements[i];
		}
		
		for(int i = shortSegements.length; i < segments; i++) {
			list[i] = longSegments[i - shortSegements.length];
		}
		
		return list;
	}

}
