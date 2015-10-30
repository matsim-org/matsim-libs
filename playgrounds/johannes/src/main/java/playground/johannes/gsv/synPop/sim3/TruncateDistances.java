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

import playground.johannes.synpop.data.Attributable;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.processing.EpisodeTask;

public class TruncateDistances implements EpisodeTask {

	private final double limit;
	
	public TruncateDistances(double limit) {
		this.limit = limit;
	}
	
	@Override
	public void apply(Episode plan) {
		for(Attributable leg : plan.getLegs()) {
			String val = leg.getAttribute(CommonKeys.LEG_GEO_DISTANCE);
			if(val != null) {
				double d = Double.parseDouble(val);
				if(d > limit) {
					leg.setAttribute(CommonKeys.LEG_GEO_DISTANCE, String.valueOf(limit));
				}
			}
		}

	}

}
