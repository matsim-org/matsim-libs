package org.matsim.contrib.ev.extensions.battery_chargers;

import java.util.Map;

import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.EvConfigGroup.InitialSocBehavior;
import org.matsim.contrib.ev.charging.ChargerPower;
import org.matsim.contrib.ev.charging.DefaultChargerPower;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecification;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.MapBinder;

/**
 * @author sebhoerl
 */
public class BatteryChargerModule extends AbstractModule {
    private MapBinder<String, ChargerPower.Factory> mapBinder;

    @Override
    public void install() {
        prepareMapBinder();

        bindChargerPower(BatteryChargerPower.CHARGER_POWER_NAME).to(BatteryChargerPower.Factory.class);
        bindChargerPower(HysteresisChargerPower.CHARGER_POWER_NAME).to(HysteresisChargerPower.Factory.class);

        addControllerListenerBinding().to(BatteryChargerStateListener.class);

        bind(ChargerPower.Factory.class).to(CompositeChargerPowerFactory.class);

        EvConfigGroup evConfig = EvConfigGroup.get(getConfig());
        if (evConfig.getInitialSocBehavior().equals(InitialSocBehavior.UpdateAfterIteration)) {
            addEventHandlerBinding().to(BatteryUpdater.class);
            addControllerListenerBinding().to(BatteryUpdater.class);
        }
    }

    private void prepareMapBinder() {
        mapBinder = MapBinder.newMapBinder(binder(), String.class, ChargerPower.Factory.class);
    }

    private LinkedBindingBuilder<ChargerPower.Factory> bindChargerPower(String name) {
        return mapBinder.addBinding(name);
    }

    @Provides
    @Singleton
    BatteryChargerPower.Factory provideBatteryChargerPowerFactory(EvConfigGroup evConfig, EventsManager eventsManager) {
        return new BatteryChargerPower.Factory(evConfig.getChargeTimeStep(), eventsManager);
    }

    @Provides
    @Singleton
    HysteresisChargerPower.Factory provideHysteresisBatteryChargerPowerFactory(EvConfigGroup evConfig,
            EventsManager eventsManager) {
        return new HysteresisChargerPower.Factory(evConfig.getChargeTimeStep(), eventsManager);
    }

    @Provides
    @Singleton
    CompositeChargerPowerFactory provideCompositeChargerPowerFactory(DefaultChargerPower.Factory defaultFactory,
            Map<String, Provider<ChargerPower.Factory>> factories) {
        return new CompositeChargerPowerFactory(defaultFactory, factories);
    }

    @Provides
    @Singleton
    BatteryChargerStateListener provideBatteryChargerStateListener(OutputDirectoryHierarchy outputDirectoryHierarchy,
            EventsManager eventsManager) {
        return new BatteryChargerStateListener(outputDirectoryHierarchy, eventsManager);
    }

    @Provides
    @Singleton
    BatteryUpdater provideUpdater(ChargingInfrastructureSpecification infrastructure) {
        return new BatteryUpdater(infrastructure);
    }
}
