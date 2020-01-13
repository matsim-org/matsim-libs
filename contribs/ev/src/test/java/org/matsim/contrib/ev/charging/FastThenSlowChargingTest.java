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
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.ev.EvUnits;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.fleet.ElectricVehicleImpl;
import org.matsim.contrib.ev.fleet.ElectricVehicleSpecification;
import org.matsim.contrib.ev.fleet.ImmutableElectricVehicleSpecification;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargerImpl;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.contrib.ev.infrastructure.ImmutableChargerSpecification;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.testcases.fakes.FakeLink;

import com.google.common.collect.ImmutableList;

/**
 * @author Michal Maciejewski (michalm)
 */
public class FastThenSlowChargingTest {

	@Test
	public void calcChargingPower() {
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

	private void assertCalcChargingPower(double capacity_kWh, double soc_kWh, double chargerPower_kW,
			double expectedChargingPower_kW) {
		Charger charger = createCharger(chargerPower_kW);
		ElectricVehicle electricVehicle = createElectricVehicle(capacity_kWh, soc_kWh);
		Assertions.assertThat(electricVehicle.getChargingPower().calcChargingPower(charger))
				.isCloseTo(EvUnits.kW_to_W(expectedChargingPower_kW), Percentage.withPercentage(1e-13));
	}

	@Test
	public void calcChargingTime_singleSection() {
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
	public void calcChargingTime_crossSection() {
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
	public void calcChargingTime_exceptions() {
		Assertions.assertThatThrownBy(() -> assertCalcChargingTime(100, 0, -1, 200, 2 * 360))
				.isExactlyInstanceOf(IllegalArgumentException.class)
				.hasMessage("Energy must be positive");
		Assertions.assertThatThrownBy(() -> assertCalcChargingTime(100, 90, 11, 200, 2 * 360))
				.isExactlyInstanceOf(IllegalArgumentException.class)
				.hasMessage("End SOC must not be greater than 100%");
	}

	private void assertCalcChargingTime(double capacity_kWh, double soc_kWh, double energy_kWh, double chargerPower_kW,
			double expectedChargingTime_s) {
		Charger charger = createCharger(chargerPower_kW);
		ElectricVehicle electricVehicle = createElectricVehicle(capacity_kWh, soc_kWh);
		Assertions.assertThat(((FastThenSlowCharging)electricVehicle.getChargingPower()).calcChargingTime(charger,
				EvUnits.kWh_to_J(energy_kWh))).isCloseTo(expectedChargingTime_s, Percentage.withPercentage(1e-13));
	}

	private Charger createCharger(double chargerPower_kW) {
		ChargerSpecification chargerSpecification = ImmutableChargerSpecification.newBuilder()
				.id(Id.create("charger_id", Charger.class))
				.chargerType(ChargerSpecification.DEFAULT_CHARGER_TYPE)
				.linkId(Id.createLinkId("link_id"))
				.maxPower(EvUnits.kW_to_W(chargerPower_kW))
				.plugCount(1)
				.build();
		return ChargerImpl.create(chargerSpecification, new FakeLink(Id.createLinkId("link_id")),
				ch -> new ChargingWithQueueingLogic(ch, new ChargeUpToMaxSocStrategy(ch, 1), new EventsManagerImpl()));
	}

	private ElectricVehicle createElectricVehicle(double capacity_kWh, double soc_kWh) {
		ElectricVehicleSpecification specification = ImmutableElectricVehicleSpecification.newBuilder()
				.id(Id.create("ev_id", ElectricVehicle.class))
				.vehicleType("vt")
				.chargerTypes(ImmutableList.of("ct"))
				.batteryCapacity(EvUnits.kWh_to_J(capacity_kWh))
				.initialSoc(EvUnits.kWh_to_J(soc_kWh))
				.build();
		return ElectricVehicleImpl.create(specification, ev -> (link, travelTime, linkEnterTime) -> {
			throw new UnsupportedOperationException();
		}, ev -> (beginTime, duration, linkId) -> {
			throw new UnsupportedOperationException();
		}, FastThenSlowCharging::new);
	}
}
