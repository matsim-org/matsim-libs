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

import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.schedule.ScheduleTimingUpdater;
import org.matsim.contrib.etaxi.ETaxiScheduler;
import org.matsim.contrib.etaxi.optimizer.BestChargerFinder;
import org.matsim.contrib.ev.dvrp.EvDvrpVehicle;
import org.matsim.contrib.ev.fleet.Battery;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.contrib.taxi.optimizer.BestDispatchFinder;
import org.matsim.contrib.taxi.optimizer.BestDispatchFinder.Dispatch;
import org.matsim.contrib.taxi.optimizer.UnplannedRequestInserter;
import org.matsim.contrib.taxi.optimizer.rules.IdleTaxiZonalRegistry;
import org.matsim.contrib.taxi.optimizer.rules.RuleBasedTaxiOptimizer;
import org.matsim.contrib.taxi.optimizer.rules.ZonalRegisters;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;

public class RuleBasedETaxiOptimizer extends RuleBasedTaxiOptimizer {

	// TODO MIN_RELATIVE_SOC should depend on the weather and time of day
	private final RuleBasedETaxiOptimizerParams params;
	private final ChargingInfrastructure chargingInfrastructure;
	private final BestChargerFinder eDispatchFinder;
	private final ETaxiScheduler eScheduler;
	private final IdleTaxiZonalRegistry idleTaxiRegistry;

	public RuleBasedETaxiOptimizer(EventsManager eventsManager, TaxiConfigGroup taxiCfg, Fleet fleet,
			ETaxiScheduler eScheduler, ScheduleTimingUpdater scheduleTimingUpdater,
			ChargingInfrastructure chargingInfrastructure, ZonalRegisters zonalRegisters,
			BestDispatchFinder dispatchFinder, UnplannedRequestInserter requestInserter) {
		super(eventsManager, taxiCfg, fleet, eScheduler, scheduleTimingUpdater, zonalRegisters, requestInserter);
		this.params = (RuleBasedETaxiOptimizerParams)taxiCfg.getTaxiOptimizerParams();
		this.chargingInfrastructure = chargingInfrastructure;
		this.eScheduler = eScheduler;
		this.idleTaxiRegistry = zonalRegisters.idleTaxiRegistry;
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

		if (eScheduler.getScheduleInquiry().isIdle(vehicle)) {
			EvDvrpVehicle eTaxi = (EvDvrpVehicle)vehicle;
			if (isUndercharged(eTaxi)) {
				chargeIdleUnderchargedVehicles(Stream.of(eTaxi));
			}
		}
	}

	private boolean isUndercharged(EvDvrpVehicle v) {
		Battery b = v.getElectricVehicle().getBattery();
		return b.getSoc() < params.getMinRelativeSoc() * b.getCapacity();
	}
}
