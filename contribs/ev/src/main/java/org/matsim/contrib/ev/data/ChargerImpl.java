/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.contrib.ev.data;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.ev.charging.ChargingLogic;

public class ChargerImpl implements Charger {
	private final Id<Charger> id;
	private final double power;
	private final int plugs;
	private final Link link;
	private final Coord coord;
	private final String chargerType;
	private ChargingLogic logic;

	public static final String DEFAULT_CHARGER_TYPE = "default";


	/**
	 * 
	 * @param id
	 *            chargerID
	 * @param power_w
	 *            Power per Charging Spot in watt
	 * @param plugs
	 *            Number of simultaneously usable charging outlets
	 * @param link
	 *            Associated Link Id
	 */
	public ChargerImpl(Id<Charger> id, double power_w, int plugs, Link link) {
		this.id = id;
		this.power = power_w;
		this.plugs = plugs;
		this.link = link;
		this.chargerType = DEFAULT_CHARGER_TYPE;
		this.coord = link.getCoord();
	}

	public ChargerImpl(Id<Charger> id, double power_w, int plugs, Link link, Coord coord, String chargerType) {
		this.id = id;
		this.power = power_w;
		this.plugs = plugs;
		this.link = link;
		this.coord = coord;
		this.chargerType = chargerType;
	}

	@Override
	public ChargingLogic getLogic() {
		return logic;
	}

	@Override
	public void setLogic(ChargingLogic logic) {
		this.logic = logic;
	}

	@Override
	public String getChargerType() {
		return chargerType;
	}

	@Override
	public Id<Charger> getId() {
		return id;
	}

	@Override
	public double getPower() {
		return power;
	}

	@Override
	public int getPlugs() {
		return plugs;
	}

	@Override
	public Link getLink() {
		return link;
	}

	@Override
	public Coord getCoord() {
		return coord;
	}
}
