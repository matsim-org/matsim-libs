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
import org.locationtech.jts.geom.prep.PreparedPolygon;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.contrib.common.zones.ZoneSystem;
import org.matsim.contrib.common.zones.ZoneSystemParams;
import org.matsim.contrib.common.zones.ZoneSystemUtils;
import org.matsim.contrib.common.zones.systems.grid.GISFileZoneSystemParams;
import org.matsim.contrib.common.zones.systems.grid.h3.H3GridZoneSystemParams;
import org.matsim.contrib.common.zones.systems.grid.h3.H3ZoneSystem;
import org.matsim.contrib.common.zones.systems.grid.square.SquareGridZoneSystem;
import org.matsim.contrib.common.zones.systems.grid.square.SquareGridZoneSystemParams;
import org.matsim.contrib.drt.analysis.DrtEventSequenceCollector;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.controler.MatsimServices;

import java.util.List;
import java.util.function.Predicate;

import static org.matsim.utils.gis.shp2matsim.ShpGeometryUtils.loadPreparedGeometries;
import static org.matsim.utils.gis.shp2matsim.ShpGeometryUtils.loadPreparedPolygons;

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

			bindModal(ZoneSystem.class).toProvider(modalProvider(getter -> {
				Network network = getter.getModal(Network.class);
				switch (zoneSystemParams.getName()) {
					case GISFileZoneSystemParams.SET_NAME: {
						Preconditions.checkNotNull(((GISFileZoneSystemParams) zoneSystemParams).zonesShapeFile);
						final List<PreparedPolygon> preparedGeometries = loadPreparedPolygons(
							ConfigGroup.getInputFileURL(getConfig().getContext(), ((GISFileZoneSystemParams) zoneSystemParams).zonesShapeFile));
						return ZoneSystemUtils.createFromPreparedGeometries(network,
							EntryStream.of(preparedGeometries).mapKeys(i -> (i + 1) + "").toMap());
					}

					case SquareGridZoneSystemParams.SET_NAME: {
						Preconditions.checkNotNull(((SquareGridZoneSystemParams) zoneSystemParams).cellSize);
						Predicate<Zone> zoneFilter;
						if(drtCfg.operationalScheme == DrtConfigGroup.OperationalScheme.serviceAreaBased) {
							List<PreparedGeometry> serviceAreas = loadPreparedGeometries(ConfigGroup.getInputFileURL(getConfig().getContext(),
								drtCfg.drtServiceAreaShapeFile));
							zoneFilter = zone -> serviceAreas.stream().anyMatch(serviceArea -> serviceArea.intersects(zone.getPreparedGeometry().getGeometry()));
						} else {
							zoneFilter = zone -> true;
						}

						SquareGridZoneSystem squareGridZoneSystem = new SquareGridZoneSystem(network, ((SquareGridZoneSystemParams) zoneSystemParams).cellSize, zoneFilter);
						return squareGridZoneSystem;
					}

					case H3GridZoneSystemParams.SET_NAME: {

						Preconditions.checkNotNull(((H3GridZoneSystemParams) zoneSystemParams).h3Resolution);
						String crs = getConfig().global().getCoordinateSystem();

						Predicate<Zone> zoneFilter;
						if (drtCfg.operationalScheme == DrtConfigGroup.OperationalScheme.serviceAreaBased) {
							List<PreparedGeometry> serviceAreas = loadPreparedGeometries(ConfigGroup.getInputFileURL(getConfig().getContext(),
								drtCfg.drtServiceAreaShapeFile));
							zoneFilter = zone -> serviceAreas.stream().anyMatch(serviceArea -> serviceArea.intersects(zone.getPreparedGeometry().getGeometry()));
						} else {
							zoneFilter = zone -> true;
						}

						return new H3ZoneSystem(crs, ((H3GridZoneSystemParams) zoneSystemParams).h3Resolution, network, zoneFilter);
					}

					default:
						throw new RuntimeException("Unsupported zone generation");
				}
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
