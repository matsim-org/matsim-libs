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

import playground.johannes.synpop.data.Segment;
import playground.johannes.synpop.source.mid2008.MiDKeys;
import playground.johannes.synpop.source.mid2008.MiDValues;

import java.util.Map;

/**
 * @author johannes
 *
 */
public class JourneyDestinationHandler implements LegAttributeHandler {

	@Override
	public void handle(Segment leg, Map<String, String> attributes) {
		String value = attributes.get(VariableNames.JOURNEY_DESTINATION);

		if("1".equals(value)) {
			leg.setAttribute(MiDKeys.LEG_DESTINATION, MiDValues.DOMESTIC);
		} else if("2".equals(value)) {
			leg.setAttribute(MiDKeys.LEG_DESTINATION, MiDValues.ABROAD);
		} else if("3".equals(value)) {
			leg.setAttribute(MiDKeys.LEG_DESTINATION, MiDValues.ABROAD);
		}
	}

}
