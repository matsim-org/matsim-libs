package playground.sebhoerl.recharging_avs.calculators;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.consistency.ConfigConsistencyChecker;

public class StaticChargeCalculatorConfigChecker implements ConfigConsistencyChecker {
    private static final Logger log = Logger.getLogger(StaticChargeCalculatorConfigChecker.class);

    @Override
    public void checkConsistency(Config config) {
        StaticChargeCalculatorConfig chargeCalculatorConfig =
                (StaticChargeCalculatorConfig) config.getModules().get(StaticChargeCalculatorConfig.STATIC_CHARGE_CALCULATOR);

        if (Double.isNaN(chargeCalculatorConfig.getDischargeRateByDistance())) {
            log.error(StaticChargeCalculatorConfig.DISCHARGE_RATE_BY_DISTANCE + " is not set");
        }

        if (Double.isNaN(chargeCalculatorConfig.getDischargeRateByTime())) {
            log.error(StaticChargeCalculatorConfig.DISCHARGE_RATE_BY_TIME + " is not set");
        }

        if (Double.isNaN(chargeCalculatorConfig.getMaximumCharge())) {
            log.error(StaticChargeCalculatorConfig.MAXIMUM_CHARGE + " is not set");
        }

        if (Double.isNaN(chargeCalculatorConfig.getMinimumCharge())) {
            log.error(StaticChargeCalculatorConfig.MINIMUM_CHARGE + " is not set");
        }

        if (Double.isNaN(chargeCalculatorConfig.getRechargeRatePerTime())) {
            log.error(StaticChargeCalculatorConfig.RECHARGE_RATE_PER_TIME + " is not set");
        }
    }
}
