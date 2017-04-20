package playground.sebhoerl.recharging_avs;

import com.google.inject.Binder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.MapBinder;
import playground.sebhoerl.recharging_avs.calculators.ChargeCalculator;

public class RechargeUtils {
    static public LinkedBindingBuilder<ChargeCalculator> bindChargeCalculator(Binder binder, String calculatorName) {
        MapBinder<String, ChargeCalculator> map = MapBinder.newMapBinder(
                binder, String.class, ChargeCalculator.class);
        return map.addBinding(calculatorName);
    }

    static public void registerChargeCalculator(Binder binder, String calculatorName, Class<? extends ChargeCalculator> clazz) {
        bindChargeCalculator(binder, calculatorName).to(clazz);
    }
}
