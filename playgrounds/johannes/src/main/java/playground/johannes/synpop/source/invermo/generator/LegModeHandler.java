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

package playground.johannes.synpop.source.invermo.generator;

import playground.johannes.synpop.data.Attributable;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.CommonValues;

/**
 * @author johannes
 *
 */
public class LegModeHandler implements LegAttributeHandler {

	@Override
	public void handle(Attributable leg, String key, String value) {
		setMode(key, "hvm1", value, "plane", leg);
		setMode(key, "hvm2", value, "rail", leg);
		setMode(key, "hvm3", value, "rail", leg);
		setMode(key, "hvm6", value, CommonValues.LEG_MODE_CAR, leg);
		setMode(key, "hvm7", value, CommonValues.LEG_MODE_CAR, leg);
		setMode(key, "hvm9", value, CommonValues.LEG_MODE_CAR, leg);
		setMode(key, "hvm10", value, CommonValues.LEG_MODE_CAR, leg);
	}

	private void setMode(String key, String modeKey, String value, String mode, Attributable leg) {
		if(key.endsWith(modeKey)) {
			if(value.equals("1")) {
				if(leg.setAttribute(CommonKeys.LEG_MODE, mode) != null) {
					System.err.println("Overwriting mode key");
				}
			}
		}
	}
}
