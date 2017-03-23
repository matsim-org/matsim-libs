package playground.sebhoerl.recharging_avs.calculators;

import org.matsim.core.controler.AbstractModule;
import playground.sebhoerl.recharging_avs.RechargeUtils;

public class StaticChargeCalculatorModule extends AbstractModule {
    @Override
    public void install() {
        bind(StaticChargeCalculator.class);
        RechargeUtils.registerChargeCalculator(binder(), "static", StaticChargeCalculator.class);
    }
}
