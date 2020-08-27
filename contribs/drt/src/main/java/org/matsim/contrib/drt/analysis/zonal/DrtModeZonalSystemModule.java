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

import static org.matsim.contrib.drt.analysis.zonal.DrtZonalSystemParams.ZoneGeneration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.analysis.DrtRequestAnalyzer;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.core.controler.MatsimServices;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;

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
		DrtZonalSystemParams params = drtCfg.getZonalSystemParams().orElseThrow();

		bindModal(DrtZonalSystem.class).toProvider(modalProvider(getter -> {
			if (params.getZonesGeneration().equals(ZoneGeneration.ShapeFile)) {
				final List<PreparedGeometry> preparedGeometries = ShpGeometryUtils.loadPreparedGeometries(
						params.getRebalancingZonesShapeFileURL(getConfig().getContext()));
				Map<String, Geometry> zones = new HashMap<>();
				for (int i = 0; i < preparedGeometries.size(); i++) {
					zones.put("" + (i + 1), preparedGeometries.get(i).getGeometry());
				}
				return new DrtZonalSystem(getter.getModal(Network.class), zones);
			} else if (drtCfg.getOperationalScheme() == DrtConfigGroup.OperationalScheme.serviceAreaBased) {
				final List<PreparedGeometry> preparedGeometries = ShpGeometryUtils.loadPreparedGeometries(
						drtCfg.getDrtServiceAreaShapeFileURL(getConfig().getContext()));
				Network modalNetwork = getter.getModal(Network.class);
				Map<String, Geometry> zones = DrtGridUtils.createGridFromNetworkWithinServiceArea(modalNetwork,
						params.getCellSize(), preparedGeometries);
				return new DrtZonalSystem(modalNetwork, zones);
			} else {
				return new DrtZonalSystem(getter.getModal(Network.class), params.getCellSize());
			}
		})).asEagerSingleton();

		//zonal analysis
		bindModal(ZonalIdleVehicleXYVisualiser.class).
				toProvider(modalProvider(
						getter -> new ZonalIdleVehicleXYVisualiser(getter.get(MatsimServices.class), drtCfg.getMode(),
								getter.getModal(DrtZonalSystem.class)))).asEagerSingleton();
		addControlerListenerBinding().to(modalKey(ZonalIdleVehicleXYVisualiser.class));
		addEventHandlerBinding().to(modalKey(ZonalIdleVehicleXYVisualiser.class));

		bindModal(DrtZonalWaitTimesAnalyzer.class).toProvider(modalProvider(
				getter -> new DrtZonalWaitTimesAnalyzer(drtCfg, getter.getModal(DrtRequestAnalyzer.class),
						getter.getModal(DrtZonalSystem.class)))).asEagerSingleton();
		addControlerListenerBinding().to(modalKey(DrtZonalWaitTimesAnalyzer.class));
		addEventHandlerBinding().to(modalKey(DrtZonalWaitTimesAnalyzer.class));
	}
}
