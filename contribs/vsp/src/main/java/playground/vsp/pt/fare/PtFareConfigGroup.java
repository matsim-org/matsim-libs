package playground.vsp.pt.fare;

import org.matsim.core.config.ReflectiveConfigGroup;

import jakarta.validation.constraints.PositiveOrZero;

import java.util.Map;

public class PtFareConfigGroup extends ReflectiveConfigGroup {
    public static final String PT_FARE = "pt fare";
    public static final String MODULE_NAME = "ptFare";
    public static final String PT_FARE_CALCULATION = "ptFareCalculation";
    public static final String APPLY_DAILY_CAP = "applyDailyCap";
    public static final String DAILY_CAP_FACTOR = "dailyCapFactor";

    public enum PtFareCalculationModels {distanceBased, fareZoneBased} // More to come (e.g. zone based, hybrid...)

    private static final String PT_FARE_CALCULATION_CMT = "PT fare calculation scheme. Current implementation: distanceBased (more to come...)";
    public static final String DAILY_CAP_FACTOR_CMT = "When daily cap is applied, upperBound  = dailyCapFactor * max Fare of the day. " +
                                                                        "This value is decided by the ratio between average daily cost of a ticket subscription and the single " +
                                                                        "trip ticket of the same trip. Usually this value should be somewhere between 1.0 and 2.0";
    public static final String APPLY_DAILY_CAP_CMT = "Enable the daily cap for daily PT fare to count for ticket subscription. Input value: true or false";

    private PtFareCalculationModels ptFareCalculation = PtFareCalculationModels.distanceBased; // Use distance based calculation by default
    private boolean applyDailyCap = true;
    @PositiveOrZero
    private double dailyCapFactor = 1.5;

    public PtFareConfigGroup() {
        super(MODULE_NAME);
    }

    @Override
    public Map<String, String> getComments() {
        Map<String, String> map = super.getComments();
        map.put(PT_FARE_CALCULATION, PT_FARE_CALCULATION_CMT );
        map.put(APPLY_DAILY_CAP, APPLY_DAILY_CAP_CMT);
        map.put(DAILY_CAP_FACTOR, DAILY_CAP_FACTOR_CMT);
        return map;
    }

    @StringGetter(PT_FARE_CALCULATION)
    public PtFareCalculationModels getPtFareCalculation() {
        return ptFareCalculation;
    }

    @StringSetter(PT_FARE_CALCULATION)
    public void setPtFareCalculationModel(PtFareCalculationModels ptFareCalculation) {
        this.ptFareCalculation = ptFareCalculation;
    }

    @StringGetter(APPLY_DAILY_CAP)
    public boolean isDailyCapApplied() {
        return applyDailyCap;
    }

    @StringSetter(APPLY_DAILY_CAP)
    public void setApplyDailyCap(boolean applyDailyCap) {
        this.applyDailyCap = applyDailyCap;
    }


    @StringGetter(DAILY_CAP_FACTOR)
    public double getDailyCapFactor() {
        return dailyCapFactor;
    }

    /**
     * @param dailyCapFactor -- {@value #DAILY_CAP_FACTOR_CMT}
     */
    @StringSetter(DAILY_CAP_FACTOR)
    public void setDailyCapFactor(double dailyCapFactor) {
        this.dailyCapFactor = dailyCapFactor;
    }
}
