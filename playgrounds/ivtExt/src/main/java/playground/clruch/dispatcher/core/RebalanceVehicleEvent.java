package playground.clruch.dispatcher.core;

import java.util.Map;

import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;

import playground.sebhoerl.avtaxi.data.AVVehicle;

public class RebalanceVehicleEvent extends Event {

    Link dest;
    AVVehicle avVehicle;

    public RebalanceVehicleEvent(Link dest, AVVehicle avVehicle, double time) {
        super(time);
        this.dest = dest;
        this.avVehicle = avVehicle;
    }

    @Override
    public String getEventType() {
        String string = getClass().getSimpleName();
        return string.substring(0, string.length() - 5);
    }

    public String getVehicleIdString() {
        return null;
    }

    @Override
    public Map<String, String> getAttributes() {
        Map<String, String> attr = super.getAttributes();
        attr.put("dest", dest.getId().toString());
        attr.put("vehicle", avVehicle.getId().toString());
        return attr;
    }
}
