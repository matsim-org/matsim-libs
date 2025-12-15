package org.matsim.contrib.ev.extensions.placement;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.contrib.ev.strategic.access.ChargerAccess;

/**
 * Wraps around the base ChargerAccess to forbid access to blacklisted chargers
 */
public class ChargerPlacementAccess implements ChargerAccess {
    private final ChargerPlacementManager manager;
    private final ChargerAccess delegate;

    public ChargerPlacementAccess(ChargerPlacementManager manager, ChargerAccess delegate) {
        this.manager = manager;
        this.delegate = delegate;
    }

    @Override
    public boolean hasAccess(Person person, Charger charger) {
        if (manager.isBlacklisted(charger.getId())) {
            return false;
        }

        return delegate.hasAccess(person, charger);
    }

    @Override
    public boolean hasAccess(Person person, ChargerSpecification charger) {
        if (manager.isBlacklisted(charger.getId())) {
            return false;
        }

        return delegate.hasAccess(person, charger);
    }
}
