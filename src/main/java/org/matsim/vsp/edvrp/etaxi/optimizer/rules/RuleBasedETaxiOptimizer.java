/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.vsp.edvrp.etaxi.optimizer.rules;

import java.util.stream.Stream;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.passenger.PassengerRequestValidator;
import org.matsim.contrib.taxi.optimizer.BestDispatchFinder;
import org.matsim.contrib.taxi.optimizer.BestDispatchFinder.Dispatch;
import org.matsim.contrib.taxi.optimizer.UnplannedRequestInserter;
import org.matsim.contrib.taxi.optimizer.rules.IdleTaxiZonalRegistry;
import org.matsim.contrib.taxi.optimizer.rules.RuleBasedRequestInserter;
import org.matsim.contrib.taxi.optimizer.rules.RuleBasedTaxiOptimizer;
import org.matsim.contrib.taxi.optimizer.rules.UnplannedRequestZonalRegistry;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.schedule.TaxiTask;
import org.matsim.contrib.taxi.schedule.TaxiTask.TaxiTaskType;
import org.matsim.contrib.zone.SquareGridSystem;
import org.matsim.contrib.zone.ZonalSystem;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vsp.edvrp.etaxi.ETaxiChargingTask;
import org.matsim.vsp.edvrp.etaxi.ETaxiScheduler;
import org.matsim.vsp.edvrp.etaxi.optimizer.BestChargerFinder;
import org.matsim.vsp.ev.data.Battery;
import org.matsim.vsp.ev.data.Charger;
import org.matsim.vsp.ev.data.ChargingInfrastructure;
import org.matsim.vsp.ev.dvrp.EvDvrpVehicle;

public class RuleBasedETaxiOptimizer extends RuleBasedTaxiOptimizer {
	public static RuleBasedETaxiOptimizer create(TaxiConfigGroup taxiCfg, Fleet fleet, ETaxiScheduler eScheduler,
			Network network, MobsimTimer timer, TravelTime travelTime, TravelDisutility travelDisutility,
			RuleBasedETaxiOptimizerParams params, ChargingInfrastructure chargingInfrastructure,
			PassengerRequestValidator requestValidator, EventsManager events) {
		return RuleBasedETaxiOptimizer.create(taxiCfg, fleet, eScheduler, network, timer, travelTime, travelDisutility,
				params, chargingInfrastructure, new SquareGridSystem(network, params.cellSize), requestValidator,
				events);
	}

	public static RuleBasedETaxiOptimizer create(TaxiConfigGroup taxiCfg, Fleet fleet, ETaxiScheduler eScheduler,
			Network network, MobsimTimer timer, TravelTime travelTime, TravelDisutility travelDisutility,
			RuleBasedETaxiOptimizerParams params, ChargingInfrastructure chargingInfrastructure,
			ZonalSystem zonalSystem, PassengerRequestValidator requestValidator, EventsManager events) {
		IdleTaxiZonalRegistry idleTaxiRegistry = new IdleTaxiZonalRegistry(zonalSystem, eScheduler);
		UnplannedRequestZonalRegistry unplannedRequestRegistry = new UnplannedRequestZonalRegistry(zonalSystem);
		BestDispatchFinder dispatchFinder = new BestDispatchFinder(eScheduler, network, timer, travelTime,
				travelDisutility);
		RuleBasedRequestInserter requestInserter = new RuleBasedRequestInserter(eScheduler, timer, dispatchFinder,
				params, idleTaxiRegistry, unplannedRequestRegistry);

		return new RuleBasedETaxiOptimizer(taxiCfg, fleet, eScheduler, chargingInfrastructure, params, idleTaxiRegistry,
				unplannedRequestRegistry, dispatchFinder, requestInserter, requestValidator, events);
	}

	// TODO MIN_RELATIVE_SOC should depend on the weather and time of day
	private final RuleBasedETaxiOptimizerParams params;
	private final ChargingInfrastructure chargingInfrastructure;
	private final BestChargerFinder eDispatchFinder;
	private final ETaxiScheduler eScheduler;
	private final IdleTaxiZonalRegistry idleTaxiRegistry;

	public RuleBasedETaxiOptimizer(TaxiConfigGroup taxiCfg, Fleet fleet, ETaxiScheduler eScheduler,
			ChargingInfrastructure chargingInfrastructure, RuleBasedETaxiOptimizerParams params,
			IdleTaxiZonalRegistry idleTaxiRegistry, UnplannedRequestZonalRegistry unplannedRequestRegistry,
			BestDispatchFinder dispatchFinder, UnplannedRequestInserter requestInserter,
			PassengerRequestValidator requestValidator, EventsManager events) {
		super(taxiCfg, fleet, eScheduler, params, idleTaxiRegistry, unplannedRequestRegistry, requestInserter,
				requestValidator, events);
		this.params = params;
		this.chargingInfrastructure = chargingInfrastructure;
		this.eScheduler = eScheduler;
		this.idleTaxiRegistry = idleTaxiRegistry;
		eDispatchFinder = new BestChargerFinder(dispatchFinder);
	}

	@Override
	public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e) {
		if (isNewDecisionEpoch(e, params.socCheckTimeStep)) {
			@SuppressWarnings("unchecked")
			Stream<EvDvrpVehicle> eTaxis = (Stream<EvDvrpVehicle>)(Stream<? extends Vehicle>)idleTaxiRegistry.vehicles();
			chargeIdleUnderchargedVehicles(eTaxis.filter(this::isUndercharged));
		}

		super.notifyMobsimBeforeSimStep(e);
	}

	private void chargeIdleUnderchargedVehicles(Stream<EvDvrpVehicle> vehicles) {
		vehicles.forEach(v -> {
			Dispatch<Charger> eDispatch = eDispatchFinder.findBestChargerForVehicle(v,
					chargingInfrastructure.getChargers().values().stream());
			eScheduler.scheduleCharging(v, v.getElectricVehicle(), eDispatch.destination, eDispatch.path);
		});
	}

	@Override
	public void nextTask(Vehicle vehicle) {
		super.nextTask(vehicle);

		if (eScheduler.isIdle(vehicle)) {
			EvDvrpVehicle eTaxi = (EvDvrpVehicle)vehicle;
			if (isUndercharged(eTaxi)) {
				chargeIdleUnderchargedVehicles(Stream.of(eTaxi));
			}
		}
	}

	@Override
	protected boolean isWaitStay(TaxiTask task) {
		return task.getTaxiTaskType() == TaxiTaskType.STAY && !(task instanceof ETaxiChargingTask);
	}

	private boolean isUndercharged(EvDvrpVehicle v) {
		Battery b = v.getElectricVehicle().getBattery();
		return b.getSoc() < params.minRelativeSoc * b.getCapacity();
	}
}
