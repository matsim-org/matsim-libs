package playground.clruch.dispatcher.core;

import java.util.Map;

import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;

import playground.sebhoerl.avtaxi.data.AVVehicle;

public class RebalanceEvent extends Event {

    Link dest;
    AVVehicle avVehicle;

    public RebalanceEvent(Link dest, AVVehicle avVehicle, double time) {
        super(time);
        this.dest = dest;
        this.avVehicle = avVehicle;
    }

    @Override
    public String getEventType() {
        return "rebalance";
    }

    @Override
    public Map<String, String> getAttributes() {
        Map<String, String> attr = super.getAttributes();
        attr.put("dest", dest.getId().toString());
        attr.put("operator", avVehicle.getOperator().getId().toString());
        return attr;
    }
}
