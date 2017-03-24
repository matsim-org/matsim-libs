package playground.sebhoerl.recharging_avs.calculators;

import java.util.ArrayList;

public class FixedBinSizeData implements BinnedChargeCalculatorData {
    final private int fixedBinDuration;
    final private int initialStartTime;

    private int numberOfBins = 0;

    private ArrayList<Double> dischargeRatesByDistance = new ArrayList<>();
    private ArrayList<Double> dischargeRatesByTime = new ArrayList<>();
    private ArrayList<Double> rechargeRates = new ArrayList<>();
    private ArrayList<Double> maximumCharges = new ArrayList<>();
    private ArrayList<Double> minimumCharges = new ArrayList<>();

    public FixedBinSizeData(int fixedBinDuration, int initialStartTime) {
        this.fixedBinDuration = fixedBinDuration;
        this.initialStartTime = initialStartTime;
    }

    public void addBin(double dischargeRateByDistance, double dischargeRateByTime, double rechargeRate, double maximumCharge, double minimumCharge) {
        dischargeRatesByDistance.add(dischargeRateByDistance);
        dischargeRatesByTime.add(dischargeRateByTime);
        rechargeRates.add(rechargeRate);
        maximumCharges.add(maximumCharge);
        minimumCharges.add(minimumCharge);
        numberOfBins += 1;
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
        return Math.max(0, Math.min(numberOfBins - 1, ((int)time - initialStartTime) / fixedBinDuration));
    }

    @Override
    public double getBinStartTime(int bin) {
        return initialStartTime + fixedBinDuration * bin;
    }

    @Override
    public double getBinEndTime(int bin) {
        return initialStartTime + fixedBinDuration * (Math.min(bin, numberOfBins - 2) + 1);
    }

    @Override
    public double getBinDuration(int bin) {
        return fixedBinDuration;
    }

    @Override
    public int getNumberOfBins() {
        return numberOfBins;
    }

    public static FixedBinSizeData createFromVariableData(VariableBinSizeData variableData) {
        if (!variableData.hasFixedIntervals() || variableData.getNumberOfBins() == 0) {
            throw new IllegalArgumentException();
        }

        FixedBinSizeData fixedData = new FixedBinSizeData((int)variableData.getBinDuration(0), (int)variableData.getBinStartTime(0));

        for (int i = 0; i < variableData.getNumberOfBins(); i++) {
            fixedData.addBin(
                    variableData.getDischargeRateByDistance(i),
                    variableData.getDischargeRateByTime(i),
                    variableData.getRechgargeRate(i),
                    variableData.getMaximumCharge(i),
                    variableData.getMinimumCharge(i)
            );
        }

        return fixedData;
    }
}
