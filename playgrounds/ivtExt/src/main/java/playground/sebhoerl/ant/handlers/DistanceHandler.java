package playground.sebhoerl.ant.handlers;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.GenericEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.vehicles.Vehicle;

import playground.sebhoerl.ant.DataFrame;

public class DistanceHandler extends AbstractHandler implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, LinkEnterEventHandler, GenericEventHandler {
    final private Map<Id<Vehicle>, Integer> passengerCount = new HashMap();
    final private Network network;

    public DistanceHandler(DataFrame data, Network network) {
        super(data);
        this.network = network;
    }

    private void ensureVehicle(Id<Vehicle> vehicleId) {
        if (!passengerCount.containsKey(vehicleId)) passengerCount.put(vehicleId, 0);
    }

    private void increaseCount(Id<Vehicle> vehicleId) {
        ensureVehicle(vehicleId);
        passengerCount.put(vehicleId, passengerCount.get(vehicleId) + 1);
    }

    private void decreaseCount(Id<Vehicle> vehicleId) {
        ensureVehicle(vehicleId);
        passengerCount.put(vehicleId, passengerCount.get(vehicleId) - 1);
    }

    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        if (!data.isOrdinaryPerson(event.getPersonId())) return;
        increaseCount(event.getVehicleId());
    }

    @Override
    public void handleEvent(PersonLeavesVehicleEvent event) {
        if (!data.isOrdinaryPerson(event.getPersonId())) return;
        decreaseCount(event.getVehicleId());
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        ensureVehicle(event.getVehicleId());

        double linkLength = network.getLinks().get(event.getLinkId()).getLength();
        data.passengerDistance += linkLength * passengerCount.get(event.getVehicleId());
        data.vehicleDistance += linkLength;

        if (event.getVehicleId().toString().startsWith("av_")) {
            data.avPassengerDistance += linkLength * passengerCount.get(event.getVehicleId());
            data.avVehicleDistance += linkLength;

            if (passengerCount.get(event.getVehicleId()) == 0) {
                data.avEmptyRideDistance += linkLength;
            }
        }
    }

    @Override
    protected void finish() {}

    @Override
    public void handleEvent(GenericEvent event) {
        if (event.getEventType().equals("AVTransit")) {
            data.avDistances.add(Double.parseDouble(event.getAttributes().get("distance")));
        }
    }
}
