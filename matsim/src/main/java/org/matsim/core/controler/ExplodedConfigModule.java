
/* *********************************************************************** *
 * project: org.matsim.*
 * ExplodedConfigModule.java
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

 package org.matsim.core.controler;

import com.google.inject.Binder;
import com.google.inject.Module;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;

public final class ExplodedConfigModule implements Module {
	private final Config config;

	public ExplodedConfigModule(Config config) {
		this.config = config;
	}

	@Override
	public void configure(Binder binder) {
		binder.bind(Config.class).toInstance(config);
		for (ConfigGroup configGroup : config.getModules().values()) {
			Class materializedConfigGroupSubclass = configGroup.getClass();
			if (materializedConfigGroupSubclass != ConfigGroup.class) {
				binder.bind(materializedConfigGroupSubclass).toInstance(configGroup);
			}
		}
	}
}
