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

package org.matsim.core.mobsim.qsim.pt;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.pt.Umlauf;

/**
 * @author aneumann
 */
public class DefaultTransitDriverAgentFactory implements TransitDriverAgentFactory {

	@Override
	public AbstractTransitDriverAgent createTransitDriver(Umlauf umlauf, InternalInterface internalInterface, TransitStopAgentTracker transitStopAgentTracker) {
		return new TransitDriverAgentImpl(umlauf, TransportMode.car, transitStopAgentTracker, internalInterface);
	}

}
