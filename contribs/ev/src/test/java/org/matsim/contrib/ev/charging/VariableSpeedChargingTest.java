/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.contrib.ev.charging;

import org.assertj.core.api.Assertions;
import org.assertj.core.data.Percentage;
import org.junit.Test;
import org.matsim.contrib.ev.EvUnits;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.fleet.ElectricVehicleImpl;
import org.matsim.contrib.ev.fleet.ElectricVehicleSpecification;
import org.matsim.contrib.ev.fleet.ImmutableElectricVehicleSpecification;

/**
 * @author Michal Maciejewski (michalm)
 */
public class VariableSpeedChargingTest {

	@Test
	public void testCalcEnergyCharge() {
		//fast charger (2 c)
		assertCalcEnergyCharge(100, 0, 200, 75);
		assertCalcEnergyCharge(100, 5, 200, 100);
		assertCalcEnergyCharge(100, 10, 200, 125);
		assertCalcEnergyCharge(100, 15, 200, 150);
		assertCalcEnergyCharge(100, 20, 200, 150);
		assertCalcEnergyCharge(100, 45, 200, 150);
		assertCalcEnergyCharge(100, 50, 200, 150);
		assertCalcEnergyCharge(100, 75, 200, 155. / 2);
		assertCalcEnergyCharge(100, 90, 200, 170. / 5);
		assertCalcEnergyCharge(100, 100, 200, 5);

		//medium-speed charger (1 c)
		assertCalcEnergyCharge(100, 0, 100, 75);
		assertCalcEnergyCharge(100, 5, 100, 100);
		assertCalcEnergyCharge(100, 10, 100, 100);
		assertCalcEnergyCharge(100, 15, 100, 100);
		assertCalcEnergyCharge(100, 20, 100, 100);
		assertCalcEnergyCharge(100, 45, 100, 100);
		assertCalcEnergyCharge(100, 50, 100, 100);
		assertCalcEnergyCharge(100, 75, 100, 155. / 2);
		assertCalcEnergyCharge(100, 90, 100, 170. / 5);
		assertCalcEnergyCharge(100, 100, 100, 5);

		//slow charger (0.5 c)
		assertCalcEnergyCharge(100, 0, 50, 50);
		assertCalcEnergyCharge(100, 5, 50, 50);
		assertCalcEnergyCharge(100, 10, 50, 50);
		assertCalcEnergyCharge(100, 15, 50, 50);
		assertCalcEnergyCharge(100, 20, 50, 50);
		assertCalcEnergyCharge(100, 45, 50, 50);
		assertCalcEnergyCharge(100, 50, 50, 50);
		assertCalcEnergyCharge(100, 75, 50, 50);
		assertCalcEnergyCharge(100, 90, 50, 170. / 5);
		assertCalcEnergyCharge(100, 100, 50, 5);
	}

	private void assertCalcEnergyCharge(double capacity_kWh, double soc_kWh, double power_kW,
			double energyAfterOneHour_kWh) {
		ElectricVehicleSpecification specification = ImmutableElectricVehicleSpecification.newBuilder()
				.batteryCapacity(EvUnits.kWh_to_J(capacity_kWh))
				.initialSoc(EvUnits.kWh_to_J(soc_kWh))
				.build();
		ElectricVehicle electricVehicle = new ElectricVehicleImpl(specification, null, null);
		VariableSpeedCharging charging = VariableSpeedCharging.createStrategyForTesla(EvUnits.kW_to_W(power_kW), 1);
		Assertions.assertThat(charging.calcEnergyCharge(electricVehicle, 3600))
				.isCloseTo(EvUnits.kWh_to_J(energyAfterOneHour_kWh), Percentage.withPercentage(1e-13));
		Assertions.assertThat(charging.calcEnergyCharge(electricVehicle, 900))
				.isCloseTo(EvUnits.kWh_to_J(energyAfterOneHour_kWh / 4), Percentage.withPercentage(1e-13));
	}
}
