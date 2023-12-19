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
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.ev.EvUnits;
import org.matsim.contrib.ev.fleet.ElectricFleetUtils;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.fleet.ElectricVehicleSpecification;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.contrib.ev.infrastructure.ImmutableChargerSpecification;
import org.matsim.vehicles.Vehicle;

import com.google.common.collect.ImmutableList;

/**
 * @author Michal Maciejewski (michalm)
 */
public class FastThenSlowChargingTest {

	@Test
	void calcChargingPower() {
		//fast charger (2 c)
		assertCalcChargingPower(100, 0, 200, 175);
		assertCalcChargingPower(100, 50, 200, 175);
		assertCalcChargingPower(100, 50.001, 200, 125);
		assertCalcChargingPower(100, 75, 200, 125);
		assertCalcChargingPower(100, 75.001, 200, 50);
		assertCalcChargingPower(100, 100, 200, 50);

		//medium-speed charger (1 c)
		assertCalcChargingPower(100, 0, 100, 100);
		assertCalcChargingPower(100, 50, 100, 100);
		assertCalcChargingPower(100, 50.001, 100, 100);
		assertCalcChargingPower(100, 75, 100, 100);
		assertCalcChargingPower(100, 75.001, 100, 50);
		assertCalcChargingPower(100, 100, 100, 50);

		//slow charger (0.4 c)
		assertCalcChargingPower(100, 0, 40, 40);
		assertCalcChargingPower(100, 50, 40, 40);
		assertCalcChargingPower(100, 50.001, 40, 40);
		assertCalcChargingPower(100, 75, 40, 40);
		assertCalcChargingPower(100, 75.001, 40, 40);
		assertCalcChargingPower(100, 100, 40, 40);
	}

	private void assertCalcChargingPower(double capacity_kWh, double charge_kWh, double chargerPower_kW,
			double expectedChargingPower_kW) {
		ChargerSpecification charger = createCharger(chargerPower_kW);
		ElectricVehicle electricVehicle = createElectricVehicle(capacity_kWh, charge_kWh);
		Assertions.assertThat(electricVehicle.getChargingPower().calcChargingPower(charger))
				.isCloseTo(EvUnits.kW_to_W(expectedChargingPower_kW), Percentage.withPercentage(1e-13));
	}

	@Test
	void calcChargingTime_singleSection() {
		//fast charger (2 c)
		assertCalcChargingTime(100, 0, 0, 200, 0);
		assertCalcChargingTime(100, 0, 17.5, 200, 360);
		assertCalcChargingTime(100, 50, 0, 200, 0);
		assertCalcChargingTime(100, 50, 12.5, 200, 360);
		assertCalcChargingTime(100, 75, 0, 200, 0);
		assertCalcChargingTime(100, 75, 5, 200, 360);
		assertCalcChargingTime(100, 100, 0, 200, 0);

		//medium-speed charger (1 c)
		assertCalcChargingTime(100, 0, 0, 100, 0);
		assertCalcChargingTime(100, 0, 10, 100, 360);
		assertCalcChargingTime(100, 50, 0, 100, 0);
		assertCalcChargingTime(100, 50, 10, 100, 360);
		assertCalcChargingTime(100, 75, 0, 100, 0);
		assertCalcChargingTime(100, 75, 5, 100, 360);
		assertCalcChargingTime(100, 100, 0, 100, 0);

		//slow charger (0.4 c)
		assertCalcChargingTime(100, 0, 0, 40, 0);
		assertCalcChargingTime(100, 0, 4, 40, 360);
		assertCalcChargingTime(100, 50, 0, 40, 0);
		assertCalcChargingTime(100, 50, 4, 40, 360);
		assertCalcChargingTime(100, 75, 0, 40, 0);
		assertCalcChargingTime(100, 75, 4, 40, 360);
		assertCalcChargingTime(100, 100, 0, 40, 0);
	}

	@Test
	void calcChargingTime_crossSection() {
		//fast charger (2 c)
		assertCalcChargingTime(100, 32.5, 17.5 + 12.5, 200, 2 * 360);
		assertCalcChargingTime(100, 62.5, 12.5 + 5, 200, 2 * 360);
		assertCalcChargingTime(100, 32.5, 17.5 + 12.5 + 12.5 + 5, 200, 4 * 360);

		//medium-speed charger (1 c)
		assertCalcChargingTime(100, 40, 10 + 10, 100, 2 * 360);
		assertCalcChargingTime(100, 65, 10 + 5, 100, 2 * 360);
		assertCalcChargingTime(100, 40, 10 + 10 + 5 + 10 + 5, 100, 4.5 * 360);

		//slow charger (0.4 c)
		assertCalcChargingTime(100, 46, 4 + 4, 40, 2 * 360);
		assertCalcChargingTime(100, 71, 4 + 4, 40, 2 * 360);
		assertCalcChargingTime(100, 46, 4 + 4 + 17 + 4 + 4, 40, 8.25 * 360);
	}

