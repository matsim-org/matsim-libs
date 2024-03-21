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

import com.google.common.base.Preconditions;
import one.util.streamex.EntryStream;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.common.zones.ZoneSystem;
import org.matsim.contrib.common.zones.ZoneSystemUtils;
import org.matsim.contrib.common.zones.h3.H3GridUtils;
import org.matsim.contrib.common.zones.h3.H3ZoneSystemUtils;
import org.matsim.contrib.drt.analysis.DrtEventSequenceCollector;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.controler.MatsimServices;

import java.util.List;
import java.util.Map;

import static org.matsim.contrib.drt.analysis.zonal.DrtGridUtils.createGridFromNetwork;
import static org.matsim.contrib.drt.analysis.zonal.DrtGridUtils.filterGridWithinServiceArea;
import static org.matsim.utils.gis.shp2matsim.ShpGeometryUtils.loadPreparedGeometries;

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
			DrtZonalSystemParams params = drtCfg.getZonalSystemParams().get();

			bindModal(ZoneSystem.class).toProvider(modalProvider(getter -> {
				Network network = getter.getModal(Network.class);
				switch (params.zonesGeneration) {
					case ShapeFile: {
						final List<PreparedGeometry> preparedGeometries = loadPreparedGeometries(
							ConfigGroup.getInputFileURL(getConfig().getContext(), params.zonesShapeFile));
						return ZoneSystemUtils.createFromPreparedGeometries(network,
							EntryStream.of(preparedGeometries).mapKeys(i -> (i + 1) + "").toMap());
					}

					case GridFromNetwork: {
						Preconditions.checkNotNull(params.cellSize);
						Map<String, PreparedGeometry> gridFromNetwork = createGridFromNetwork(network, params.cellSize);
						var gridZones =
							switch (drtCfg.operationalScheme) {
								case stopbased, door2door -> gridFromNetwork;
								case serviceAreaBased -> filterGridWithinServiceArea(gridFromNetwork,
									loadPreparedGeometries(ConfigGroup.getInputFileURL(getConfig().getContext(),
										drtCfg.drtServiceAreaShapeFile)));
							};
						return ZoneSystemUtils.createFromPreparedGeometries(network, gridZones);
					}

					case H3:
						Preconditions.checkNotNull(params.h3Resolution);
						String crs = getConfig().global().getCoordinateSystem();
						Map<String, PreparedGeometry> gridFromNetwork = H3GridUtils.createH3GridFromNetwork(network, params.h3Resolution, crs);
						var gridZones =
							switch (drtCfg.operationalScheme) {
								case stopbased, door2door -> gridFromNetwork;
								case serviceAreaBased -> filterGridWithinServiceArea(gridFromNetwork,
									loadPreparedGeometries(ConfigGroup.getInputFileURL(getConfig().getContext(),
										drtCfg.drtServiceAreaShapeFile)));
							};
						return H3ZoneSystemUtils.createFromPreparedGeometries(network, gridZones, crs, params.h3Resolution);

					default:
						throw new RuntimeException("Unsupported zone generation");
				}
			})).asEagerSingleton();

			bindModal(DrtZoneTargetLinkSelector.class).toProvider(modalProvider(getter -> {
				switch (params.targetLinkSelection) {
					case mostCentral:
						return new MostCentralDrtZoneTargetLinkSelector(getter.getModal(ZoneSystem.class));
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
