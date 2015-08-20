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

import java.util.Map;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.synpop.data.Segment;
import playground.johannes.synpop.source.mid2008.generator.LegAttributeHandler;

/**
 * @author johannes
 *
 */
public class LegRoundTrip implements LegAttributeHandler {

	/* (non-Javadoc)
	 * @see playground.johannes.synpop.source.mid2008.generator.LegAttributeHandler#handle(playground.johannes.gsv.synPop.ProxyLeg, java.util.Map)
	 */
	@Override
	public void handle(Segment leg, Map<String, String> attributes) {
		String val = attributes.get(MIDKeys.LEG_DESTINATION);

		if(val.equalsIgnoreCase("Rundweg")) {
			leg.setAttribute(CommonKeys.LEG_ROUNDTRIP, "true");
		} else {
			leg.setAttribute(CommonKeys.LEG_ROUNDTRIP, "false");
		}
	}

}
