/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.wrashid.PSF.converter.addingParkings;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.PSF.ParametersPSFMutator;
import playground.wrashid.PSF.energy.AddEnergyScoreListener;
import playground.wrashid.PSF.energy.SimulationStartupListener;
import playground.wrashid.PSF.energy.charging.ChargingTimes;
import playground.wrashid.PSF.energy.charging.optimizedCharging.OptimizedCharger;
import playground.wrashid.PSF.energy.consumption.LogEnergyConsumption;
import playground.wrashid.PSF.parking.LogParkingTimes;

/*
 * Just testing, that the scenario runs without errors.
 */

class OptimizedChargerTestGeneral implements ParametersPSFMutator {

	Controler controler;

	public OptimizedChargerTestGeneral(Controler controler) {
		this.controler = controler;
	}

	@Override
	public void mutateParameters() {

	}

	public void optimizedChargerTest() {
		controler.addControlerListener(new AddEnergyScoreListener());

		LogEnergyConsumption logEnergyConsumption = new LogEnergyConsumption(controler);
		LogParkingTimes logParkingTimes = new LogParkingTimes(controler);
		SimulationStartupListener simulationStartupListener = new SimulationStartupListener(controler);
		controler.addControlerListener(simulationStartupListener);

		simulationStartupListener.addEventHandler(logEnergyConsumption);
		simulationStartupListener.addEventHandler(logParkingTimes);
		simulationStartupListener.addParameterPSFMutator(this);

		controler.run();

		OptimizedCharger optimizedCharger = new OptimizedCharger(logEnergyConsumption.getEnergyConsumption(),
				logParkingTimes.getParkingTimes(), Double.parseDouble(controler.getConfig().findParam("PSF",
						"default.maxBatteryCapacity")));
		HashMap<Id, ChargingTimes> chargingTimes = optimizedCharger.getChargingTimes();

		ChargingTimes chargingTimesOfAgentOne = chargingTimes.get(new IdImpl("66805"));

		// the agent charges once at home in the evening (during off peak time),
		// because the energy consumption
		// per link is set very low in the scenario and there is no need to
		// charge during peak time.
		MatsimTestCase.assertEquals(1, chargingTimesOfAgentOne.getChargingTimes().size());
	}

}
