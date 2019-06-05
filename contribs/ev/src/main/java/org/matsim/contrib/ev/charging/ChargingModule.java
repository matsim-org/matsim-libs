/*
 * *********************************************************************** *
 * project: org.matsim.*
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
 * *********************************************************************** *
 */

package org.matsim.contrib.ev.charging;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureProvider;
import org.matsim.core.controler.AbstractModule;

import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.name.Names;

/**
 * @author Michal Maciejewski (michalm)
 */
public class ChargingModule extends AbstractModule {
	@Inject
	private EvConfigGroup evCfg;

	private final Key<Network> networkKey;

	public ChargingModule() {
		this(Key.get(Network.class));
	}

	public ChargingModule(Key<Network> networkKey) {
		this.networkKey = networkKey;
	}

	@Override
	public void install() {
		bind(Network.class).annotatedWith(Names.named(ChargingInfrastructureProvider.CHARGERS))
				.to(networkKey)
				.asEagerSingleton();
		bind(ChargingInfrastructure.class).toProvider(
				new ChargingInfrastructureProvider(evCfg.getChargersFileUrl(getConfig().getContext())))
				.asEagerSingleton();
		bind(ChargingLogic.Factory.class).toInstance(
				charger -> new ChargingWithQueueingLogic(charger, new FixedSpeedChargingStrategy(charger.getPower())));

		bind(ChargingHandler.class).asEagerSingleton();
		addMobsimListenerBinding().to(ChargingHandler.class);
	}
}
