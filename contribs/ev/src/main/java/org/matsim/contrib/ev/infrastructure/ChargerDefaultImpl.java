/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.ev.infrastructure;

import java.util.Objects;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.ev.charging.ChargingLogic;

import com.google.common.base.Preconditions;

class ChargerDefaultImpl implements Charger {

	private final ChargerSpecification specification;
	private final Link link;
	private final ChargingLogic logic;

	ChargerDefaultImpl( ChargerSpecification specification, Link link, ChargingLogic logic ) {
		Preconditions.checkArgument(link.getId().equals(specification.getLinkId()), "link.id != specification.linkId");
		this.specification = specification;
		this.link = link;
		this.logic = Objects.requireNonNull(logic);
	}

	@Override
	public ChargerSpecification getSpecification() {
		return specification;
	}

	@Override
	public ChargingLogic getLogic() {
		return logic;
	}

	@Override
	public Id<Charger> getId() {
		return specification.getId();
	}

	@Override
	public Link getLink() {
		return link;
	}

	@Override
	public String getChargerType() {
		return specification.getChargerType();
	}

	@Override
	public double getPlugPower() {
		return specification.getPlugPower();
	}

	@Override
	public int getPlugCount() {
		return specification.getPlugCount();
	}

	//TODO in order to add a separate coord: adapt DTD, ChargerSpecification and ChargerReader/Writer
	// Additionally, the reader and writer should convert coordinates if CRS different than that of the network
	@Override
	public Coord getCoord() {
		return link.getCoord();
	}
}
