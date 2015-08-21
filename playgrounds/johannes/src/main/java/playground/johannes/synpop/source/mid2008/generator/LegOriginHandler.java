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

import java.util.Map;

import playground.johannes.gsv.synPop.ActivityType;
import playground.johannes.synpop.source.mid2008.MiDKeys;
import playground.johannes.synpop.data.Segment;
import playground.johannes.synpop.source.mid2008.MiDValues;

/**
 * @author johannes
 *
 */
public class LegOriginHandler implements LegAttributeHandler {

	@Override
	public void handle(Segment leg, Map<String, String> attributes) {
		String origin = attributes.get(VariableNames.LEG_ORIGIN);

		if(origin.equalsIgnoreCase("1"))
			leg.setAttribute(MiDKeys.LEG_ORIGIN, ActivityType.HOME);
		else if(origin.equalsIgnoreCase("2"))
			leg.setAttribute(MiDKeys.LEG_ORIGIN, ActivityType.WORK);
		else if(origin.equalsIgnoreCase("3"))
			leg.setAttribute(MiDKeys.LEG_ORIGIN, MiDValues.IN_TOWN);
		else if(origin.equalsIgnoreCase("4"))
			leg.setAttribute(MiDKeys.LEG_ORIGIN, MiDValues.OUT_OF_TOWN);

	}
}
