/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityChains.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.sim.analysis;

import gnu.trove.TObjectDoubleHashMap;

import java.util.Set;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;

/**
 * @author illenberger
 *
 */
public class ActivityChains {

	public TObjectDoubleHashMap<String> chains(Set<Plan> plans) {
		TObjectDoubleHashMap<String> hist = new TObjectDoubleHashMap<String>();
		
		for(Plan plan : plans) {
			StringBuilder builder = new StringBuilder();
			for(int i = 0; i < plan.getPlanElements().size(); i += 2) {
				String type = ((Activity) plan.getPlanElements().get(i)).getType();
				builder.append(type);
				builder.append("-");
			}
			
			String chain = builder.toString();
			
			hist.adjustOrPutValue(chain, 1, 1);
		}
		
		return hist;
	}
}
