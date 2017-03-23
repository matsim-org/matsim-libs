package playground.sebhoerl.recharging_avs.calculators;

import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import playground.sebhoerl.avtaxi.data.AVVehicle;

public interface ChargeCalculator {
    double calculateConsumption(VrpPathWithTravelData path);
    double calculateConsumption(double from, double until);

    double getInitialCharge(double now);
    double getMaximumCharge(double now);

    boolean isCritical(double charge, double now);
    double getRechargeTime(double now);
}
