/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultMobsimModule.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.core.mobsim;

import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.events.MobsimScopeEventHandlingModule;
import org.matsim.core.mobsim.hermes.HermesProvider;
import org.matsim.core.mobsim.qsim.QSimModule;

public class DefaultMobsimModule extends AbstractModule {
	@Override
	public void install() {

		var mobsimType = parseMobsimType(getConfig().controller().getMobsim());
		switch (mobsimType) {
			case ControllerConfigGroup.MobsimType.qsim -> install(new QSimModule());
			case ControllerConfigGroup.MobsimType.hermes -> bindMobsim().toProvider(HermesProvider.class);
			// Install qsim components, but without the default qsim
			// This has to be installed here, because of the shenanigans with the qsim components
			// Installed qsim components might depend on the order of modules
			case ControllerConfigGroup.MobsimType.dsim -> install(new QSimModule(false, false));
		}

		// dsim comes with its own scope event handling module
		if (!mobsimType.equals(ControllerConfigGroup.MobsimType.dsim)) {
			install(new MobsimScopeEventHandlingModule());
		}
	}

	private static ControllerConfigGroup.MobsimType parseMobsimType(String type) {
		try {
			return ControllerConfigGroup.MobsimType.valueOf(type);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(type + " is not supported. Supported types: [qsim, hermes, dsim]");
		}
	}
}
