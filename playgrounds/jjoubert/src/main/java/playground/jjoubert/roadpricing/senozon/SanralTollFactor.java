/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.jjoubert.roadpricing.senozon;

import org.matsim.api.core.v01.Id;

public abstract class SanralTollFactor {

	public static double getTollFactor(final Id vehicleId) {
		long id = Long.parseLong(vehicleId.toString());
		if (id < 1000000) {
			return 1.0;
		} else if (id < 2000000) {
			return 2.0;
		} else if (id < 3500000) {
			return 5.0;
		} else {
			return 0.0;
		}
	}
}
