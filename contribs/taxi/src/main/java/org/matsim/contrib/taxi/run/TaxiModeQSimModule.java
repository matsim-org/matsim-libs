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

package org.matsim.contrib.taxi.run;

import org.matsim.contrib.dvrp.passenger.DefaultPassengerRequestValidator;
import org.matsim.contrib.dvrp.passenger.PassengerEngineQSimModule;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.contrib.dvrp.passenger.PassengerRequestValidator;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentSourceQSimModule;
import org.matsim.contrib.taxi.optimizer.TaxiModeOptimizerQSimModule;
import org.matsim.contrib.taxi.passenger.TaxiRequestCreator;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.modal.ModalProviders;

import com.google.inject.Inject;

/**
 * @author michalm
 */
public class TaxiModeQSimModule extends AbstractDvrpModeQSimModule {
	private final TaxiConfigGroup taxiCfg;
	private final AbstractQSimModule optimizerQSimModule;

	public TaxiModeQSimModule(TaxiConfigGroup taxiCfg) {
		this(taxiCfg, new TaxiModeOptimizerQSimModule(taxiCfg));
	}

	public TaxiModeQSimModule(TaxiConfigGroup taxiCfg, AbstractQSimModule optimizerQSimModule) {
		super(taxiCfg.getMode());
		this.taxiCfg = taxiCfg;
		this.optimizerQSimModule = optimizerQSimModule;
	}

	@Override
	protected void configureQSim() {
		install(new VrpAgentSourceQSimModule(getMode()));
		install(new PassengerEngineQSimModule(getMode()));
		install(optimizerQSimModule);

		bindModal(PassengerRequestCreator.class).toProvider(
				new ModalProviders.AbstractProvider<>(getMode(), DvrpModes::mode) {
					@Inject
					private EventsManager events;

					@Override
					public TaxiRequestCreator get() {
						return new TaxiRequestCreator(getMode(), events);
					}
				}).asEagerSingleton();

		bindModal(PassengerRequestValidator.class).to(DefaultPassengerRequestValidator.class).asEagerSingleton();
	}
}
