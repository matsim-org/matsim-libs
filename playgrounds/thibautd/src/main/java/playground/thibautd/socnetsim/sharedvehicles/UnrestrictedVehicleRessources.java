/* *********************************************************************** *
 * project: org.matsim.*
 * UnrestrictedVehicleRessources.java
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
package playground.thibautd.socnetsim.sharedvehicles;

import java.util.Collections;
import java.util.Set;

import org.matsim.api.core.v01.Id;

/**
 * One vehicle per agent
 * @author thibautd
 */
public class UnrestrictedVehicleRessources implements VehicleRessources {

	@Override
	public Set<Id> identifyVehiclesUsableForAgent(final Id person) {
		return Collections.singleton( person );
	}
}

