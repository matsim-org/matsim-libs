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

import static org.matsim.contrib.drt.analysis.zonal.DrtGridUtils.createGridFromNetwork;
import static org.matsim.contrib.drt.analysis.zonal.DrtGridUtils.createGridFromNetworkWithinServiceArea;
import static org.matsim.contrib.drt.run.DrtConfigGroup.OperationalScheme;
import static org.matsim.utils.gis.shp2matsim.ShpGeometryUtils.loadPreparedGeometries;

import java.util.List;

import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.analysis.DrtEventSequenceCollector;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.controler.MatsimServices;

import com.google.common.base.Preconditions;

import one.util.streamex.EntryStream;

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
			Network network = getter.getModal(Network.class);
			switch (params.zonesGeneration) {
				case ShapeFile:
					final List<PreparedGeometry> preparedGeometries = loadPreparedGeometries(
							ConfigGroup.getInputFileURL(getConfig().getContext(), params.zonesShapeFile));
					return DrtZonalSystem.createFromPreparedGeometries(network,
							EntryStream.of(preparedGeometries).mapKeys(i -> (i + 1) + "").toMap());

				case GridFromNetwork:
					Preconditions.checkNotNull(params.cellSize);
					var gridZones = drtCfg.operationalScheme == OperationalScheme.serviceAreaBased ?
							createGridFromNetworkWithinServiceArea(network, params.cellSize,
									loadPreparedGeometries(ConfigGroup.getInputFileURL(getConfig().getContext(),
											drtCfg.drtServiceAreaShapeFile))) :
							createGridFromNetwork(network, params.cellSize);
					return DrtZonalSystem.createFromPreparedGeometries(network, gridZones);

				default:
					throw new RuntimeException("Unsupported zone generation");
			}
		})).asEagerSingleton();

		bindModal(DrtZoneTargetLinkSelector.class).toProvider(modalProvider(getter -> {
			switch (params.targetLinkSelection) {
				case mostCentral:
					return new MostCentralDrtZoneTargetLinkSelector(getter.getModal(DrtZonalSystem.class));
				case random:
					return new RandomDrtZoneTargetLinkSelector();
				default:
					throw new RuntimeException(
							"Unsupported target link selection = " + params.targetLinkSelection);
			}
		})).asEagerSingleton();

		//zonal analysis
		bindModal(ZonalIdleVehicleXYVisualiser.class).toProvider(modalProvider(
				getter -> new ZonalIdleVehicleXYVisualiser(getter.get(MatsimServices.class), drtCfg.getMode(),
						getter.getModal(DrtZonalSystem.class)))).asEagerSingleton();
		addControlerListenerBinding().to(modalKey(ZonalIdleVehicleXYVisualiser.class));
		addEventHandlerBinding().to(modalKey(ZonalIdleVehicleXYVisualiser.class));

		bindModal(DrtZonalWaitTimesAnalyzer.class).toProvider(modalProvider(
				getter -> new DrtZonalWaitTimesAnalyzer(drtCfg, getter.getModal(DrtEventSequenceCollector.class),
						getter.getModal(DrtZonalSystem.class)))).asEagerSingleton();
		addControlerListenerBinding().to(modalKey(DrtZonalWaitTimesAnalyzer.class));
	}
}
