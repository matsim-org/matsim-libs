/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesSpatialCut.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.balmermi.algos;

import org.matsim.core.api.facilities.Facilities;
import org.matsim.core.api.facilities.Facility;
import org.matsim.core.api.network.Link;
import org.matsim.gbl.Gbl;
import org.matsim.network.NetworkLayer;
import org.matsim.world.World;

public class FacilitiesCreateFromNetwork {

	private final NetworkLayer network;
	
	public FacilitiesCreateFromNetwork(NetworkLayer network) {
		this.network = network;
	}

	public void run(Facilities facilities) {
		if (!facilities.getFacilities().isEmpty()) { Gbl.errorMsg("Facilities not empty"); }
		for (Link l : network.getLinks().values()) {
			Facility f = facilities.createFacility(l.getId(),l.getCoord());
			f.createActivityOption("h");
			f.createActivityOption("w");
		}
	}
}
