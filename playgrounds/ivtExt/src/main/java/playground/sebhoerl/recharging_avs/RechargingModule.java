package playground.sebhoerl.recharging_avs;

import org.matsim.core.controler.AbstractModule;
import playground.sebhoerl.avtaxi.framework.AVUtils;
import playground.sebhoerl.recharging_avs.calculators.ChargeCalculator;
import playground.sebhoerl.recharging_avs.calculators.StaticChargeCalculator;
import playground.sebhoerl.recharging_avs.logic.RechargingDispatcher;

public class RechargingModule extends AbstractModule {
    @Override
    public void install() {
        bind(ChargeCalculator.class).to(StaticChargeCalculator.class).asEagerSingleton();
        AVUtils.registerDispatcherFactory(binder(), "Recharging", RechargingDispatcher.Factory.class);
    }
}
