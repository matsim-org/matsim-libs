package playground.sebhoerl.avtaxi.dispatcher;

import org.matsim.api.core.v01.events.Event;
import playground.sebhoerl.avtaxi.data.AVVehicle;

import java.util.Map;

public class AVVehicleAssignmentEvent extends Event {
    final private AVVehicle vehicle;

    public AVVehicleAssignmentEvent(AVVehicle vehicle, double time) {
        super(time);
        this.vehicle = vehicle;
    }

    @Override
    public String getEventType() {
        return "AVVehicleAssignment";
    }

    @Override
    public Map<String, String> getAttributes() {
        Map<String, String> attr = super.getAttributes();
        attr.put("vehicle", vehicle.getId().toString());
        attr.put("operator", vehicle.getOperator().getId().toString());
        return attr;
    }
}
