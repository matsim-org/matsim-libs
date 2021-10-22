package org.matsim.contrib.drt.extension.eshifts.charging;

import org.matsim.contrib.ev.charging.BatteryCharging;
import org.matsim.contrib.ev.charging.FastThenSlowCharging;
import org.matsim.contrib.ev.charging.FixedSpeedCharging;
import org.matsim.contrib.ev.fleet.Battery;
import org.matsim.contrib.ev.fleet.ElectricVehicle;

/**
 * @author nkuehnel / MOIA
 */
public class ChargingPowerEstimations {

    public static double estimatePowerCharged(ElectricVehicle ev, double duration) {
        final BatteryCharging chargingPower = (BatteryCharging) ev.getChargingPower();
        if (chargingPower instanceof FastThenSlowCharging) {
            return estimatePowerChargedFastThenSlow(ev, duration);
        } else if (chargingPower instanceof FixedSpeedCharging) {
            return estimatePowerChargedFixedSpeed(ev, duration);
        }
        throw new IllegalStateException("Unknown battery charging");
    }

    public static double estimatePowerChargedFixedSpeed(ElectricVehicle ev, double duration) {
        final Battery battery = ev.getBattery();
        double c = battery.getCapacity() / 3600.;
        double power = duration * c;
        return Math.min(battery.getSoc() + power, battery.getCapacity());
    }


    public static double estimatePowerChargedFastThenSlow(ElectricVehicle ev, double duration) {

        double remainingTime = duration;

        double power = 0;

        final Battery battery = ev.getBattery();
        double relativeSoc = battery.getSoc() / battery.getCapacity();

        double c = battery.getCapacity() / 3600;
        double capB = 0.5 * battery.getCapacity();
        double capC = 0.75 * battery.getCapacity();

        if (relativeSoc <= 0.5) {
            double diff = capB - battery.getSoc();
            final double chargingSpeed = 1.75 * c;
            double maxTime = diff / chargingSpeed;
            power += Math.min(maxTime, remainingTime) * chargingSpeed;
            remainingTime -= maxTime;
            relativeSoc = (battery.getSoc() + power) / battery.getCapacity();
        }

        if (remainingTime > 0 && relativeSoc <= 0.75) {
            double diff = capC - battery.getSoc() + power;
            final double chargingSpeed = 1.25 * c;
            double maxTime = diff / chargingSpeed;
            power += Math.min(maxTime, remainingTime) * chargingSpeed;
            remainingTime -= maxTime;
            relativeSoc = (battery.getSoc() + power) / battery.getCapacity();
        }

        if (remainingTime > 0 && relativeSoc > 0.75) {
            double diff = capC - battery.getSoc() + power;
            final double chargingSpeed = 1.25 * c;
            double maxTime = diff / chargingSpeed;
            power += Math.min(maxTime, remainingTime) * chargingSpeed;
        }
        return Math.min(battery.getSoc() + power, battery.getCapacity());
    }
}
