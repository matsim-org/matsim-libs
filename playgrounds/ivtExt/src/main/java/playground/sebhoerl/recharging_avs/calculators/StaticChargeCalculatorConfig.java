package playground.sebhoerl.recharging_avs.calculators;

import org.matsim.core.config.ReflectiveConfigGroup;

public class StaticChargeCalculatorConfig extends ReflectiveConfigGroup {
    static final String STATIC_CHARGE_CALCULATOR = "static_charge_calculator";

    static final String DISCHARGE_RATE_BY_DISTANCE = "dischargeRateByDistance";
    static final String DISCHARGE_RATE_BY_TIME = "dischargeRateByTime";
    static final String MAXIMUM_CHARGE = "maximumCharge";
    static final String MINIMUM_CHARGE = "minimumCharge";
    static final String RECHARGE_RATE_PER_TIME = "rechargeRatePerTime";

    private double dischargeRateByDistance = Double.NaN;
    private double dischargeRateByTime = Double.NaN;

    private double maximumCharge = Double.NaN;
    private double minimumCharge = Double.NaN;

    private double rechargeRatePerTime = Double.NaN;

    public StaticChargeCalculatorConfig() {
        super("static_charge_calculator");
    }

    @ReflectiveConfigGroup.StringGetter(DISCHARGE_RATE_BY_DISTANCE)
    public double getDischargeRateByDistance() {
        return dischargeRateByDistance;
    }

    @ReflectiveConfigGroup.StringSetter(DISCHARGE_RATE_BY_DISTANCE)
    public void setDischargeRateByDistance(double dischargeRateByDistance) {
        this.dischargeRateByDistance = dischargeRateByDistance;
    }

    @ReflectiveConfigGroup.StringGetter(DISCHARGE_RATE_BY_TIME)
    public double getDischargeRateByTime() {
        return dischargeRateByTime;
    }

    @ReflectiveConfigGroup.StringSetter(DISCHARGE_RATE_BY_TIME)
    public void setDischargeRateByTime(double dischargeRateByTime) {
        this.dischargeRateByTime = dischargeRateByTime;
    }

    @ReflectiveConfigGroup.StringGetter(MAXIMUM_CHARGE)
    public double getMaximumCharge() {
        return maximumCharge;
    }

    @ReflectiveConfigGroup.StringSetter(MAXIMUM_CHARGE)
    public void setMaximumCharge(double maximumCharge) {
        this.maximumCharge = maximumCharge;
    }

    @ReflectiveConfigGroup.StringGetter(MINIMUM_CHARGE)
    public double getMinimumCharge() {
        return minimumCharge;
    }

    @ReflectiveConfigGroup.StringSetter(MINIMUM_CHARGE)
    public void setMinimumCharge(double minimumCharge) {
        this.minimumCharge = minimumCharge;
    }

    @ReflectiveConfigGroup.StringGetter(RECHARGE_RATE_PER_TIME)
    public double getRechargeRatePerTime() {
        return rechargeRatePerTime;
    }

    @ReflectiveConfigGroup.StringSetter(RECHARGE_RATE_PER_TIME)
    public void setRechargeRatePerTime(double rechargeRatePerTime) {
        this.rechargeRatePerTime = rechargeRatePerTime;
    }
}
