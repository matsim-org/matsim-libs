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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.events.MobsimScopeEventHandlingModule;
import org.matsim.core.mobsim.hermes.HermesProvider;
import org.matsim.core.mobsim.qsim.QSimModule;

public class DefaultMobsimModule extends AbstractModule {
	private static final Logger log = LogManager.getLogger(DefaultMobsimModule.class);

	@Override
	public void install() {

		var mobsimType = getConfig().controller().getMobsim();
		if (mobsimType.equals(ControllerConfigGroup.MobsimType.qsim.name())) {
			install(new QSimModule());
		} else if (mobsimType.equals(ControllerConfigGroup.MobsimType.hermes.name())) {
			bindMobsim().toProvider(HermesProvider.class);
		} else if (mobsimType.equals(ControllerConfigGroup.MobsimType.dsim.name())) {
			// Install qsim components, but without the default qsim
			// This has to be installed here, because of the shenanigans with the qsim components
			// Installed qsim components might depend on the order of modules
			install(new QSimModule(false, false));
		} else if (mobsimType.equals("JDEQSim")) {
			throw new IllegalArgumentException("JDEQSim is not supported anymore, since March 2025. Use one of: qsim, hermes, dsim instead");
		} else {
			log.warn("Unknown mobsim type: " + mobsimType + " When using something other than qsim, hermes or dsim, you have to provide your own mobsim implementation!");
		}

		// dsim comes with its own scope event handling module
		if (!mobsimType.equals(ControllerConfigGroup.MobsimType.dsim.name())) {
			install(new MobsimScopeEventHandlingModule());
		}
	}
}
