package playground.sebhoerl.recharging_avs;

import org.matsim.core.controler.AbstractModule;
import playground.sebhoerl.avtaxi.framework.AVUtils;

public class RechargingModule extends AbstractModule {
    @Override
    public void install() {
        bind(ChargeCalculator.class).to(StaticChargeCalculator.class).asEagerSingleton();
        AVUtils.registerDispatcherFactory(binder(), "Recharging", RechargingDispatcher.Factory.class);
    }
}