	@Test
	void calcChargingTime_exceptions() {
		Assertions.assertThatThrownBy(() -> assertCalcChargingTime(100, 0, -1, 200, 2 * 360))
				.isExactlyInstanceOf(IllegalArgumentException.class)
				.hasMessageStartingWith("Energy is negative: ");
		Assertions.assertThatThrownBy(() -> assertCalcChargingTime(100, 90, 11, 200, 2 * 360))
				.isExactlyInstanceOf(IllegalArgumentException.class)
				.hasMessageStartingWith("End charge greater than battery capacity: ");
	}

	private void assertCalcChargingTime(double capacity_kWh, double charge_kWh, double energy_kWh,
			double chargerPower_kW, double expectedChargingTime_s) {
		ChargerSpecification charger = createCharger(chargerPower_kW);
		ElectricVehicle electricVehicle = createElectricVehicle(capacity_kWh, charge_kWh);
		Assertions.assertThat(((FastThenSlowCharging)electricVehicle.getChargingPower()).calcChargingTime(charger,
				EvUnits.kWh_to_J(energy_kWh))).isCloseTo(expectedChargingTime_s, Percentage.withPercentage(1e-13));
	}

	private ChargerSpecification createCharger(double chargerPower_kW) {
		return ImmutableChargerSpecification.newBuilder()
				.id(Id.create("charger_id", Charger.class))
				.chargerType(ChargerSpecification.DEFAULT_CHARGER_TYPE)
				.linkId(Id.createLinkId("link_id"))
				.plugPower(EvUnits.kW_to_W(chargerPower_kW))
				.plugCount(1)
				.build();
	}

	private ElectricVehicle createElectricVehicle(double capacity_kWh, double charge_kWh) {
		// this record is a bit hacky implementation of ElectricVehicleSpecification just meant for tests
		record TestEvSpecification(Id<Vehicle> getId, Vehicle getMatsimVehicle, String getVehicleType,
								   ImmutableList<String> getChargerTypes, double getBatteryCapacity,
								   double getInitialSoc) implements ElectricVehicleSpecification {
		}
		var specification = new TestEvSpecification(Id.create("ev_id", Vehicle.class), null, "vt",
				ImmutableList.of("ct"), EvUnits.kWh_to_J(capacity_kWh), charge_kWh / capacity_kWh);

		return ElectricFleetUtils.create(specification, ev -> ( link, travelTime, linkEnterTime) -> {
			throw new UnsupportedOperationException();
		}, ev -> (beginTime, duration, linkId) -> {
			throw new UnsupportedOperationException();
		}, FastThenSlowCharging::new );
	}

	@Test
	void calcEnergyCharge() {
		assertCalcEnergyCharge(100, 100, 200, 10, 0);
		assertCalcEnergyCharge(100, 76, 200, 10, 500000);
		assertCalcEnergyCharge(100, 51, 200, 10, 1250000);
		assertCalcEnergyCharge(100, 50, 200, 10, 1250000);
		assertCalcEnergyCharge(100, 0, 200, 10, 1750000);
	}

	private void assertCalcEnergyCharge(double capacity_kWh, double charge_kWh, double chargerPower_kW,
			double chargingPeriod, double expectedEnergy) {
		ChargerSpecification charger = createCharger(chargerPower_kW);
		ElectricVehicle electricVehicle = createElectricVehicle(capacity_kWh, charge_kWh);
		double energy = ((FastThenSlowCharging)electricVehicle.getChargingPower()).calcEnergyCharged(charger,
				chargingPeriod);
		Assertions.assertThat(energy).isCloseTo(expectedEnergy, Percentage.withPercentage(1e-13));
	}

	@Test
	void calcEnergyCharged_exceptions() {
		Assertions.assertThatThrownBy(() -> assertCalcEnergyCharge(100, 100, 10, -1, 0))
				.isExactlyInstanceOf(IllegalArgumentException.class)
				.hasMessageStartingWith("Charging period is negative: ");
	}

	@Test
	void calcEnergyChargeAndVerifyWithDuration() {
		assertEnergyAndDurationCalcCompliance(100, 76, 200, 100);
		assertEnergyAndDurationCalcCompliance(100, 51, 200, 100);
		assertEnergyAndDurationCalcCompliance(100, 50, 200, 100);
		assertEnergyAndDurationCalcCompliance(100, 0, 200, 3000);
	}

	private void assertEnergyAndDurationCalcCompliance(double capacity_kWh, double charge_kWh, double chargerPower_kW,
			double chargingPeriod) {
		ChargerSpecification charger = createCharger(chargerPower_kW);
		ElectricVehicle electricVehicle = createElectricVehicle(capacity_kWh, charge_kWh);
		double energyCharged_J = ((FastThenSlowCharging)electricVehicle.getChargingPower()).calcEnergyCharged(charger,
				chargingPeriod);
		double chargingTime = ((FastThenSlowCharging)electricVehicle.getChargingPower()).calcChargingTime(charger,
				energyCharged_J);
		Assertions.assertThat(chargingTime).isCloseTo(chargingPeriod, Percentage.withPercentage(1e-13));
	}

}
