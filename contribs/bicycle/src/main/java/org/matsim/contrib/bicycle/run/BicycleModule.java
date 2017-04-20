/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.contrib.bicycle.run;

import org.matsim.core.controler.AbstractModule;

public class BicycleModule extends AbstractModule {

	@Override
	public void install() {
		addTravelTimeBinding("bike").to(BicycleTravelTime.class);
		addTravelDisutilityFactoryBinding("bike").to(BicycleTravelDisutilityFactory.class);

		// cf. RunMobsimWithMultipleModeVehiclesExample; needed to be able to set maxSpeed und PCU
		bindMobsim().toProvider(BicycleQSimFactory.class);
	}
}