package playground.sebhoerl.recharging_avs.calculators;

import com.google.inject.Inject;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;

public class BinnedChargeCalculator implements ChargeCalculator {
    final private BinnedChargeCalculatorConfig config;
    final private BinnedChargeCalculatorData data;
    final private double S_PER_H = 3600.0;

    @Inject
    public BinnedChargeCalculator(BinnedChargeCalculatorConfig config, BinnedChargeCalculatorData data) {
        this.config = config;
        this.data = data;
    }

    @Override
    public double calculateConsumption(VrpPathWithTravelData path) {
        double consumption = 0.0;
        double now = path.getDepartureTime();

        for (int i = 0; i < path.getLinkCount(); i++) {
            consumption += data.getDischargeRateByDistance(data.calculateBin(now)) * path.getLink(i).getLength() / 1e-3;
            now += path.getLinkTravelTime(i);
        }

        return consumption;
    }

    @Override
    public double calculateConsumption(double from, double until) {
        int fromBin = data.calculateBin(from);
        int untilBin = data.calculateBin(until);

        if (fromBin == untilBin) {
            return (until - from) / S_PER_H * data.getDischargeRateByTime(fromBin);
        }

        double consumption = 0.0;

        consumption += (data.getBinEndTime(fromBin) - from) / S_PER_H * data.getDischargeRateByTime(fromBin);
        consumption += (until - data.getBinStartTime(untilBin)) / S_PER_H * data.getDischargeRateByTime(untilBin);

        for (int b = fromBin; b < untilBin; b++) {
            consumption += data.getBinDuration(b) / S_PER_H * data.getDischargeRateByTime(b);
        }

        return consumption;
    }

    @Override
    public double getInitialCharge(double now) {
        return data.getMaximumCharge(data.calculateBin(now));
    }

    @Override
    public double getMaximumCharge(double now) {
        return data.getMaximumCharge(data.calculateBin(now));
    }

    @Override
    public boolean isCritical(double charge, double now) {
        return charge < data.getMinimumCharge(data.calculateBin(now));
    }

    @Override
    public double getRechargeTime(double now) {
        int currentBin = data.calculateBin(now);

        double charge = data.getMinimumCharge(currentBin);
        double time = 0.0;
        double duration;

        do {
            duration = data.getBinDuration(currentBin);

            charge += duration / S_PER_H * data.getRechgargeRate(currentBin);
            time += duration;

            currentBin += 1;
        } while (charge < data.getMaximumCharge(currentBin - 1));

        return time - S_PER_H * (charge - data.getMaximumCharge(currentBin - 1)) / data.getRechgargeRate(currentBin - 1);
    }
}
