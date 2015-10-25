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

import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Segment;

import java.util.Map;

/**
 * @author johannes
 * 
 */
public class LegDistanceHandler implements LegAttributeHandler {

	@Override
	public void handle(Segment leg, Map<String, String> attributes) {
		String att = attributes.get(VariableNames.LEG_DISTANCE);

		double d = Double.parseDouble(att);
		if (d <= 950) {
			d = d * 1000;
			leg.setAttribute(CommonKeys.LEG_ROUTE_DISTANCE, String.valueOf(d));
		} else {
			leg.setAttribute(CommonKeys.LEG_ROUTE_DISTANCE, null);
		}
	}
}
