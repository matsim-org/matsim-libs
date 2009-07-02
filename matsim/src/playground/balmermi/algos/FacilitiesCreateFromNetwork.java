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

import org.matsim.core.api.facilities.ActivityFacilities;
import org.matsim.core.api.facilities.ActivityFacility;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.world.World;

public class FacilitiesCreateFromNetwork {

	private final NetworkLayer network;
	
	public FacilitiesCreateFromNetwork(NetworkLayer network) {
		this.network = network;
	}

	public void run(ActivityFacilities facilities) {
		if (!facilities.getFacilities().isEmpty()) { Gbl.errorMsg("Facilities not empty"); }
		for (LinkImpl l : network.getLinks().values()) {
			ActivityFacility f = facilities.createFacility(l.getId(),l.getCoord());
			f.createActivityOption("h");
			f.createActivityOption("w");
		}
	}
}
