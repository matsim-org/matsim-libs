/* *********************************************************************** *
 * project: org.matsim.*
 * PtRouteParser.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.marcel.pt.implementations.routes;

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.utils.misc.Time;

import playground.marcel.pt.interfaces.routes.PtRoute;

public class PtRouteParser {

	public PtRoute createPtRoute(final String stringRepresentation) {
		// TODO [MR] continue work here
		final Id departureId = new IdImpl(1);
		final Id arrivalId = new IdImpl(2);
		final Id lineId = new IdImpl(3);
		final double travelTime = Time.UNDEFINED_TIME;
		PtRoute ptRoute = new PtRouteImpl(lineId, departureId, arrivalId, travelTime);

		return ptRoute;
	}
}
