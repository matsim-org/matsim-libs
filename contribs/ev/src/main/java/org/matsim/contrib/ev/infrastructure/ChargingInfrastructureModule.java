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

package org.matsim.contrib.ev.infrastructure;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.charging.ChargingLogic;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;

import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

/**
 * @author Michal Maciejewski (michalm)
 */
public final class ChargingInfrastructureModule extends AbstractModule {
	public static final String CHARGERS = "chargers";
	private final Key<Network> networkKey;

	@Inject
	private EvConfigGroup evCfg;

	public ChargingInfrastructureModule() {
		this(Key.get(Network.class));
	}

	public ChargingInfrastructureModule(Key<Network> networkKey) {
		this.networkKey = networkKey;
	}

	@Override
	public void install() {
		bind(Network.class).annotatedWith(Names.named(CHARGERS)).to(networkKey).asEagerSingleton();

		bind(ChargingInfrastructureSpecification.class).toProvider(() -> {
			ChargingInfrastructureSpecification chargingInfrastructureSpecification = new ChargingInfrastructureSpecificationDefaultImpl();
			new ChargerReader(chargingInfrastructureSpecification).parse(
					ConfigGroup.getInputFileURL(getConfig().getContext(), evCfg.chargersFile));
			return chargingInfrastructureSpecification;
		}).asEagerSingleton();

		installQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				bind(ChargingInfrastructure.class).toProvider(new Provider<>() {
					@Inject
					@Named(CHARGERS)
					private Network network;
					@Inject
					private ChargingInfrastructureSpecification chargingInfrastructureSpecification;
					@Inject
					private ChargingLogic.Factory chargingLogicFactory;

					@Override
					public ChargingInfrastructure get() {
						return ChargingInfrastructureUtils.createChargingInfrastructure(chargingInfrastructureSpecification,
								network.getLinks()::get, chargingLogicFactory );
					}
				}).asEagerSingleton();
			}
		});
	}
}
