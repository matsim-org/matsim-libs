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
import playground.johannes.gsv.synPop.ProxyObject;
import playground.johannes.gsv.synPop.io.AttributeSerializer;

/**
 * @author johannes
 *
 */
public class LegDistanceHandler implements LegAttributeHandler, AttributeSerializer {

	/* (non-Javadoc)
	 * @see playground.johannes.gsv.synPop.mid.LegAttributeHandler#handle(playground.johannes.gsv.synPop.ProxyLeg, java.util.Map)
	 */
	@Override
	public void handle(ProxyObject leg, Map<String, String> attributes) {
		String att = attributes.get(MIDKeys.LEG_DISTANCE);
		
		double d = Double.parseDouble(att);
		if(d < 9994) {
			d = d * 1000;
			leg.setAttribute(CommonKeys.LEG_ROUTE_DISTANCE, String.valueOf(d));
		} else {
			leg.setAttribute(CommonKeys.LEG_ROUTE_DISTANCE, null);
		}

	}

	@Override
	public String encode(Object value) {
		return String.valueOf((Double)value);
	}

	@Override
	public Object decode(String value) {
		return (Double)Double.parseDouble(value);
	}

}
