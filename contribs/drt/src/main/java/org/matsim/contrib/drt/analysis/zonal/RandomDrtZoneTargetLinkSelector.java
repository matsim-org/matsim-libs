/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
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

package org.matsim.contrib.drt.analysis.zonal;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.gbl.MatsimRandom;

import java.util.ArrayList;
import java.util.Random;

public class RandomDrtZoneTargetLinkSelector implements DrtZoneTargetLinkSelector{

	private final Random random = MatsimRandom.getLocalInstance();

	@Override
	public Link selectTargetLinkFor(DrtZone zone) {
		return new ArrayList<>(zone.getLinks().values()).get(random.nextInt(zone.getLinks().size()));
	}

}
