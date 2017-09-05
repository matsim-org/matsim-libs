package playground.clruch.trb18.analysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.vehicles.Vehicle;

import playground.sebhoerl.av_paper.BinCalculator;

public class DistanceHandler implements LinkEnterEventHandler, LinkLeaveEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler {
    final private Network network;
    final private DataFrame dataFrame;
    final private BinCalculator binCalculator;

    final private Map<Id<Vehicle>, LinkEnterEvent> enterEvents = new HashMap<>();
    final private Set<Id<Vehicle>> occupiedVehicles = new HashSet<>();

    public DistanceHandler(DataFrame dataFrame, BinCalculator binCalculator, Network network) {
        this.dataFrame = dataFrame;
        this.binCalculator = binCalculator;
        this.network = network;
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        if (event.getVehicleId().toString().startsWith("av_")) {
            enterEvents.put(event.getVehicleId(), event);
        }
    }

    @Override
    public void handleEvent(LinkLeaveEvent leaveEvent) {
        LinkEnterEvent enterEvent = enterEvents.get(leaveEvent.getVehicleId());

        if (enterEvent != null) {
            Link link = network.getLinks().get(enterEvent.getLinkId());
            boolean isOccupied = occupiedVehicles.contains(enterEvent.getVehicleId());

            for (BinCalculator.BinEntry entry : binCalculator.getBinEntriesNormalized(enterEvent.getTime(), leaveEvent.getTime())) {
                dataFrame.vehicleDistance.set(entry.getIndex(), dataFrame.vehicleDistance.get(entry.getIndex()) + entry.getWeight() * link.getLength());
                if (isOccupied) dataFrame.passengerDistance.set(entry.getIndex(), dataFrame.passengerDistance.get(entry.getIndex()) + entry.getWeight() * link.getLength());
            }
        }
    }

    /*@Override
    public void handleEvent(VehicleEntersTrafficEvent event) {
        if (event.getVehicleId().toString().startsWith("av_")) {
            enterEvents.put(event.getVehicleId(), new LinkEnterEvent(event.getTime(), event.getVehicleId(), event.getLinkId()));
        }
    }*/

    /*@Override
    public void handleEvent(VehicleLeavesTrafficEvent event) {
        enterEvents.remove(event.getVehicleId());
    }*/

    @Override
    public void reset(int iteration) {}

    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        if (event.getVehicleId().toString().startsWith("av_") && !event.getPersonId().toString().startsWith("av_")) {
            occupiedVehicles.add(event.getVehicleId());
        }
    }

    @Override
    public void handleEvent(PersonLeavesVehicleEvent event) {
        if (event.getVehicleId().toString().startsWith("av_") && !event.getPersonId().toString().startsWith("av_")) {
            occupiedVehicles.remove(event.getVehicleId());
        }
    }
}
