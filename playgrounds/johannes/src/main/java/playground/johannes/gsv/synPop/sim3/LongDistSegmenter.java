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
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.socialnetworks.utils.CollectionUtils;
import playground.johannes.synpop.data.Element;
import playground.johannes.synpop.data.Episode;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author johannes
 *
 */
public class LongDistSegmenter implements PopulationSegmenter {

	private final double threshold;
	
	public LongDistSegmenter(double threshold) {
		this.threshold = threshold;
	}
	
	/* (non-Javadoc)
	 * @see playground.johannes.gsv.synPop.sim3.PopulationSegmenter#split(java.util.Collection, int)
	 */
	@Override
	public List<ProxyPerson>[] split(Collection<ProxyPerson> persons, int segments) {
		Set<ProxyPerson> longDist = new HashSet<>(persons.size());
		
		for(ProxyPerson person : persons) {
			double max = 0;
			for(Episode plan : person.getPlans()) {
				for(Element leg : plan.getLegs()) {
					String val = leg.getAttribute(CommonKeys.LEG_GEO_DISTANCE);
					if(val != null) {
						max = Math.max(Double.parseDouble(val), max);
					}
				}
			}
			
			if(max > threshold) {
				longDist.add(person);
			}
		}
		
		return CollectionUtils.split(longDist, segments);
	}

}
