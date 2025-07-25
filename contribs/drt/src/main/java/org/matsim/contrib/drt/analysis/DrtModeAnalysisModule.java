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

package org.matsim.contrib.drt.analysis;

import java.awt.Color;
import java.awt.Paint;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.contrib.common.zones.ZoneSystem;
import org.matsim.contrib.common.zones.ZoneSystemParams;
import org.matsim.contrib.common.zones.ZoneSystemUtils;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalWaitTimesAnalyzer;
import org.matsim.contrib.drt.analysis.zonal.ZonalIdleVehicleXYVisualiser;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DefaultDrtStopTask;
import org.matsim.contrib.drt.schedule.DrtDriveTask;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.scheduler.EmptyVehicleRelocator;
import org.matsim.contrib.drt.sharingmetrics.SharingMetricsModule;
import org.matsim.contrib.dvrp.analysis.CapacityLoadAnalysisHandler;
import org.matsim.contrib.dvrp.analysis.ExecutedScheduleCollector;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.load.DvrpLoadType;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.common.timeprofile.ProfileWriter;
import org.matsim.contrib.dvrp.analysis.VehicleOccupancyProfileCalculator;
import org.matsim.contrib.dvrp.analysis.VehicleOccupancyProfileView;
import org.matsim.contrib.dvrp.analysis.VehicleTaskProfileCalculator;
import org.matsim.contrib.dvrp.analysis.VehicleTaskProfileView;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;

/**
 * @author michalm (Michal Maciejewski)
 */
public class DrtModeAnalysisModule extends AbstractDvrpModeModule {
	private final DrtConfigGroup drtCfg;
	private ImmutableSet<Task.TaskType> passengerServingTaskTypes = ImmutableSet.of(DrtDriveTask.TYPE, DefaultDrtStopTask.TYPE);

	public final static String ANALYSIS_ZONE_SYSTEM = "analysis";

	private static final Comparator<Task.TaskType> taskTypeComparator = Comparator.comparing(type -> {
		//we want the following order on the plot: STAY, RELOCATE, other
		if (type.equals(DrtStayTask.TYPE)) {
			return "B";
		} else if (type.equals(EmptyVehicleRelocator.RELOCATE_VEHICLE_TASK_TYPE)) {
			return "A";
		} else {
			return "C" + type.name();
		}
	});

	private static final Comparator<Task.TaskType> nonPassengerTaskTypeComparator = Comparator.comparing(type -> {
		//we want the following order on the plot: STAY, RELOCATE, other
		if (type.equals(DrtStayTask.TYPE)) {
			return "C";
		} else if (type.equals(EmptyVehicleRelocator.RELOCATE_VEHICLE_TASK_TYPE)) {
			return "B";
		} else {
			return "A" + type.name();
		}
	});

	private static final Map<Task.TaskType, Paint> taskTypePaints = ImmutableMap.of(DrtStayTask.TYPE, Color.LIGHT_GRAY);

	public DrtModeAnalysisModule(DrtConfigGroup drtCfg) {
		super(drtCfg.getMode());
		this.drtCfg = drtCfg;
	}

	public DrtModeAnalysisModule(DrtConfigGroup drtCfg, ImmutableSet<Task.TaskType> passengerServingTaskTypes) {
		super(drtCfg.getMode());
		this.drtCfg = drtCfg;
		this.passengerServingTaskTypes = passengerServingTaskTypes;
	}

