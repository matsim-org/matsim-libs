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

package playground.johannes.synpop.source.mid2008.generator;

import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.source.mid2008.MiDKeys;

import java.util.Map;

/**
 * @author johannes
 *
 */
public class JourneyDaysHandler implements EpisodeAttributeHandler {

	@Override
	public void handle(Episode plan, Map<String, String> attributes) {
		String value = attributes.get(VariableNames.JOURNEY_NIGHTS);
		if(value != null) {
			int nights = Integer.parseInt(value);

			if (nights < 95)
				plan.setAttribute(MiDKeys.JOURNEY_DAYS, String.valueOf(nights + 1));
		}
	}

}
