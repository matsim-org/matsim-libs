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

package playground.johannes.gsv.synPop.invermo;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyObject;

/**
 * @author johannes
 *
 */
public class LegPurposeHandler implements LegAttributeHandler {

	@Override
	public void handle(ProxyObject leg, int idx, String key, String value) {
		if(key.endsWith("zweck1")) {
			leg.setAttribute(CommonKeys.LEG_PURPOSE, "leisure");
		} else if(key.endsWith("zweck2")) {
			leg.setAttribute(CommonKeys.LEG_PURPOSE, "shop");
		} else if(key.endsWith("zweck3")) {
			leg.setAttribute(CommonKeys.LEG_PURPOSE, "leisure");
		} else if(key.endsWith("zweck4")) {
			leg.setAttribute(CommonKeys.LEG_PURPOSE, "leisure");
		} else if(key.endsWith("zweck5")) {
			leg.setAttribute(CommonKeys.LEG_PURPOSE, "leisure");
		} else if(key.endsWith("zweck6")) {
			leg.setAttribute(CommonKeys.LEG_PURPOSE, "leisure");
		} else if(key.endsWith("zweck7")) {
			leg.setAttribute(CommonKeys.LEG_PURPOSE, "leisure");
		} else if(key.endsWith("zweck8")) {
			leg.setAttribute(CommonKeys.LEG_PURPOSE, "work");
		} else if(key.endsWith("zweck9")) {
			leg.setAttribute(CommonKeys.LEG_PURPOSE, "misc");
		}

	}

}
