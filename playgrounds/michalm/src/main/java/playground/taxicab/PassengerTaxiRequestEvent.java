/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.taxicab;

import org.matsim.api.core.v01.Id;
import org.matsim.core.events.AgentDepartureEventImpl;

/**
 * @author nagel
 *
 */
public class PassengerTaxiRequestEvent extends AgentDepartureEventImpl {
	// abusing the AgentDepartureEventImpl in order to avoid a new implementation (just laziness)

	public PassengerTaxiRequestEvent(double time, Id agentId, Id linkId, String legMode) {
		super(time, agentId, linkId, legMode);
		// TODO Auto-generated constructor stub
	}

}
