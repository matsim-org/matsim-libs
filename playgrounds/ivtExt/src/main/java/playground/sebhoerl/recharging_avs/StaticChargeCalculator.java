package playground.sebhoerl.recharging_avs;

import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import playground.sebhoerl.avtaxi.data.AVVehicle;

public class StaticChargeCalculator implements ChargeCalculator {
    @Override
    public double calculateConsumption(VrpPathWithTravelData path) {
        double distance = 0.0;

        for (int i = 0; i < path.getLinkCount(); i++) {
            distance += path.getLink(i).getLength();
        }

        return distance * 1e-3;
    }

    @Override
    public double calculateConsumption(double from, double until) {
        return 0.0;
    }

    @Override
    public double getInitialCharge(AVVehicle vehicle) {
        return 80.0;
    }

    @Override
    public double getMaximumCharge(AVVehicle vehicle) {
        return 80.0;
    }

    @Override
    public boolean isCritical(double charge) {
        return charge <= 0.0;
    }

    @Override
    public double getRechargeTime(double now) {
        return 1800.0;
    }
}
