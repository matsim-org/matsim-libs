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

import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyObject;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.ProxyPlan;
import playground.johannes.sna.math.Discretizer;
import playground.johannes.sna.math.LinearDiscretizer;

/**
 * @author johannes
 *
 */
public class DistancePopSegmenter implements PopulationSegmenter {

	@Override
	public List<ProxyPerson>[] split(Collection<ProxyPerson> persons, int segments) {
		double max = 0;
		
		TObjectDoubleHashMap<ProxyPerson> values = new TObjectDoubleHashMap<>(persons.size());
		
		for(ProxyPerson person : persons) {
			double sum = 0;
			for(ProxyPlan plan : person.getPlans()) {
				for(ProxyObject leg : plan.getLegs()) {
					String val = leg.getAttribute(CommonKeys.LEG_DISTANCE);
					if(val != null) {
						sum += Double.parseDouble(val);
					}
				}
			}
			
			max = Math.max(sum, max);
			values.put(person, sum);
		}
		
		double width = Math.ceil(max/(double)(segments-1));
		Discretizer disc = new LinearDiscretizer(width);
		
		List<ProxyPerson>[] bins = new List[segments];
		
		for(int i = 0; i < segments; i++) {
			 bins[i] = new ArrayList<>(persons.size());
		}
		
		TObjectDoubleIterator<ProxyPerson> it = values.iterator();
		for(int i = 0; i < values.size(); i++) {
			it.advance();
			int bin = (int) disc.index(it.value());
			bins[bin].add(it.key());
		}
		
		return bins;
	}

}
