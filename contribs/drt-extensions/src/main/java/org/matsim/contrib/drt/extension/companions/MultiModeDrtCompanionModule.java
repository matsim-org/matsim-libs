/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2022 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.extension.companions;

import com.google.inject.Inject;
import org.matsim.contrib.drt.extension.DrtWithExtensionsConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.core.controler.AbstractModule;

/**
 * @author steffenaxer
 */
public class MultiModeDrtCompanionModule extends AbstractModule {
	@Override
	public void install() {
		MultiModeDrtConfigGroup multiModeDrtCfg = MultiModeDrtConfigGroup.get(getConfig());
		for (DrtConfigGroup drtCfg : multiModeDrtCfg.getModalElements()) {
			if (drtCfg instanceof DrtWithExtensionsConfigGroup drtWithExtensionsConfigGroup && ((DrtWithExtensionsConfigGroup) drtCfg).getDrtCompanionParams().isPresent()) {
				drtWithExtensionsConfigGroup = (DrtWithExtensionsConfigGroup) drtCfg;
				install(new DrtCompanionModule(drtCfg.getMode(), drtWithExtensionsConfigGroup));
			}
		}
	}
}
