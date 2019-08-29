
/* *********************************************************************** *
 * project: org.matsim.*
 * TravelDistanceStatsModule.java
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

 package org.matsim.analysis;


import org.matsim.core.controler.AbstractModule;

public class TravelDistanceStatsModule extends AbstractModule {

	@Override
	public void install() {
		bind(TravelDistanceStats.class).asEagerSingleton();
		bind(PKMbyModeCalculator.class).asEagerSingleton();
		addControlerListenerBinding().to(TravelDistanceStatsControlerListener.class);
	}

}
