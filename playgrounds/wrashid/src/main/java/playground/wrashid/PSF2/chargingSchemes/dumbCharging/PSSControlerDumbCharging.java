/* *********************************************************************** *
 * project: org.matsim.*
 * PSSControlerDumbCharging.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.wrashid.PSF2.chargingSchemes.dumbCharging;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.mobsim.framework.events.SimulationBeforeCleanupEvent;

import playground.wrashid.PSF.ParametersPSF;
import playground.wrashid.PSF.ParametersPSFMutator;
import playground.wrashid.PSF.PSS.EventReadControler;
import playground.wrashid.PSF.PSS.PSSControler;
import playground.wrashid.PSF.energy.AddEnergyScoreListener;
import playground.wrashid.PSF.energy.AfterSimulationListener;
import playground.wrashid.PSF.energy.SimulationStartupListener;
import playground.wrashid.PSF.energy.charging.ChargingTimes;
import playground.wrashid.PSF.energy.consumption.LogEnergyConsumption;
import playground.wrashid.PSF.lib.PSFGeneralLib;
import playground.wrashid.PSF.parking.LogParkingTimes;
import playground.wrashid.PSF2.ParametersPSF2;
import playground.wrashid.PSF2.chargingSchemes.ActivityIntervalTracker_NonParallelizableHandler;
import playground.wrashid.PSF2.chargingSchemes.ChargingAfterSimListener;
import playground.wrashid.PSF2.chargingSchemes.ChargingFleetInitializer;
import playground.wrashid.PSF2.chargingSchemes.LinkEnergyConsumptionTracker_NonParallelizableHandler;
import playground.wrashid.PSF2.vehicle.energyConsumption.EnergyConsumptionTable;
import playground.wrashid.lib.obj.GeneralLogObject;

public class PSSControlerDumbCharging extends PSSControler {

	public PSSControlerDumbCharging(String configFilePath, ParametersPSFMutator parameterPSFMutator) {
		super(configFilePath, parameterPSFMutator);
	}

	public void runMATSimIterations() {

		// use the right Controler (read parameter
		Config config = new Config();
		MatsimConfigReader reader = new MatsimConfigReader(config);
		reader.readFile(configFilePath);
		String tempStringValue = config.findParam(ParametersPSF.getPSFModule(), "main.inputEventsForSimulationPath");
		if (tempStringValue != null) {
			// ATTENTION, this does not work at the moment, because the read
			// link from the
			// event file is null and this causes some probelems in my
			// handlers...
			controler = new EventReadControler(configFilePath, tempStringValue);
		} else {
			controler = new Controler(configFilePath);
		}

		controler.setOverwriteFiles(true);

		initializeParametersPSF2(controler);

		addSimulationStartupListener(controler, parameterPSFMutator);

		addAfterSimulationListener(controler);

		controler.addControlerListener(new ShutdownListener() {
			public void notifyShutdown(ShutdownEvent event) {
				ParametersPSF2.getPSFGeneralLog().writeFileAndCloseStream();
			}
		});

		controler.run();

	}

	private void addAfterSimulationListener(Controler controler) {
		controler.addControlerListener(new IterationEndsListener() {

			@Override
			public void notifyIterationEnds(IterationEndsEvent event) {
				ChargingTimes.writeChargingTimes(ParametersPSF2.chargingTimes, event.getControler().getControlerIO()
						.getIterationFilename(event.getControler().getIterationNumber(), "chargingLog.txt"));
				ChargingTimes.writeChargingTimes(ParametersPSF2.chargingTimes, event.getControler().getControlerIO()
						.getOutputFilename("chargingLog.txt"));

				double[][] energyUsageStatistics = ChargingTimes.getEnergyUsageStatistics(ParametersPSF2.chargingTimes,
						ParametersPSF.getHubLinkMapping());

				ChargingTimes.writeEnergyUsageStatisticsData(
						event.getControler().getControlerIO()
								.getIterationFilename(event.getControler().getIterationNumber(), "vehicleEnergyConsumption.txt"),
						energyUsageStatistics);
				ChargingTimes.writeVehicleEnergyConsumptionStatisticsGraphic(event.getControler().getControlerIO()
						.getIterationFilename(event.getControler().getIterationNumber(), "vehicleEnergyConsumption.png"),
						energyUsageStatistics);
			}
		});

	}

	private static void addSimulationStartupListener(Controler controler, ParametersPSFMutator parameterPSFMutator) {
		SimulationStartupListener simulationStartupListener = new SimulationStartupListener(controler);
		controler.addControlerListener(simulationStartupListener);
		simulationStartupListener.addParameterPSFMutator(parameterPSFMutator);
		simulationStartupListener.addEventHandler(new LinkEnergyConsumptionTracker_NonParallelizableHandler());
		simulationStartupListener.addEventHandler(ParametersPSF2.activityIntervalTracker);
		controler.addControlerListener(new ChargingAfterSimListener());
	}

	private static void initializeParametersPSF2(Controler controler) {
		ParametersPSF2.fleetInitializer = new ChargingFleetInitializer();
		ParametersPSF2.energyConsumptionTable = new EnergyConsumptionTable(ParametersPSF2.pathToEnergyConsumptionTable);
		ParametersPSF2.energyStateMaintainer = new ARTEMISEnergyStateMaintainer_StartChargingUponArrival(
				ParametersPSF2.energyConsumptionTable);
		ParametersPSF2.chargingTimes = new HashMap<Id, ChargingTimes>();

		ParametersPSF2.controler = controler;
		ParametersPSF2.activityIntervalTracker = new ActivityIntervalTracker_NonParallelizableHandler();

		controler.addControlerListener(new StartupListener() {
			public void notifyStartup(StartupEvent event) {
				ParametersPSF2.initVehicleFleet(event.getControler());
				ParametersPSF2.setPSFGeneralLog(new GeneralLogObject(event.getControler().getControlerIO()
						.getOutputFilename("PSFGeneralLog.txt")));
			}
		});

		controler.addControlerListener(new BeforeMobsimListener() {
			public void notifyBeforeMobsim(BeforeMobsimEvent event) {
				ParametersPSF2.setPSFIterationLog(new GeneralLogObject(event.getControler().getControlerIO()
						.getIterationFilename(event.getControler().getIterationNumber(), "PSFIterationLog.txt")));
			}
		});

		controler.addControlerListener(new AfterMobsimListener() {
			public void notifyAfterMobsim(AfterMobsimEvent event) {
				ParametersPSF2.getPSFIterationLog().writeFileAndCloseStream();
			}
		});

	}

}
