/*
 * Copyright (C) 2022 MOIA GmbH - All Rights Reserved
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package org.matsim.contrib.drt.extension.operations.operationFacilities;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static org.matsim.contrib.common.timeprofile.TimeProfileCollector.ProfileCalculator;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.common.timeprofile.TimeProfileCollector;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.modal.ModalProviders;

import com.google.common.collect.ImmutableMap;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

public class OperationFacilitiesQSimModule extends AbstractDvrpModeQSimModule {

	public static final int DEFAULT_CAPACITY_HORIZON = 30 * 3600;
	public static final int TIME_BIN_SIZE = 300;

	public OperationFacilitiesQSimModule(DrtConfigGroup drtCfg) {
		super(drtCfg.getMode());
	}

	@Override
	protected void configureQSim() {

		bindModal(OperationFacilityFinder.class).toProvider(
						modalProvider(getter -> new NearestOperationFacilityWithCapacityFinder(
								getter.getModal(OperationFacilities.class),
								getter.getModal(OperationFacilityReservationManager.class),
								getter.getModal(Network.class),
								getter.getModal(TravelTime.class),
								getter.getModal(TravelDisutilityFactory.class).createTravelDisutility(getter.getModal(TravelTime.class)),
								getter.get(MobsimTimer.class)
								)))
				.asEagerSingleton();

		bindModal(OperationFacilities.class).toProvider(modalProvider( getter -> {
				OperationFacilitiesSpecification operationFacilitiesSpecification = getter.getModal(OperationFacilitiesSpecification.class);
				ImmutableMap<Id<OperationFacility>, OperationFacility> operationFacilities =
						operationFacilitiesSpecification.getOperationFacilitySpecifications()
						.values()
						.stream()
						.map(spec -> (OperationFacility) new OperationFacilityImpl(spec.getId(), spec.getLinkId(), spec.getCoord(), spec.getCapacity(),
spec.getChargers(), spec.getType()))
						.collect(toImmutableMap(OperationFacility::getId, s -> s));
				return () -> operationFacilities;
			})).asEagerSingleton();

		addModalQSimComponentBinding().toProvider(
				modalProvider(dvrpModeInstanceGetter -> {
					List<OperationFacility> facilitiesList = new ArrayList<>(
							dvrpModeInstanceGetter.getModal(OperationFacilities.class).getFacilities().values());
					var header = facilitiesList.stream().map(f -> f.getId() + "").collect(toImmutableList());

                    ProfileCalculator profileCalculator = () ->
							facilitiesList.stream().collect(
									toImmutableMap(f -> f.getId().toString(),
											f -> (double) f.getRegisteredVehicles().size()
									)
							);
					return new TimeProfileCollector(header, profileCalculator, TIME_BIN_SIZE,
							"individual_operation_facility_capacity_time_profiles" + "_" + getMode(),
							dvrpModeInstanceGetter.get(MatsimServices.class));
				}));
	}
}
