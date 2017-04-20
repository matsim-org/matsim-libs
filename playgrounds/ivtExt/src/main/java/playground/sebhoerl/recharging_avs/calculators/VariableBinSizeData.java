package playground.sebhoerl.recharging_avs.calculators;

import java.util.ArrayList;
import java.util.Iterator;

public class VariableBinSizeData implements BinnedChargeCalculatorData {
    private ArrayList<Integer> startTimes = new ArrayList<>();
    private ArrayList<Double> dischargeRatesByDistance = new ArrayList<>();
    private ArrayList<Double> dischargeRatesByTime = new ArrayList<>();
    private ArrayList<Double> rechargeRates = new ArrayList<>();
    private ArrayList<Double> maximumCharges = new ArrayList<>();
    private ArrayList<Double> minimumCharges = new ArrayList<>();

    public void addBin(int startTime, double dischargeRateByDistance, double dischargeRateByTime, double rechargeRate, double maximumCharge, double minimumCharge) {
        int previous = -1;
        if (startTimes.size() > 0) previous = startTimes.get(startTimes.size() - 1);

        if (startTime < previous) {
            throw new IllegalArgumentException("Bins must be added in chronological order and must have a duraton of at least one second");
        }

        startTimes.add(startTime);
        dischargeRatesByDistance.add(dischargeRateByDistance);
        dischargeRatesByTime.add(dischargeRateByTime);
        rechargeRates.add(rechargeRate);
        maximumCharges.add(maximumCharge);
        minimumCharges.add(minimumCharge);
    }

    public boolean hasFixedIntervals() {
        if (startTimes.size() < 2) return false;
        int firstInterval = startTimes.get(1) - startTimes.get(0);

        for (int i = 2; i < startTimes.size(); i++) {
            if (startTimes.get(i) - startTimes.get(i-1) != firstInterval) {
                return false;
            }
        }

        return true;
    }

    @Override
    public double getDischargeRateByDistance(int bin) {
        return dischargeRatesByDistance.get(bin);
    }

    @Override
    public double getDischargeRateByTime(int bin) {
        return dischargeRatesByTime.get(bin);
    }

    @Override
    public double getRechgargeRate(int bin) {
        return rechargeRates.get(bin);
    }

    @Override
    public double getMaximumCharge(int bin) {
        return maximumCharges.get(bin);
    }

    @Override
    public double getMinimumCharge(int bin) {
        return minimumCharges.get(bin);
    }

    @Override
    public int calculateBin(double time) {
        Iterator<Integer> iterator = startTimes.iterator();
        int index = 0;

        while (iterator.hasNext()) {
            if (iterator.next() < time) {
                index++;
            }
        }

        return Math.max(0, index - 1);
    }

    @Override
    public double getBinStartTime(int bin) {
        return startTimes.get(bin);
    }

    @Override
    public double getBinEndTime(int bin) {
        if (bin == startTimes.size() - 1) {
            return startTimes.get(startTimes.size() - 1);
        }

        return startTimes.get(bin + 1);
    }

    @Override
    public double getBinDuration(int bin) {
        return getBinEndTime(bin) - getBinStartTime(bin);
    }

    @Override
    public int getNumberOfBins() {
        return startTimes.size();
    }
}
