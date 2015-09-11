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

package playground.johannes.synpop.processing;


import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.CommonValues;
import playground.johannes.synpop.data.Episode;

/**
 * @author johannes
 *
 */
public class ValidateOverlappingLegs implements EpisodeTask {

	@Override
	public void apply(Episode episode) {
		for(int i = 1; i < episode.getLegs().size(); i++) {
			String startTime = episode.getLegs().get(i).getAttribute(CommonKeys.LEG_START_TIME);
			String endTime = episode.getLegs().get(i - 1).getAttribute(CommonKeys.LEG_END_TIME);

			if(startTime != null && endTime != null) {
				double s = Double.parseDouble(startTime);
				double e = Double.parseDouble(endTime);

				if(s < e) {
					episode.setAttribute(CommonKeys.DELETE, CommonValues.TRUE);
					return;
				}
			}
		}
	}
}
