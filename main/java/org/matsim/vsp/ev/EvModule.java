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

package org.matsim.vsp.ev;

import javax.inject.Inject;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.vsp.ev.charging.ChargingHandler;
import org.matsim.vsp.ev.charging.ChargingLogic;
import org.matsim.vsp.ev.charging.ChargingLogic.Factory;
import org.matsim.vsp.ev.charging.ChargingWithQueueingLogic;
import org.matsim.vsp.ev.charging.FixedSpeedChargingStrategy;
import org.matsim.vsp.ev.data.Charger;
import org.matsim.vsp.ev.data.ChargingInfrastructure;
import org.matsim.vsp.ev.data.EvFleet;
import org.matsim.vsp.ev.data.file.ChargingInfrastructureProvider;
import org.matsim.vsp.ev.discharging.AuxDischargingHandler;
import org.matsim.vsp.ev.discharging.DriveDischargingHandler;
import org.matsim.vsp.ev.stats.IndividualSocTimeProfileCollectorProvider;
import org.matsim.vsp.ev.stats.SocHistogramTimeProfileCollectorProvider;

import com.google.inject.name.Names;

public class EvModule extends AbstractModule {
	private static Factory DEFAULT_CHARGING_LOGIC_FACTORY = new Factory() {
		@Override
		public ChargingLogic create(Charger charger) {
			return new ChargingWithQueueingLogic(charger, new FixedSpeedChargingStrategy(charger.getPower()));
		}
	};

	private final EvFleet evFleet;

	public EvModule(EvFleet evFleet) {
		this.evFleet = evFleet;
	}

	public EvModule() {
		this(null);
	}

	@Override
	public void install() {
		EvConfigGroup evCfg = EvConfigGroup.get(getConfig());

		if (evFleet != null) {
			bind(EvFleet.class).toInstance(evFleet);
		}

		bind(Network.class).annotatedWith(Names.named(ChargingInfrastructure.CHARGERS)).to(Network.class)
				.asEagerSingleton();
		bind(ChargingInfrastructure.class)
				.toProvider(new ChargingInfrastructureProvider(evCfg.getChargersFileUrl(getConfig().getContext())))
				.asEagerSingleton();
		bind(ChargingLogic.Factory.class).toInstance(DEFAULT_CHARGING_LOGIC_FACTORY);

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

		bind(InitAtIterationStart.class).asEagerSingleton();
		addControlerListenerBinding().to(InitAtIterationStart.class);
	}

	static class InitAtIterationStart implements IterationStartsListener {
		private final EvFleet evFleet;
		private final ChargingInfrastructure chargingInfrastructure;
		private final ChargingLogic.Factory logicFactory;
		private final EventsManager eventsManager;

		@Inject
		InitAtIterationStart(EvFleet evFleet, ChargingInfrastructure chargingInfrastructure,
				ChargingLogic.Factory logicFactory, EventsManager eventsManager) {
			this.evFleet = evFleet;
			this.chargingInfrastructure = chargingInfrastructure;
			this.logicFactory = logicFactory;
			this.eventsManager = eventsManager;
		}

		@Override
		public void notifyIterationStarts(IterationStartsEvent event) {
			evFleet.resetBatteries();
			chargingInfrastructure.initChargingLogics(logicFactory, eventsManager);
		}
	}
}
