/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.analysis.zonal;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.common.zones.ZoneSystem;
import org.matsim.contrib.common.zones.ZoneSystemParams;
import org.matsim.contrib.common.zones.ZoneSystemUtils;
import org.matsim.contrib.drt.analysis.DrtEventSequenceCollector;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.core.controler.MatsimServices;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DrtModeZonalSystemModule extends AbstractDvrpModeModule {

	private final DrtConfigGroup drtCfg;

	public DrtModeZonalSystemModule(DrtConfigGroup drtCfg) {
		super(drtCfg.getMode());
		this.drtCfg = drtCfg;
	}

	@Override
	public void install() {
		if (drtCfg.getZonalSystemParams().isPresent()) {
			DrtZoneSystemParams params = drtCfg.getZonalSystemParams().get();
			ZoneSystemParams zoneSystemParams = params.getZoneSystemParams();
			String crs = getConfig().global().getCoordinateSystem();

			bindModal(ZoneSystem.class).toProvider(modalProvider(getter -> {
				Network network = getter.getModal(Network.class);
                return ZoneSystemUtils.createZoneSystem(getConfig().getContext(), network, zoneSystemParams, crs);
			})).asEagerSingleton();

			bindModal(DrtZoneTargetLinkSelector.class).toProvider(modalProvider(getter -> {
				switch (params.targetLinkSelection) {
					case mostCentral:
						return new MostCentralDrtZoneTargetLinkSelector(getter.getModal(ZoneSystem.class));
					case random:
						return new RandomDrtZoneTargetLinkSelector(getter.getModal(ZoneSystem.class));
					default:
						throw new RuntimeException(
							"Unsupported target link selection = " + params.targetLinkSelection);
				}
			})).asEagerSingleton();

			//zonal analysis
			bindModal(ZonalIdleVehicleXYVisualiser.class).toProvider(modalProvider(
				getter -> new ZonalIdleVehicleXYVisualiser(getter.get(MatsimServices.class), drtCfg.getMode(),
					getter.getModal(ZoneSystem.class)))).asEagerSingleton();
			addControlerListenerBinding().to(modalKey(ZonalIdleVehicleXYVisualiser.class));
			addEventHandlerBinding().to(modalKey(ZonalIdleVehicleXYVisualiser.class));

			bindModal(DrtZonalWaitTimesAnalyzer.class).toProvider(modalProvider(
				getter -> new DrtZonalWaitTimesAnalyzer(drtCfg, getter.getModal(DrtEventSequenceCollector.class),
					getter.getModal(ZoneSystem.class)))).asEagerSingleton();
			addControlerListenerBinding().to(modalKey(DrtZonalWaitTimesAnalyzer.class));
		}
	}
}
