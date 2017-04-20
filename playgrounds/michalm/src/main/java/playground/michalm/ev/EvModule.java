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

package playground.michalm.ev;

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

import playground.michalm.ev.charging.ChargingHandler;
import playground.michalm.ev.data.EvData;
import playground.michalm.ev.discharging.*;

public class EvModule extends AbstractModule {
	private final EvData evData;

	public EvModule(EvData evData) {
		this.evData = evData;
	}

	@Override
	public void install() {
		bind(EvData.class).toInstance(evData);
		bind(DriveDischargingHandler.class).asEagerSingleton();
		addEventHandlerBinding().to(DriveDischargingHandler.class);
		bind(AuxDischargingHandler.class).asEagerSingleton();
		addMobsimListenerBinding().to(AuxDischargingHandler.class);
		bind(ChargingHandler.class).asEagerSingleton();
		addMobsimListenerBinding().to(ChargingHandler.class);

		if (EvConfigGroup.get(getConfig()).getTimeProfiles()) {
			addMobsimListenerBinding().toProvider(SocHistogramTimeProfileCollectorProvider.class);
			addMobsimListenerBinding().toProvider(IndividualSocTimeProfileCollectorProvider.class);
			// add more time profiles if necessary
		}

		addControlerListenerBinding().toInstance(new IterationEndsListener() {
			public void notifyIterationEnds(IterationEndsEvent event) {
				evData.clearQueuesAndResetBatteries();
			}
		});
	}
}
