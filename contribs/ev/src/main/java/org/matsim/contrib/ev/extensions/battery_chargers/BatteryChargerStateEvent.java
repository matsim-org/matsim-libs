package org.matsim.contrib.ev.extensions.battery_chargers;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.contrib.ev.infrastructure.Charger;

/**
 * This event is used to analyze the state of charge of a battery-based charger.
 * 
 * @author sebhoerl
 */
public class BatteryChargerStateEvent extends Event {
    static public final String EVENT_TYPE = "battery charger state";

    private final Id<Charger> chargerId;
    private final double state_kWh;
    private final double soc;

    public BatteryChargerStateEvent(double time, Id<Charger> chargerId, double state_kWh, double soc) {
        super(time);

        this.chargerId = chargerId;
        this.state_kWh = state_kWh;
        this.soc = soc;
    }

    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }

    public Id<Charger> getChargerId() {
        return chargerId;
    }

    public double getState_kWh() {
        return state_kWh;
    }

    public double getSoc() {
        return soc;
    }

    public Map<String, String> getAttributes() {
        Map<String, String> attributes = super.getAttributes();
        attributes.put("charger", chargerId.toString());
        attributes.put("state_kWh", String.valueOf(state_kWh));
        attributes.put("soc", String.valueOf(soc));
        return attributes;
    }

    public static BatteryChargerStateEvent convert(GenericEvent genericEvent) {
        Map<String, String> attributes = genericEvent.getAttributes();
        double time = genericEvent.getTime();
        Id<Charger> chargerId = Id.create(attributes.get("charger"), Charger.class);
        double state_kWh = Double.parseDouble("state_kWh");
        double soc = Double.parseDouble("soc");
        return new BatteryChargerStateEvent(time, chargerId, state_kWh, soc);
    }
}
