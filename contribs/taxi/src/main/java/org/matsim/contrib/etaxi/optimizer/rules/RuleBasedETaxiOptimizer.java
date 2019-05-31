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

package org.matsim.contrib.etaxi.optimizer.rules;

import java.util.stream.Stream;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.etaxi.ETaxiChargingTask;
import org.matsim.contrib.etaxi.ETaxiScheduler;
import org.matsim.contrib.etaxi.optimizer.BestChargerFinder;
import org.matsim.contrib.ev.data.Charger;
import org.matsim.contrib.ev.data.ChargingInfrastructure;
import org.matsim.contrib.ev.dvrp.EvDvrpVehicle;
import org.matsim.contrib.ev.fleet.Battery;
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

public class RuleBasedETaxiOptimizer extends RuleBasedTaxiOptimizer {
	public static RuleBasedETaxiOptimizer create(EventsManager eventsManager, TaxiConfigGroup taxiCfg, Fleet fleet,
			ETaxiScheduler eScheduler, Network network, MobsimTimer timer, TravelTime travelTime,
			TravelDisutility travelDisutility, ChargingInfrastructure chargingInfrastructure) {
		double cellSize = ((RuleBasedETaxiOptimizerParams)taxiCfg.getTaxiOptimizerParams()).getRuleBasedTaxiOptimizerParams()
				.getCellSize();
		return RuleBasedETaxiOptimizer.create(eventsManager, taxiCfg, fleet, eScheduler, network, timer, travelTime,
				travelDisutility, chargingInfrastructure, new SquareGridSystem(network, cellSize));
	}

	public static RuleBasedETaxiOptimizer create(EventsManager eventsManager, TaxiConfigGroup taxiCfg, Fleet fleet,
			ETaxiScheduler eScheduler, Network network, MobsimTimer timer, TravelTime travelTime,
			TravelDisutility travelDisutility, ChargingInfrastructure chargingInfrastructure, ZonalSystem zonalSystem) {
		IdleTaxiZonalRegistry idleTaxiRegistry = new IdleTaxiZonalRegistry(zonalSystem, eScheduler);
		UnplannedRequestZonalRegistry unplannedRequestRegistry = new UnplannedRequestZonalRegistry(zonalSystem);
		BestDispatchFinder dispatchFinder = new BestDispatchFinder(eScheduler, network, timer, travelTime,
				travelDisutility);
		RuleBasedRequestInserter requestInserter = new RuleBasedRequestInserter(eScheduler, timer, dispatchFinder,
				((RuleBasedETaxiOptimizerParams)taxiCfg.getTaxiOptimizerParams()).getRuleBasedTaxiOptimizerParams(),
				idleTaxiRegistry, unplannedRequestRegistry);

		return new RuleBasedETaxiOptimizer(eventsManager, taxiCfg, fleet, eScheduler, chargingInfrastructure,
				idleTaxiRegistry, unplannedRequestRegistry, dispatchFinder, requestInserter);
	}

	// TODO MIN_RELATIVE_SOC should depend on the weather and time of day
	private final RuleBasedETaxiOptimizerParams params;
	private final ChargingInfrastructure chargingInfrastructure;
	private final BestChargerFinder eDispatchFinder;
	private final ETaxiScheduler eScheduler;
	private final IdleTaxiZonalRegistry idleTaxiRegistry;

	public RuleBasedETaxiOptimizer(EventsManager eventsManager, TaxiConfigGroup taxiCfg, Fleet fleet,
			ETaxiScheduler eScheduler, ChargingInfrastructure chargingInfrastructure,
			IdleTaxiZonalRegistry idleTaxiRegistry, UnplannedRequestZonalRegistry unplannedRequestRegistry,
			BestDispatchFinder dispatchFinder, UnplannedRequestInserter requestInserter) {
		super(eventsManager, taxiCfg, fleet, eScheduler, idleTaxiRegistry, unplannedRequestRegistry, requestInserter);
		this.params = (RuleBasedETaxiOptimizerParams)taxiCfg.getTaxiOptimizerParams();
		this.chargingInfrastructure = chargingInfrastructure;
		this.eScheduler = eScheduler;
		this.idleTaxiRegistry = idleTaxiRegistry;
		eDispatchFinder = new BestChargerFinder(dispatchFinder);
	}

	@Override
	public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e) {
		if (isNewDecisionEpoch(e, params.getSocCheckTimeStep())) {
			@SuppressWarnings("unchecked")
			Stream<EvDvrpVehicle> eTaxis = (Stream<EvDvrpVehicle>)(Stream<? extends DvrpVehicle>)idleTaxiRegistry.vehicles();
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
	public void nextTask(DvrpVehicle vehicle) {
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
		return b.getSoc() < params.getMinRelativeSoc() * b.getCapacity();
	}
}
