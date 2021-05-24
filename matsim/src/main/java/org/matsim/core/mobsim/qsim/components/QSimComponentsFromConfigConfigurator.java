
/* *********************************************************************** *
 * project: org.matsim.*
 * QSimComponentsFromConfigConfigurator.java
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

 package org.matsim.core.mobsim.qsim.components;

import org.matsim.core.config.Config;

public class QSimComponentsFromConfigConfigurator implements QSimComponentsConfigurator {
	final private Config config;

	public QSimComponentsFromConfigConfigurator(Config config) {
		this.config = config;
	}

	@Override
	public void configure(QSimComponentsConfig components) {
		QSimComponentsConfigGroup componentsConfig = (QSimComponentsConfigGroup) config.getModules()
				.get(QSimComponentsConfigGroup.GROUP_NAME);
		// we do not want addOrCreateModule(...) since the design has a specific execution path for "null" (see below).  kai, nov'19

		if (componentsConfig != null) {
			components.clear();

			// TODO: Eventually, here a translation of strings to more specific annotations
			// could happen if we ever want a full config-configurable QSim.
			componentsConfig.getActiveComponents().forEach(components::addNamedComponent);
		}
	}
}
