package vwExamples.utils.customEV;

import org.matsim.contrib.ev.charging.ChargingStrategy;
import org.matsim.contrib.ev.fleet.Battery;
import org.matsim.contrib.ev.fleet.ElectricVehicle;

public class CustomFastThenSlowCharging implements ChargingStrategy {

	private final double chargingPower;
	private final double maxRelativeSoc;

	public CustomFastThenSlowCharging(double chargingPower,double maxRelativeSoc) {
		if (chargingPower <= 0) {
			throw new IllegalArgumentException("chargingPower must be positive");
		}
		
		if (maxRelativeSoc <= 0 || maxRelativeSoc > 1) {
			throw new IllegalArgumentException("maxRelativeSoc must be in (0,1]");
		}
		this.chargingPower = chargingPower;
		this.maxRelativeSoc = maxRelativeSoc;
	}
	

	@Override
	public double calcEnergyCharge(ElectricVehicle ev, double chargePeriod) {
		Battery b = ev.getBattery();
		double relativeSoc = b.getSoc() / b.getCapacity();
		double c = b.getCapacity() / 3600;
		double currentPower;
		if (relativeSoc <= 0.5) {
			currentPower = Math.min(chargingPower, 1.75 * c);
		} else if (relativeSoc <= 0.75) {
			currentPower = Math.min(chargingPower, 1.25 * c);
		} else {
			currentPower = Math.min(chargingPower, 0.5 * c);
		}
		return currentPower * chargePeriod;
	}

	@Override
	public boolean isChargingCompleted(ElectricVehicle ev) {
		return calcRemainingEnergyToCharge(ev) <= 0;
	}

	@Override
	public double calcRemainingEnergyToCharge(ElectricVehicle ev) {
		Battery b = ev.getBattery();
		return maxRelativeSoc * b.getCapacity() - b.getSoc();
	}

	@Override
	public double calcRemainingTimeToCharge(ElectricVehicle ev) {
		return calcRemainingEnergyToCharge(ev) / chargingPower;//TODO should consider variable charging speed
	}
}
