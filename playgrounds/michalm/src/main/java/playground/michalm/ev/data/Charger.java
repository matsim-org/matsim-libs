/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.michalm.ev.data;

import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.network.Link;

import playground.michalm.ev.charging.ChargingLogic;

public interface Charger extends BasicLocation<Charger> {
	ChargingLogic getLogic();

	void setLogic(ChargingLogic logic);

	Link getLink();

	/**
	 * @return max power at a single plug, in [W]
	 */
	double getPower();

	/**
	 * @return number of plugs
	 */
	int getPlugs();

	void resetLogic();
}
