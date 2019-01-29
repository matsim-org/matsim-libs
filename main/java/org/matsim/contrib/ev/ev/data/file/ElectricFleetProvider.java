/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.contrib.ev.ev.data.file;

import com.google.inject.Provider;
import org.matsim.contrib.ev.ev.data.ElectricFleet;
import org.matsim.contrib.ev.ev.data.ElectricFleetImpl;

import java.net.URL;

/**
 * @author michalm
 */
public class ElectricFleetProvider implements Provider<ElectricFleet> {
	private final URL url;

	public ElectricFleetProvider(URL url) {
		this.url = url;
	}

	@Override
	public ElectricFleet get() {
		ElectricFleetImpl fleet = new ElectricFleetImpl();
		new ElectricVehicleReader(fleet).parse(url);
		return fleet;
	}
}