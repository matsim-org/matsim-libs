
/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkChangeEventsQSimComponentsConfigurator.java
 *                                                                         *
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
 * *********************************************************************** */

 package org.matsim.core.mobsim.qsim.changeeventsengine;

import static org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsModule.NETWORK_CHANGE_EVENTS_ENGINE_NAME;

import org.matsim.core.config.Config;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfig;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigurator;

public class NetworkChangeEventsQSimComponentsConfigurator implements QSimComponentsConfigurator {
	final private Config config;

	public NetworkChangeEventsQSimComponentsConfigurator(Config config) {
		this.config = config;
	}

	@Override
	public void configure(QSimComponentsConfig components) {
		if (config.network().isTimeVariantNetwork()) {
			components.addNamedComponent(NETWORK_CHANGE_EVENTS_ENGINE_NAME);
		}
	}
}
