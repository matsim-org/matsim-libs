/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.ev;

import com.google.inject.Singleton;
import org.matsim.contrib.ev.analysis.EvAnalysisModule;
import org.matsim.contrib.ev.charging.ChargingActivityEngine;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;

public class EvModule extends AbstractModule {

	public static final String EV_COMPONENT = "EV_COMPONENT";

	@Override
	public void install() {
		install(new EvBaseModule());
		install(new EvAnalysisModule());

		// this is not for DynVehicles.  Does that mean that we cannot combine charging for normal vehicles with charging for eTaxis?  Can't say ...  kai, dec'22
		installQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				bind(ChargingActivityEngine.class).in(Singleton.class);
				addQSimComponentBinding(EvModule.EV_COMPONENT).to(ChargingActivityEngine.class);
			}
		});

	}
}
