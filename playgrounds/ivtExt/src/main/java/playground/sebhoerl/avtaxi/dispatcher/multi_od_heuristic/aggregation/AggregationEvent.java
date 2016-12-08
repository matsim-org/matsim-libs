package playground.sebhoerl.avtaxi.dispatcher.multi_od_heuristic.aggregation;

import org.matsim.api.core.v01.events.Event;
import playground.sebhoerl.avtaxi.passenger.AVRequest;

import java.util.Map;

public class AggregationEvent extends Event {
    final private AVRequest master;
    final private AVRequest slave;

    public AggregationEvent(AVRequest master, AVRequest slave, double time) {
        super(time);

        this.master = master;
        this.slave = slave;
    }

    @Override
    public String getEventType() {
        return "ODRSAggregation";
    }

    @Override
    public Map<String, String> getAttributes() {
        Map<String, String> attr = super.getAttributes();
        attr.put("master", master.getPassenger().getId().toString());
        attr.put("slave", slave.getPassenger().getId().toString());
        return attr;
    }
}
