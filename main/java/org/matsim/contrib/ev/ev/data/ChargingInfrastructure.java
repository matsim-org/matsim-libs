/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.contrib.ev.ev.data;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.ev.ev.charging.ChargingLogic;
import org.matsim.core.api.experimental.events.EventsManager;

import java.util.Map;

/**
 * @author michalm
 */
public interface ChargingInfrastructure {
	public static final String CHARGERS = "chargers";

	Map<Id<Charger>, Charger> getChargers();

    Map<Id<Charger>, Charger> getChargersAtLink(Id<Link> linkId);


	void initChargingLogics(ChargingLogic.Factory logicFactory, EventsManager eventsManager);
}
