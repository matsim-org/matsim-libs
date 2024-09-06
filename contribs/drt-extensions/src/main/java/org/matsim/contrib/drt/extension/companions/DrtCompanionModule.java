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
import org.matsim.contrib.drt.extension.DrtWithExtensionsConfigGroup;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.passenger.PassengerGroupIdentifier;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;


/**
 * This module samples additional drt rides on booked drt trips in order to
 * replicate a more realistic vehicle occupancy.
 *
 * @author Steffen Axer
 */
public class DrtCompanionModule extends AbstractDvrpModeModule {
	final DrtWithExtensionsConfigGroup drtWithExtensionsConfigGroup;

	public DrtCompanionModule(final String mode, final DrtWithExtensionsConfigGroup drtWithExtensionsConfigGroup) {
		super(mode);
		this.drtWithExtensionsConfigGroup = drtWithExtensionsConfigGroup;
	}

	@Override
	public void install() {
		bindModal(DrtCompanionRideGenerator.class).toProvider(
				modalProvider(getter -> new DrtCompanionRideGenerator(
					getMode(), //
					getter.getModal(FleetSpecification.class), //
					getter.get(Scenario.class), //
					this.drtWithExtensionsConfigGroup)))
			.asEagerSingleton();
		addControlerListenerBinding().to(modalKey(DrtCompanionRideGenerator.class));
		installOverridingQSimModule(new AbstractDvrpModeQSimModule(getMode()) {
			@Override
			protected void configureQSim() {
				bindModal(PassengerGroupIdentifier.class).toProvider(
					modalProvider(getter -> new DrtCompanionGroupIdentifier()));
			}
		});
	}
}