	@Override
	public void install() {
		bindModal(ExecutedScheduleCollector.class).toProvider(modalProvider(getter -> new ExecutedScheduleCollector(getMode()))).asEagerSingleton();
		addEventHandlerBinding().to(modalKey(ExecutedScheduleCollector.class));

		bindModal(DrtVehicleDistanceStats.class).toProvider(
						modalProvider(getter -> new DrtVehicleDistanceStats(getter.get(Network.class), drtCfg, getter.getModal(FleetSpecification.class), getter.getModal(DvrpLoadType.class))))
				.asEagerSingleton();
		addEventHandlerBinding().to(modalKey(DrtVehicleDistanceStats.class));

		bindModal(DrtEventSequenceCollector.class).toProvider(modalProvider(getter -> new DrtEventSequenceCollector(drtCfg.getMode())))
				.asEagerSingleton();
		addEventHandlerBinding().to(modalKey(DrtEventSequenceCollector.class));

		bindModal(VehicleOccupancyProfileCalculator.class).toProvider(modalProvider(
				getter -> new VehicleOccupancyProfileCalculator(getMode(), getter.getModal(FleetSpecification.class), 300,
						getter.get(QSimConfigGroup.class), passengerServingTaskTypes, getter.getModal(DvrpLoadType.class)))).asEagerSingleton();
		addEventHandlerBinding().to(modalKey(VehicleOccupancyProfileCalculator.class));

		addControllerListenerBinding().toProvider(modalProvider(getter -> {
			MatsimServices matsimServices = getter.get(MatsimServices.class);
			String mode = drtCfg.getMode();
			var profileView = new VehicleOccupancyProfileView(getter.getModal(VehicleOccupancyProfileCalculator.class),
					nonPassengerTaskTypeComparator, taskTypePaints);
			return new ProfileWriter(matsimServices, mode, profileView, "occupancy_time_profiles");
		}));

		bindModal(VehicleTaskProfileCalculator.class).toProvider(modalProvider(
				getter -> new VehicleTaskProfileCalculator(getMode(), getter.getModal(FleetSpecification.class), 300,
						getter.get(QSimConfigGroup.class)))).asEagerSingleton();
		addEventHandlerBinding().to(modalKey(VehicleTaskProfileCalculator.class));

		addControllerListenerBinding().toProvider(modalProvider(getter -> {
			MatsimServices matsimServices = getter.get(MatsimServices.class);
			String mode = drtCfg.getMode();
			var profileView = new VehicleTaskProfileView(getter.getModal(VehicleTaskProfileCalculator.class), taskTypeComparator, taskTypePaints);
			return new ProfileWriter(matsimServices, mode, profileView, "task_time_profiles");
		}));

		addControllerListenerBinding().toProvider(modalProvider(
						getter -> new DrtAnalysisControlerListener(getter.get(Config.class), drtCfg, getter.getModal(FleetSpecification.class),
								getter.getModal(DrtVehicleDistanceStats.class), getter.get(MatsimServices.class), getter.get(Network.class),
								getter.getModal(DrtEventSequenceCollector.class), getter.getModal(VehicleOccupancyProfileCalculator.class), getter.getModal(DvrpLoadType.class))))
				.asEagerSingleton();

		install(new SharingMetricsModule(drtCfg));

		addControllerListenerBinding().toProvider(modalProvider( //
			getter -> new CapacityLoadAnalysisHandler(getMode(), //
			getter.getModal(FleetSpecification.class), //
			getter.get(OutputDirectoryHierarchy.class), //
			getter.get(EventsManager.class), //
                    drtCfg.addOrGetLoadParams().getAnalysisInterval(), //
			getter.getModal(DvrpLoadType.class))));


		modalMapBinder(String.class, ZoneSystem.class).addBinding(ANALYSIS_ZONE_SYSTEM).toProvider(modalProvider(getter -> {
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
			ZoneSystemParams zoneSystemParams = drtCfg.addOrGetAnalysisZoneSystemParams();
			return ZoneSystemUtils.createZoneSystem(getConfig().getContext(), network, zoneSystemParams, crs, zoneFilter);
		})).asEagerSingleton();

		//zonal analysis
		bindModal(ZonalIdleVehicleXYVisualiser.class).toProvider(modalProvider(
				getter -> {
					ZoneSystem zoneSystem = getter.getModal(new TypeLiteral<Map<String, Provider<ZoneSystem>>>() {})
							.get(ANALYSIS_ZONE_SYSTEM).get();
					return new ZonalIdleVehicleXYVisualiser(getter.get(MatsimServices.class), drtCfg.getMode(),
						zoneSystem);
				})).asEagerSingleton();
		addControllerListenerBinding().to(modalKey(ZonalIdleVehicleXYVisualiser.class));
		addEventHandlerBinding().to(modalKey(ZonalIdleVehicleXYVisualiser.class));

		bindModal(DrtZonalWaitTimesAnalyzer.class).toProvider(modalProvider(
				getter -> {
					ZoneSystem zoneSystem = getter.getModal(new TypeLiteral<Map<String, Provider<ZoneSystem>>>() {})
							.get(ANALYSIS_ZONE_SYSTEM).get();
					return new DrtZonalWaitTimesAnalyzer(drtCfg, getter.getModal(DrtEventSequenceCollector.class),
						zoneSystem, getConfig().global().getDefaultDelimiter());
				})).asEagerSingleton();
		addControllerListenerBinding().to(modalKey(DrtZonalWaitTimesAnalyzer.class));
	}
}
