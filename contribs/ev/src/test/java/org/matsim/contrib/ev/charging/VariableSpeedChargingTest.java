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
public class VariableSpeedChargingTest {

	@Test
	public void testCalcEnergyCharge() {
		//fast charger (2 c)
		assertCalcChargingPower(100, 0, 200, 75);
		assertCalcChargingPower(100, 5, 200, 100);
		assertCalcChargingPower(100, 10, 200, 125);
		assertCalcChargingPower(100, 15, 200, 150);
		assertCalcChargingPower(100, 20, 200, 150);
		assertCalcChargingPower(100, 45, 200, 150);
		assertCalcChargingPower(100, 50, 200, 150);
		assertCalcChargingPower(100, 75, 200, 155. / 2);
		assertCalcChargingPower(100, 90, 200, 170. / 5);
		assertCalcChargingPower(100, 100, 200, 5);

		//medium-speed charger (1 c)
		assertCalcChargingPower(100, 0, 100, 75);
		assertCalcChargingPower(100, 5, 100, 100);
		assertCalcChargingPower(100, 10, 100, 100);
		assertCalcChargingPower(100, 15, 100, 100);
		assertCalcChargingPower(100, 20, 100, 100);
		assertCalcChargingPower(100, 45, 100, 100);
		assertCalcChargingPower(100, 50, 100, 100);
		assertCalcChargingPower(100, 75, 100, 155. / 2);
		assertCalcChargingPower(100, 90, 100, 170. / 5);
		assertCalcChargingPower(100, 100, 100, 5);

		//slow charger (0.5 c)
		assertCalcChargingPower(100, 0, 50, 50);
		assertCalcChargingPower(100, 5, 50, 50);
		assertCalcChargingPower(100, 10, 50, 50);
		assertCalcChargingPower(100, 15, 50, 50);
		assertCalcChargingPower(100, 20, 50, 50);
		assertCalcChargingPower(100, 45, 50, 50);
		assertCalcChargingPower(100, 50, 50, 50);
		assertCalcChargingPower(100, 75, 50, 50);
		assertCalcChargingPower(100, 90, 50, 170. / 5);
		assertCalcChargingPower(100, 100, 50, 5);
	}

	private void assertCalcChargingPower(double capacity_kWh, double soc_kWh, double chargerPower_kW,
			double expectedChargingPower_kW) {
		ElectricVehicleSpecification specification = ImmutableElectricVehicleSpecification.newBuilder()
				.id(Id.create("ev_id", ElectricVehicle.class))
				.vehicleType("vt")
				.chargerTypes(ImmutableList.of("ct"))
				.batteryCapacity(EvUnits.kWh_to_J(capacity_kWh))
				.initialSoc(EvUnits.kWh_to_J(soc_kWh))
				.build();
		ChargerSpecification chargerSpecification = ImmutableChargerSpecification.newBuilder()
				.id(Id.create("charger_id", Charger.class))
				.chargerType(ChargerSpecification.DEFAULT_CHARGER_TYPE)
				.linkId(Id.createLinkId("link_id"))
				.plugPower(EvUnits.kW_to_W(chargerPower_kW))
				.plugCount(1)
				.build();
		Charger charger = ChargerImpl.create(chargerSpecification, new FakeLink(Id.createLinkId("link_id")),
				ch -> new ChargingWithQueueingLogic(ch, new ChargeUpToMaxSocStrategy(ch, 1), new EventsManagerImpl()));

		ElectricVehicle electricVehicle = ElectricVehicleImpl.create(specification,
				ev -> (link, travelTime, linkEnterTime) -> {
					throw new UnsupportedOperationException();
				}, ev -> (beginTime, duration, linkId) -> {
					throw new UnsupportedOperationException();
				}, VariableSpeedCharging::createForTesla);
		Assertions.assertThat(electricVehicle.getChargingPower().calcChargingPower(charger))
				.isCloseTo(EvUnits.kW_to_W(expectedChargingPower_kW), Percentage.withPercentage(1e-13));
	}
}
