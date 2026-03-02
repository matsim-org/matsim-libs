package org.matsim.contrib.ev.extensions.battery_chargers;

import java.util.Map;

import org.matsim.contrib.ev.charging.ChargerPower;
import org.matsim.contrib.ev.charging.DefaultChargerPower;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;

import com.google.common.base.Preconditions;
import com.google.inject.Provider;

/**
 * This is a utility class that either generates simple battery-based or
 * hysteresis-based battery ChargerPower logic based on the attributes that are
 * defined in the chargers.
 * 
 * @author sebhoerl
 */
public class CompositeChargerPowerFactory implements ChargerPower.Factory {
    static public final String CHARGER_ATTRIBUTE = "chargerPower";

    private final DefaultChargerPower.Factory defaultFactory;
    private final Map<String, Provider<ChargerPower.Factory>> factories;

    public CompositeChargerPowerFactory(DefaultChargerPower.Factory defaultFactory,
            Map<String, Provider<ChargerPower.Factory>> factories) {
        this.factories = factories;
        this.defaultFactory = defaultFactory;
    }

    @Override
    public ChargerPower create(ChargerSpecification charger) {
        String selection = (String) charger.getAttributes().getAttribute(CHARGER_ATTRIBUTE);

        if (selection != null) {
            Provider<ChargerPower.Factory> factory = factories.get(selection);
            Preconditions.checkNotNull(factory, "Cannot find factory for ChargerPower called: " + selection);
            return factory.get().create(charger);
        } else {
            return defaultFactory.create(charger);
        }
    }
}
