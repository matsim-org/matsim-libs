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

import gnu.trove.TObjectDoubleHashMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.sna.util.ProgressLogger;

/**
 * @author johannes
 *
 */
public class PersonCloner {

	public static Set<ProxyPerson> weightedClones(Collection<ProxyPerson> persons, int N, Random random) {
		if(persons.size() == N) {
			return new HashSet<ProxyPerson>(persons); //TODO weights are left untouched
		} else if(persons.size() > N) {
			throw new IllegalArgumentException("Cannot shrink population.");
		}
		
		List<ProxyPerson> templates = new ArrayList<ProxyPerson>(persons);
		/*
		 * get max weight
		 */
		TObjectDoubleHashMap<ProxyPerson> weights = new TObjectDoubleHashMap<ProxyPerson>(persons.size());
		double maxW = 0;
//		double minW = Double.MAX_VALUE;
		for(ProxyPerson person : persons) {
			String wStr = person.getAttribute(CommonKeys.PERSON_WEIGHT);
			double w = 0;
			if(wStr != null) {
				w = Double.parseDouble(wStr);
			}
			weights.put(person, w);
			maxW = Math.max(w, maxW);
//			minW = Math.min(w, minW);
		}
		/*
		 * adjust weight so that max weight equals probability 1
		 */
		ProgressLogger.init(N, 2, 10);
		Set<ProxyPerson> clones = new HashSet<ProxyPerson>();
		while(clones.size() < N) {
			ProxyPerson template = templates.get(random.nextInt(templates.size()));
//			double w = (Double) template.getAttribute(CommonKeys.PERSON_WEIGHT);
			double w = weights.get(template);
			double p = w/maxW;
			if(p > random.nextDouble()) {
				StringBuilder builder = new StringBuilder();
				builder.append(template.getId());
				builder.append("clone");
				builder.append(clones.size());
				
				ProxyPerson clone = template.cloneWithNewId(builder.toString());
				clone.setAttribute(CommonKeys.PERSON_WEIGHT, "1.0");
				clones.add(clone);
				ProgressLogger.step();
			}
		}

		return clones;
	}
}
