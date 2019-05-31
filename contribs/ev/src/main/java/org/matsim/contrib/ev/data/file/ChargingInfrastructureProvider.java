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

package org.matsim.contrib.ev.data.file;

import java.net.URL;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureImpl;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

/**
 * @author michalm
 */
public class ChargingInfrastructureProvider implements Provider<ChargingInfrastructure> {
	@Inject
	@Named(ChargingInfrastructure.CHARGERS)
	private Network network;

	private final URL url;

	public ChargingInfrastructureProvider(URL url) {
		this.url = url;
	}

	@Override
	public ChargingInfrastructure get() {
		ChargingInfrastructureImpl chargingInfrastructure = new ChargingInfrastructureImpl();
		new ChargerReader(network, chargingInfrastructure).parse(url);
		return chargingInfrastructure;
	}
}
