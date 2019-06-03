/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.contrib.ev.dvrp;

import java.util.function.Function;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.router.DvrpRoutingNetworkProvider;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.contrib.dynagent.run.DynActivityEngineModule;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.EvModule;
import org.matsim.contrib.ev.MobsimScopeEventHandling;
import org.matsim.contrib.ev.charging.ChargingModule;
import org.matsim.contrib.ev.charging.ChargingStrategy;
import org.matsim.contrib.ev.charging.ChargingWithQueueingAndAssignmentLogic;
import org.matsim.contrib.ev.discharging.DischargingModule;
import org.matsim.contrib.ev.fleet.ElectricFleetModule;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.stats.EvStatsModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigurator;

import com.google.inject.Key;
import com.google.inject.name.Names;

/**
 * Use this module instead of the default EvModule
 *
 * @author michalm
 */
public class EvDvrpIntegrationModule extends AbstractDvrpModeModule {
	public static QSimComponentsConfigurator activateModes(String... modes) {
		return components -> {
			DynActivityEngineModule.configureComponents(components);
			components.addNamedComponent(EvModule.EV_COMPONENT);
			for (String m : modes) {
				components.addComponent(DvrpModes.mode(m));
			}
		};
	}

	private Function<Charger, ChargingStrategy> chargingStrategyFactory;

	public EvDvrpIntegrationModule(String mode) {
		super(mode);
	}

	@Override
	public void install() {
		EvConfigGroup evCfg = EvConfigGroup.get(getConfig());

		bind(MobsimScopeEventHandling.class).asEagerSingleton();
		addControlerListenerBinding().to(MobsimScopeEventHandling.class);

		install(new ElectricFleetModule(evCfg));

		install(new ChargingModule(evCfg, Key.get(Network.class, Names.named(DvrpRoutingNetworkProvider.DVRP_ROUTING)),
				charger -> new ChargingWithQueueingAndAssignmentLogic(charger,
						chargingStrategyFactory.apply(charger))));

		install(new DischargingModule(evCfg));
		install(new EvStatsModule(evCfg));
	}

	public EvDvrpIntegrationModule setChargingStrategyFactory(
			Function<Charger, ChargingStrategy> chargingStrategyFactory) {
		this.chargingStrategyFactory = chargingStrategyFactory;
		return this;
	}
}
