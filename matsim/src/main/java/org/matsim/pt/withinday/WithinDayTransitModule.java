/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * WithinDayTransitModule.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2018 by the members listed in the COPYING, *
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

package org.matsim.pt.withinday;

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.withinday.mobsim.MobsimDataProvider;

public class WithinDayTransitModule extends AbstractModule {

	private LegRerouter rerouter;
	
	public WithinDayTransitModule(LegRerouter rerouter) {
		this.rerouter = rerouter;
	}

	@Override
	public void install() {
		bind(SimpleDisruptionEngine.class);
		bind(WithinDayTransitEngine.class);
        bind(MobsimDataProvider.class).asEagerSingleton();
        bind(Mobsim.class).toProvider(WithinDayTransitQSimFactory.class);
        bind(LegRerouter.class).toInstance(rerouter);
	}

}
