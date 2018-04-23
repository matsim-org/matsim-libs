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

package electric.edrt.run;

import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DrtTask;
import org.matsim.contrib.drt.schedule.DrtTask.DrtTaskType;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.vsp.edvrp.edrt.optimizer.EDrtVehicleDataEntryFactory.EDrtVehicleDataEntryFactoryProvider;
import org.matsim.vsp.edvrp.edrt.run.EDrtControlerCreator;
import org.matsim.vsp.ev.EvConfigGroup;
import org.matsim.vsp.ev.EvModule;
import org.matsim.vsp.ev.charging.FixedSpeedChargingStrategy;
import org.matsim.vsp.ev.discharging.AuxEnergyConsumption;
import org.matsim.vsp.ev.discharging.DriveEnergyConsumption;
import org.matsim.vsp.ev.dvrp.EvDvrpIntegrationModule;

import electric.edrt.energyconsumption.VwAVAuxEnergyConsumption;
import electric.edrt.energyconsumption.VwDrtDriveEnergyConsumption;

public class RunEDrtScenario {
	private static final String CONFIG_FILE = "mielec_2014_02/mielec_edrt_config.xml";
	private static final double CHARGING_SPEED_FACTOR = 1.; // full speed
	private static final double MAX_RELATIVE_SOC = 0.8;// charge up to 80% SOC
	private static final double MIN_RELATIVE_SOC = 0.2;// send to chargers vehicles below 20% SOC
	private static final double TEMPERATURE = 20;// oC

	public static void run(String configFile, boolean otfvis) {
		Config config = ConfigUtils.loadConfig(configFile, new DrtConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup(), new EvConfigGroup());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		createControler(config).run();
	}

	public static Controler createControler(Config config) {
		Controler controler = EDrtControlerCreator.createControler(config, false);
		controler.addOverridingModule(new EvModule());
		controler.addOverridingModule(createEvDvrpIntegrationModule());
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(EDrtVehicleDataEntryFactoryProvider.class)
						.toInstance(new EDrtVehicleDataEntryFactoryProvider(MIN_RELATIVE_SOC));
				bind(DriveEnergyConsumption.class).to(VwDrtDriveEnergyConsumption.class);
				bind(AuxEnergyConsumption.class).to(VwAVAuxEnergyConsumption.class);
			}
		});

		
		return controler;
	}

	public static EvDvrpIntegrationModule createEvDvrpIntegrationModule() {
		return new EvDvrpIntegrationModule()//
				.setChargingStrategyFactory(charger -> new FixedSpeedChargingStrategy(
						charger.getPower() * CHARGING_SPEED_FACTOR, MAX_RELATIVE_SOC))//
				.setTemperatureProvider(() -> TEMPERATURE) //
				.setTurnedOnPredicate(RunEDrtScenario::isTurnedOn)//
				.setVehicleFileUrlGetter(cfg -> DrtConfigGroup.get(cfg).getVehiclesFileUrl(cfg.getContext()));
	}

	private static boolean isTurnedOn(Vehicle vehicle) {
		Schedule schedule = vehicle.getSchedule();
		if (schedule.getStatus() == ScheduleStatus.STARTED) {
			DrtTaskType currentTaskType = ((DrtTask)schedule.getCurrentTask()).getDrtTaskType();
			return currentTaskType != DrtTaskType.STAY;// turned on only if DRIVE or STOP
		}
		return false;
	}

	public static void main(String[] args) {
		RunEDrtScenario.run(CONFIG_FILE, false);
	}
}
