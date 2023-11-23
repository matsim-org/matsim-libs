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

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.extension.DrtWithExtensionsConfigGroup;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.core.router.MainModeIdentifier;

/**
 * This module samples additional drt rides on booked drt trips in order to
 * replicate a more realistic vehicle occupancy.
 *
 * @author Steffen Axer
 *
 */
public class DrtCompanionModule extends AbstractDvrpModeModule {
    DrtWithExtensionsConfigGroup drtWithExtensionsConfigGroup;

	public DrtCompanionModule(String mode, DrtWithExtensionsConfigGroup drtWithExtensionsConfigGroup) {
		super(mode);
		this.drtWithExtensionsConfigGroup = drtWithExtensionsConfigGroup;

	}

	@Override
	public void install() {
		bindModal(DrtCompanionRideGenerator.class).toProvider(
				modalProvider(getter -> new DrtCompanionRideGenerator(
						getMode(), //
						getter.get(MainModeIdentifier.class), //
						getter.get(Scenario.class), //
						getter.getModal(Network.class),  //
						this.drtWithExtensionsConfigGroup)))
				.asEagerSingleton();
		addControlerListenerBinding().to(modalKey(DrtCompanionRideGenerator.class));
	}
}
