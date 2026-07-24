/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2025 by the members listed in the COPYING,        *
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
 * *********************************************************************** */

package org.matsim.contrib.drt.optimizer.insertion.parallel;

import org.matsim.contrib.drt.optimizer.DrtRequestInsertionRetryQueue;
import org.matsim.contrib.drt.optimizer.MultiQSimScopeForkJoinPoolHolder;
import org.matsim.contrib.drt.optimizer.QsimScopeForkJoinPool;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.insertion.DrtInsertionSearch;
import org.matsim.contrib.drt.optimizer.insertion.RequestFleetFilter;
import org.matsim.contrib.drt.optimizer.insertion.UnplannedRequestInserter;
import org.matsim.contrib.drt.optimizer.insertion.parallel.partitioner.requests.LoadAwareRoundRobinRequestsPartitioner;
import org.matsim.contrib.drt.optimizer.insertion.parallel.partitioner.requests.RequestsPartitioner;
import org.matsim.contrib.drt.optimizer.insertion.parallel.partitioner.requests.RoundRobinRequestsPartitioner;
import org.matsim.contrib.drt.optimizer.insertion.parallel.partitioner.vehicles.ReplicatingVehicleEntryPartitioner;
import org.matsim.contrib.drt.optimizer.insertion.parallel.partitioner.vehicles.RoundRobinVehicleEntryPartitioner;
import org.matsim.contrib.drt.optimizer.insertion.parallel.partitioner.vehicles.ShiftingRoundRobinVehicleEntryPartitioner;
import org.matsim.contrib.drt.optimizer.insertion.parallel.partitioner.vehicles.VehicleEntryPartitioner;
import org.matsim.contrib.drt.passenger.DrtOfferAcceptor;
import org.matsim.contrib.drt.routing.DrtRouteConstraintsCalculator;
import org.matsim.contrib.drt.routing.DrtStopNetwork;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.scheduler.RequestInsertionScheduler;
import org.matsim.contrib.drt.stops.PassengerStopDurationProvider;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.passenger.DvrpLoadFromTrip;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.api.core.v01.network.Network;

import java.util.Optional;

import static org.matsim.contrib.drt.optimizer.insertion.parallel.partitioner.requests.LoadAwareRoundRobinRequestsPartitioner.getDefaultPartitionScalingFunction;

/**
 * @author Steffen Axer
 */
public class ParallelRequestInserterModule extends AbstractDvrpModeQSimModule {
	DrtConfigGroup drtConfigGroup;

	public ParallelRequestInserterModule(DrtConfigGroup drtConfigGroup) {
		super(drtConfigGroup.getMode());
		this.drtConfigGroup = drtConfigGroup;
	}

	@Override
	protected void configureQSim() {
		Optional<DrtParallelInserterParams> drtParallelInserterParams = drtConfigGroup.getDrtParallelInserterParams();

		if (drtParallelInserterParams.isEmpty()) {
			return;
		}

		// Request Partitioner
		switch (drtParallelInserterParams.get().requestsPartitioner) {
			case RoundRobinRequestsPartitioner:
				bindModal(RequestsPartitioner.class).to(RoundRobinRequestsPartitioner.class).asEagerSingleton();
				break;
			case LoadAwareRoundRobinRequestsPartitioner:
				bindModal(RequestsPartitioner.class).toProvider(() ->
						new LoadAwareRoundRobinRequestsPartitioner(getDefaultPartitionScalingFunction()))
					.asEagerSingleton();
				break;
			default:
				throw new IllegalArgumentException("Unknown request partitioner: " + drtParallelInserterParams.get().requestsPartitioner);
		}

		// Vehicle Partitioner
		switch (drtParallelInserterParams.get().vehiclesPartitioner) {
			case ReplicatingVehicleEntryPartitioner:
				bindModal(VehicleEntryPartitioner.class).to(ReplicatingVehicleEntryPartitioner.class).asEagerSingleton();
				break;
			case RoundRobinVehicleEntryPartitioner:
				bindModal(VehicleEntryPartitioner.class).to(RoundRobinVehicleEntryPartitioner.class).asEagerSingleton();
				break;
			case ShiftingRoundRobinVehicleEntryPartitioner:
				bindModal(VehicleEntryPartitioner.class).to(ShiftingRoundRobinVehicleEntryPartitioner.class).asEagerSingleton();
				break;
			default:
				throw new IllegalArgumentException("Unknown vehicle partitioner: " + drtParallelInserterParams.get().vehiclesPartitioner);
		}


		bindModal(UnplannedRequestInserter.class).toProvider(modalProvider(
			getter -> {
				Optional<DrtServiceQualityProbe> serviceQualityProbe = drtParallelInserterParams.get().isWriteServiceQualityProbes()
					? Optional.of(new DrtServiceQualityProbe(
					getter.get(MatsimServices.class),
					drtConfigGroup.getMode(),
					getter.getModal(DrtStopNetwork.class),
					getter.getModal(Network.class),
					getter.getModal(TravelTime.class),
					getter.get(LeastCostPathCalculatorFactory.class),
					getter.getModal(TravelDisutilityFactory.class),
					getter.getModal(DrtRouteConstraintsCalculator.class),
					getter.getModal(DvrpLoadFromTrip.class),
					getter.getModal(RequestFleetFilter.class),
					() -> getter.getModal(DrtInsertionSearch.class),
					drtParallelInserterParams.get()))
					: Optional.empty();
				return new ParallelUnplannedRequestInserter(
				getter.get(MatsimServices.class),
				getter.getModal(RequestsPartitioner.class),
				getter.getModal(VehicleEntryPartitioner.class),
				drtParallelInserterParams.get(),
				drtConfigGroup.getMode(),
				getter.getModal(Fleet.class),
				getter.get(EventsManager.class),
				() -> getter.getModal(RequestInsertionScheduler.class),
				getter.getModal(VehicleEntry.EntryFactory.class),
				() -> getter.getModal(DrtInsertionSearch.class),
				getter.getModal(DrtOfferAcceptor.class),
				getter.getModal(PassengerStopDurationProvider.class),
				getter.getModal(RequestFleetFilter.class),
				getter.getModal(DrtRequestInsertionRetryQueue.class),
				serviceQualityProbe
			);
			})).asEagerSingleton();
		addModalQSimComponentBinding().to(modalKey(UnplannedRequestInserter.class));

		addModalComponent(QsimScopeForkJoinPool.class,
			() -> new MultiQSimScopeForkJoinPoolHolder(drtParallelInserterParams.get().getInsertionSearchThreadsPerWorker()));
	}


}
