package org.matsim.contrib.ev.charging;

import com.google.inject.Inject;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;

public class DefaultChargerPower implements ChargerPower {
	private final ChargerSpecification charger;
	private final double chargingPeriod;

	public DefaultChargerPower(ChargerSpecification charger, double chargingPeriod) {
		this.charger = charger;
		this.chargingPeriod = chargingPeriod;
	}

	@Override
	public void plugVehicle(double now, ElectricVehicle vehicle) {
		// do nothing
	}

	@Override
	public void unplugVehicle(double now, ElectricVehicle vehicle) {
		// do nothing
	}

	@Override
	public double calcMaximumEnergyToCharge(double now, ElectricVehicle vehicle) {
		return charger.getPlugPower() * chargingPeriod;
	}

	@Override
	public void consumeEnergy(double energy) {
		// has no effect
	}

	@Override
	public void update(double now) {
		// has no effect
	}

	static public class Factory implements ChargerPower.Factory {
		private final double chargingPeriod;

		@Inject
		public Factory(EvConfigGroup config) {
			this.chargingPeriod = config.getChargeTimeStep();
		}

		@Override
		public ChargerPower create(ChargerSpecification charger) {
			return new DefaultChargerPower(charger, chargingPeriod);
		}
	}
}
