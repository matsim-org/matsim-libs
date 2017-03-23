package playground.sebhoerl.recharging_avs;

import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import playground.sebhoerl.avtaxi.data.AVVehicle;

public interface ChargeCalculator {
    double calculateConsumption(VrpPathWithTravelData path);
    double calculateConsumption(double from, double until);

    double getInitialCharge(AVVehicle vehicle);
    double getMaximumCharge(AVVehicle vehicle);

    boolean isCritical(double charge);
    double getRechargeTime(double now);
}
