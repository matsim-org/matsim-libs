/* *********************************************************************** *
 * project: org.matsim.*
 * RunEmissionToolOffline.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.contrib.taxibus.run.configuration;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.taxibus.algorithm.passenger.TaxibusPassengerOrderManager;
import org.matsim.contrib.taxibus.algorithm.utils.TaxibusUtils;
import org.matsim.contrib.taxibus.run.sim.TaxibusQSimProvider;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.data.file.VehicleReader;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.trafficmonitoring.VrpTravelTimeModules;
import org.matsim.contrib.dynagent.run.*;
import org.matsim.core.controler.*;

import com.google.inject.name.*;
import com.google.inject.util.Providers;

/**
 * @author jbischoff
 *
 */
public class ConfigBasedTaxibusLaunchUtils {
	private Controler controler;

	public ConfigBasedTaxibusLaunchUtils(Controler controler) {
		this.controler = controler;

	}

	public void initiateTaxibusses() {
		// this is done exactly once per simulation

		Scenario scenario = controler.getScenario();
		final TaxibusConfigGroup tbcg = (TaxibusConfigGroup)scenario.getConfig().getModules()
				.get(TaxibusConfigGroup.GROUP_NAME);
		final FleetImpl fleetData = new FleetImpl();
		new VehicleReader(scenario.getNetwork(), fleetData)
				.parse(tbcg.getVehiclesFileUrl(scenario.getConfig().getContext()));
		final TaxibusPassengerOrderManager orderManager;

		if ((tbcg.getAlgorithm().equals("clustered_jsprit")) || (tbcg.getAlgorithm().equals("jsprit"))) {
			orderManager = new TaxibusPassengerOrderManager();
		} else {
			orderManager = null;
		}

		controler.addOverridingModule(VrpTravelTimeModules.createTravelTimeEstimatorModule());
		controler.addOverridingModule(new DynQSimModule<>(TaxibusQSimProvider.class));
		controler.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {

				if (orderManager != null) {
					addEventHandlerBinding().toInstance(orderManager);
					bind(TaxibusPassengerOrderManager.class).toInstance(orderManager);
				} else {
					bind(TaxibusPassengerOrderManager.class)
							.toProvider(Providers.<TaxibusPassengerOrderManager> of(null));
				}
				addRoutingModuleBinding(TaxibusUtils.TAXIBUS_MODE)
						.toInstance(new DynRoutingModule(TaxibusUtils.TAXIBUS_MODE));
				bind(Fleet.class).toInstance(fleetData);
				bind(Network.class).annotatedWith(Names.named(DvrpModule.DVRP_ROUTING)).to(Network.class).asEagerSingleton();
			}
		});

	}

}
