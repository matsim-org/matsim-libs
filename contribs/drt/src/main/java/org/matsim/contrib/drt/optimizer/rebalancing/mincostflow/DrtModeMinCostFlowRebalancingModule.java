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

package org.matsim.contrib.drt.optimizer.rebalancing.mincostflow;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.drt.analysis.DrtRequestAnalyzer;
import org.matsim.contrib.drt.analysis.zonal.DrtGridUtils;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalWaitTimesAnalyzer;
import org.matsim.contrib.drt.analysis.zonal.EqualVehicleDensityZonalDemandAggregator;
import org.matsim.contrib.drt.analysis.zonal.FleetSizeWeightedByPopulationShareDemandAggregator;
import org.matsim.contrib.drt.analysis.zonal.PreviousIterationZonalDRTDemandAggregator;
import org.matsim.contrib.drt.analysis.zonal.TimeDependentActivityBasedZonalDemandAggregator;
import org.matsim.contrib.drt.analysis.zonal.ZonalDemandAggregator;
import org.matsim.contrib.drt.analysis.zonal.ZonalIdleVehicleXYVisualiser;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingStrategy.RebalancingTargetCalculator;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.core.controler.MatsimServices;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;

/**
 * @author michalm
 */
public class DrtModeMinCostFlowRebalancingModule extends AbstractDvrpModeModule {
	private final DrtConfigGroup drtCfg;

	public DrtModeMinCostFlowRebalancingModule(DrtConfigGroup drtCfg) {
		super(drtCfg.getMode());
		this.drtCfg = drtCfg;
	}

	@Override
	public void install() {
		MinCostFlowRebalancingParams params = drtCfg.getMinCostFlowRebalancing().orElseThrow();
		bindModal(DrtZonalSystem.class).toProvider(modalProvider(getter -> {

			if (params.getRebalancingZonesGeneration()
					.equals(MinCostFlowRebalancingParams.RebalancingZoneGeneration.ShapeFile)) {
				final List<PreparedGeometry> preparedGeometries = ShpGeometryUtils.loadPreparedGeometries(
						params.getRebalancingZonesShapeFileURL(getConfig().getContext()));
				Map<String, Geometry> zones = new HashMap<>();
				for (int i = 0; i < preparedGeometries.size(); i++) {
					zones.put("" + (i + 1), preparedGeometries.get(i).getGeometry());
				}
				return new DrtZonalSystem(getter.getModal(Network.class), zones);
			}

			if (drtCfg.getOperationalScheme() == DrtConfigGroup.OperationalScheme.serviceAreaBased) {
				final List<PreparedGeometry> preparedGeometries = ShpGeometryUtils.loadPreparedGeometries(
						drtCfg.getDrtServiceAreaShapeFileURL(getConfig().getContext()));
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
				bindModal(RebalancingStrategy.class).toProvider(modalProvider(
						getter -> new MinCostFlowRebalancingStrategy(getter.getModal(RebalancingTargetCalculator.class),
								getter.getModal(DrtZonalSystem.class), getter.getModal(Fleet.class),
								getter.getModal(MinCostRelocationCalculator.class), params))).asEagerSingleton();

				bindModal(RebalancingTargetCalculator.class).toProvider(modalProvider(
						getter -> new LinearRebalancingTargetCalculator(getter.getModal(ZonalDemandAggregator.class),
								params))).asEagerSingleton();

				bindModal(MinCostRelocationCalculator.class).toProvider(modalProvider(
						getter -> new AggregatedMinCostRelocationCalculator(getter.getModal(DrtZonalSystem.class),
								getter.getModal(Network.class)))).asEagerSingleton();
			}
		});

		switch (params.getZonalDemandAggregatorType()) {
			case PreviousIteration:
				bindModal(PreviousIterationZonalDRTDemandAggregator.class).toProvider(modalProvider(
						getter -> new PreviousIterationZonalDRTDemandAggregator(getter.getModal(DrtZonalSystem.class),
								drtCfg))).asEagerSingleton();
				bindModal(ZonalDemandAggregator.class).to(modalKey(PreviousIterationZonalDRTDemandAggregator.class));
				addEventHandlerBinding().to(modalKey(PreviousIterationZonalDRTDemandAggregator.class));
				break;
			case TimeDependentActivityBased:
				bindModal(TimeDependentActivityBasedZonalDemandAggregator.class).toProvider(modalProvider(
						getter -> new TimeDependentActivityBasedZonalDemandAggregator(
								getter.getModal(DrtZonalSystem.class), drtCfg))).asEagerSingleton();
				bindModal(ZonalDemandAggregator.class).to(
						modalKey(TimeDependentActivityBasedZonalDemandAggregator.class));
				addEventHandlerBinding().to(modalKey(TimeDependentActivityBasedZonalDemandAggregator.class));
				break;
			case EqualVehicleDensity:
				bindModal(ZonalDemandAggregator.class).toProvider(modalProvider(
						getter -> new EqualVehicleDensityZonalDemandAggregator(getter.getModal(DrtZonalSystem.class),
								getter.getModal(FleetSpecification.class)))).asEagerSingleton();
				break;
			case FleetSizeWeightedByPopulationShare:
				bindModal(ZonalDemandAggregator.class).toProvider(modalProvider(
						getter -> new FleetSizeWeightedByPopulationShareDemandAggregator(getter.getModal(DrtZonalSystem.class),
								getter.get(Population.class), getter.getModal(FleetSpecification.class)))).asEagerSingleton();
				break;
			default:
				throw new IllegalArgumentException("do not know what to do with ZonalDemandAggregatorType="
						+ params.getZonalDemandAggregatorType());
		}

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
