/* *********************************************************************** *
 * project: org.matsim.* 												   *
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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
package ch.sbb.matsim.mobsim.qsim;

import ch.sbb.matsim.config.SBBTransitConfigGroup;
import ch.sbb.matsim.mobsim.qsim.pt.SBBTransitEngineQSimModule;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.controler.AbstractModule;

/**
 * @author Sebastian Hörl / ETHZ
 */
public class SBBTransitModule extends AbstractModule {

	@Override
	public void install() {
		ConfigGroup existing = getConfig().getModules().get(SBBTransitConfigGroup.GROUP_NAME);
		if (!(existing instanceof SBBTransitConfigGroup)) {
			throw new RuntimeException(
				"""
					SBBTransitConfigGroup is not registered in the Config. \
					Please register it before constructing the Controler:

					  ConfigUtils.addOrGetModule(config, SBBTransitConfigGroup.class);

					This must be done before passing the config to ScenarioUtils.createScenario() \
					or new Controler(), so that MATSim's ExplodedConfigModule can bind it for injection."""
			);
		}
		installQSimModule(new SBBTransitEngineQSimModule());
	}
}
