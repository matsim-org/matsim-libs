package playground.sebhoerl.recharging_avs.calculators;

public interface BinnedChargeCalculatorData {
    double getDischargeRateByDistance(int bin);
    double getDischargeRateByTime(int bin);
    double getRechgargeRate(int bin);
    double getMaximumCharge(int bin);
    double getMinimumCharge(int bin);

    int calculateBin(double time);
    double getBinStartTime(int bin);
    double getBinEndTime(int bin);
    double getBinDuration(int bin);

    int getNumberOfBins();
}
