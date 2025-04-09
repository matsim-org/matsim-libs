/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2022 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.optimizer.rebalancing;

import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.contrib.common.zones.ZoneSystem;
import org.matsim.contrib.common.zones.ZoneSystemParams;
import org.matsim.contrib.common.zones.ZoneSystemUtils;
import org.matsim.contrib.drt.analysis.zonal.DrtZoneTargetLinkSelector;
import org.matsim.contrib.drt.analysis.zonal.MostCentralDrtZoneTargetLinkSelector;
import org.matsim.contrib.drt.analysis.zonal.RandomDrtZoneTargetLinkSelector;
import org.matsim.contrib.drt.optimizer.rebalancing.Feedforward.DrtModeFeedforwardRebalanceModule;
import org.matsim.contrib.drt.optimizer.rebalancing.Feedforward.FeedforwardRebalancingStrategyParams;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.DrtModeMinCostFlowRebalancingModule;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingStrategyParams;
import org.matsim.contrib.drt.optimizer.rebalancing.plusOne.DrtModePlusOneRebalanceModule;
import org.matsim.contrib.drt.optimizer.rebalancing.plusOne.PlusOneRebalancingStrategyParams;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.core.config.ConfigGroup;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * @author Michal Maciejewski (michalm)
 */
public class RebalancingModule extends AbstractDvrpModeModule {

	public final static String REBALANCING_ZONE_SYSTEM = "rebalancing";

	private final DrtConfigGroup drtCfg;

	public RebalancingModule(DrtConfigGroup drtCfg) {
		super(drtCfg.getMode());
		this.drtCfg = drtCfg;
	}


	@Override
	public void install() {
		if (drtCfg.getRebalancingParams().isPresent()) {
			RebalancingParams rebalancingParams = drtCfg.getRebalancingParams().get();

			modalMapBinder(String.class, ZoneSystem.class).addBinding(REBALANCING_ZONE_SYSTEM).toProvider(modalProvider(getter -> {
				Network network = getter.getModal(Network.class);
				Predicate<Zone> zoneFilter;
				if(drtCfg.getOperationalScheme() == DrtConfigGroup.OperationalScheme.serviceAreaBased) {
					List<PreparedGeometry> serviceAreaGeoms = ShpGeometryUtils.loadPreparedGeometries(
							ConfigGroup.getInputFileURL(this.getConfig().getContext(), this.drtCfg.getDrtServiceAreaShapeFile()));
					zoneFilter = zone -> serviceAreaGeoms.stream()
							.anyMatch((serviceArea) -> serviceArea.intersects(zone.getPreparedGeometry().getGeometry()));
				} else {
					zoneFilter = zone -> true;
				}
				String crs = getConfig().global().getCoordinateSystem();
				ZoneSystemParams zoneSystemParams = rebalancingParams.getZoneSystemParams();
				return ZoneSystemUtils.createZoneSystem(getConfig().getContext(), network, zoneSystemParams, crs, zoneFilter);
			})).asEagerSingleton();


			bindModal(DrtZoneTargetLinkSelector.class).toProvider(modalProvider(getter -> {
				ZoneSystem zoneSystem = getter.getModal(new TypeLiteral<Map<String, Provider<ZoneSystem>>>() {})
						.get(REBALANCING_ZONE_SYSTEM).get();
				switch (rebalancingParams.getTargetLinkSelection()) {
					case mostCentral:
						return new MostCentralDrtZoneTargetLinkSelector(zoneSystem);
					case random:
						return new RandomDrtZoneTargetLinkSelector(zoneSystem);
					default:
						throw new RuntimeException(
								"Unsupported target link selection = " + rebalancingParams.getTargetLinkSelection());
				}
			})).asEagerSingleton();

			if (rebalancingParams.getRebalancingStrategyParams() instanceof MinCostFlowRebalancingStrategyParams) {
				install(new DrtModeMinCostFlowRebalancingModule(drtCfg));
			} else if (rebalancingParams.getRebalancingStrategyParams() instanceof PlusOneRebalancingStrategyParams) {
				install(new DrtModePlusOneRebalanceModule(drtCfg));
			} else if (rebalancingParams.getRebalancingStrategyParams() instanceof FeedforwardRebalancingStrategyParams) {
				install(new DrtModeFeedforwardRebalanceModule(drtCfg));
			} else if (rebalancingParams.getRebalancingStrategyParams() instanceof CustomRebalancingStrategyParams) {
				// User is responsible for installing custom module
			} else {
				throw new RuntimeException(
						"Unsupported rebalancingStrategyParams: " + rebalancingParams.getRebalancingStrategyParams());
			}
		} else {
			bindModal(RebalancingStrategy.class).to(NoRebalancingStrategy.class).asEagerSingleton();
		}
	}
}
