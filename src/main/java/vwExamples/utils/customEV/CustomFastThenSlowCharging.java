package vwExamples.utils.customEV;

import org.matsim.contrib.ev.charging.ChargingPower;
import org.matsim.contrib.ev.fleet.Battery;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.Charger;

public class CustomFastThenSlowCharging implements ChargingPower {
	private final ElectricVehicle electricVehicle;

	public CustomFastThenSlowCharging(ElectricVehicle electricVehicle) {
		this.electricVehicle = electricVehicle;
	}

	@Override
	public double calcChargingPower(Charger charger) {
		Battery b = electricVehicle.getBattery();
		double relativeSoc = b.getSoc() / b.getCapacity();
		double c = b.getCapacity() / 3600;
		if (relativeSoc <= 0.5) {
			return Math.min(charger.getPower(), 1.75 * c);
		} else if (relativeSoc <= 0.75) {
			return Math.min(charger.getPower(), 1.25 * c);
		} else {
			return Math.min(charger.getPower(), 0.5 * c);
		}
	}
}
