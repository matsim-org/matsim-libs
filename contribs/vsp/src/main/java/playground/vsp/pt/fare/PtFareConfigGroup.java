package playground.vsp.pt.fare;

import org.matsim.core.config.ReflectiveConfigGroup;

import jakarta.validation.constraints.PositiveOrZero;

import java.util.Map;

public class PtFareConfigGroup extends ReflectiveConfigGroup {
    public static final String PT_FARE = "pt fare";
    public static final String MODULE_NAME = "ptFare";
    public static final String PT_FARE_CALCULATION = "ptFareCalculation";
    public static final String APPLY_UPPER_BOUND = "applyUpperBound";
    public static final String UPPER_BOUND_FACTOR = "upperBoundFactor";

    public enum PtFareCalculationModels {distanceBased} // More to come (e.g. zone based, hybrid...)

    private static final String PT_FARE_CALCULATION_CMT = "PT fare calculation scheme. Current implementation: distanceBased (more to come...)";
    public static final String UPPER_BOUND_FACTOR_CMT = "When upper bound is applied, upperBound  = upperBoundFactor * max Fare of the day. " +
                                                                        "This value is decided by the ratio between average daily cost of a ticket subscription and the single " +
                                                                        "trip ticket of the same trip. Usually this value should be somewhere between 1.0 and 2.0";
    public static final String APPLY_UPPER_BOUND_CMT = "Enable the upper bound for daily PT fare to count for ticket subscription. Input value: true or false";

    private PtFareCalculationModels ptFareCalculation = PtFareCalculationModels.distanceBased; // Use distance based calculation by default
    private boolean applyUpperBound = true;
    @PositiveOrZero
    private double upperBoundFactor = 1.5;

    public PtFareConfigGroup() {
        super(MODULE_NAME);
    }

    @Override
    public Map<String, String> getComments() {
        Map<String, String> map = super.getComments();
        map.put(PT_FARE_CALCULATION, PT_FARE_CALCULATION_CMT );
        map.put(APPLY_UPPER_BOUND, APPLY_UPPER_BOUND_CMT );
        map.put(UPPER_BOUND_FACTOR, UPPER_BOUND_FACTOR_CMT );
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

    @StringGetter(APPLY_UPPER_BOUND)
    public boolean getApplyUpperBound() {
        return applyUpperBound;
    }

    @StringSetter(APPLY_UPPER_BOUND)
    public void setApplyUpperBound(boolean applyUpperBound) {
        this.applyUpperBound = applyUpperBound;
    }


    @StringGetter(UPPER_BOUND_FACTOR)
    public double getUpperBoundFactor() {
        return upperBoundFactor;
    }

    /**
     * @param upperBoundFactor -- {@value #UPPER_BOUND_FACTOR_CMT}
     */
    @StringSetter(UPPER_BOUND_FACTOR)
    public void setUpperBoundFactor(double upperBoundFactor) {
        this.upperBoundFactor = upperBoundFactor;
    }
}
