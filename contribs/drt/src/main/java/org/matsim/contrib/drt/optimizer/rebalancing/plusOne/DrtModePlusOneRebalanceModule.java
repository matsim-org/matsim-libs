/*
 * *********************************************************************** *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.drt.optimizer.rebalancing.plusOne;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.analysis.DrtRequestAnalyzer;
import org.matsim.contrib.drt.analysis.zonal.DrtGridUtils;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalWaitTimesAnalyzer;
import org.matsim.contrib.drt.analysis.zonal.ZonalIdleVehicleXYVisualiser;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.MatsimServices;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;

/**
 * @author michalm, Chengqi Lu
 */
public class DrtModePlusOneRebalanceModule extends AbstractDvrpModeModule {
	private final DrtConfigGroup drtCfg;

	public DrtModePlusOneRebalanceModule(DrtConfigGroup drtCfg) {
		super(drtCfg.getMode());
		this.drtCfg = drtCfg;
	}

	@Override
	public void install() {
		System.out.println("Plus one Rebalancing Algorithm is now being installed!");
		PlusOneRebalancingParams params = drtCfg.getPlusOneRebalancing().orElseThrow();
		bindModal(DrtZonalSystem.class).toProvider(modalProvider(getter -> {

			if (params.getRebalancingZonesGeneration()
					.equals(PlusOneRebalancingParams.RebalancingZoneGeneration.ShapeFile)) {
				final List<PreparedGeometry> preparedGeometries = ShpGeometryUtils
						.loadPreparedGeometries(params.getRebalancingZonesShapeFileURL(getConfig().getContext()));
				Map<String, Geometry> zones = new HashMap<>();
				for (int i = 0; i < preparedGeometries.size(); i++) {
					zones.put("" + (i + 1), preparedGeometries.get(i).getGeometry());
				}
				return new DrtZonalSystem(getter.getModal(Network.class), zones);
			}

			if (drtCfg.getOperationalScheme() == DrtConfigGroup.OperationalScheme.serviceAreaBased) {
				final List<PreparedGeometry> preparedGeometries = ShpGeometryUtils
						.loadPreparedGeometries(drtCfg.getDrtServiceAreaShapeFileURL(getConfig().getContext()));
				Network modalNetwork = getter.getModal(Network.class);
				Map<String, Geometry> zones = DrtGridUtils.createGridFromNetworkWithinServiceArea(modalNetwork,
						params.getCellSize(), preparedGeometries);
				return new DrtZonalSystem(modalNetwork, zones);
			}
			return new DrtZonalSystem(getter.getModal(Network.class), params.getCellSize());
		})).asEagerSingleton();

		installQSimModule(new AbstractDvrpModeQSimModule(getMode()) {
			@Override
			protected void configureQSim() {
				bindModal(RebalancingStrategy.class).toProvider(
						modalProvider(getter -> new PlusOneRebalancingStrategy(getter.getModal(Network.class), params,
								getter.get(EventsManager.class))))
						.asEagerSingleton();
			}
		});

		{
			//this is rather analysis - but depends on DrtZonalSystem so it can not be moved into DrtModeAnalysisModule until DrtZonalSystem at the moment...
			bindModal(ZonalIdleVehicleXYVisualiser.class).
					toProvider(modalProvider(
							getter -> new ZonalIdleVehicleXYVisualiser(getter.get(MatsimServices.class),
									drtCfg.getMode(), getter.getModal(DrtZonalSystem.class)))).asEagerSingleton();
			addControlerListenerBinding().to(modalKey(ZonalIdleVehicleXYVisualiser.class));
			addEventHandlerBinding().to(modalKey(ZonalIdleVehicleXYVisualiser.class));

			bindModal(DrtZonalWaitTimesAnalyzer.class).toProvider(modalProvider(
					getter -> new DrtZonalWaitTimesAnalyzer(drtCfg, getter.getModal(DrtRequestAnalyzer.class),
							getter.getModal(DrtZonalSystem.class)))).asEagerSingleton();
			addControlerListenerBinding().to(modalKey(DrtZonalWaitTimesAnalyzer.class));
			addEventHandlerBinding().to(modalKey(DrtZonalWaitTimesAnalyzer.class));
		}

	}
}
