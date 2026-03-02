package org.matsim.contrib.ev.extensions.placement;

import org.matsim.contrib.ev.infrastructure.ChargerSpecification;

public class ChargerPlacement {
    static public final String REMOVABLE_ATTRIBUTE = "chargerPlacement:isRemovable";

    private ChargerPlacement() {
    }

    static public void setRemovable(ChargerSpecification charger, boolean isRemovable) {
        charger.getAttributes().putAttribute(REMOVABLE_ATTRIBUTE, isRemovable);
    }

    static public boolean isRemovable(ChargerSpecification charger) {
        Boolean isRemovable = (Boolean) charger.getAttributes().getAttribute(REMOVABLE_ATTRIBUTE);
        return isRemovable != null && isRemovable;
    }
}
