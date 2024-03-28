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

import java.util.function.IntUnaryOperator;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.core.gbl.MatsimRandom;

/**
 * @author tschlenther
 */
public class RandomDrtZoneTargetLinkSelector implements DrtZoneTargetLinkSelector {
	private final IntUnaryOperator random;

	public RandomDrtZoneTargetLinkSelector() {
		this(MatsimRandom.getLocalInstance()::nextInt);
	}

	public RandomDrtZoneTargetLinkSelector(IntUnaryOperator random) {
		this.random = random;
	}

	@Override
	public Link selectTargetLink(Zone zone) {
		return zone.getLinks().get(random.applyAsInt(zone.getLinks().size()));
	}
}
