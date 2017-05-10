package playground.clruch.traveltimetracker;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import playground.clruch.utils.GlobalAssert;

public class AVTravelTimeTracker implements LinkEnterEventHandler, LinkLeaveEventHandler, VehicleLeavesTrafficEventHandler {
    final private Map<Id<Vehicle>, Double> enterTimes = new HashMap<>();
    final private Map<Id<Link>, LinkTravelTime> travelTimes = new HashMap<>();

    public class LinkTravelTime {
        public double travelTime = Double.NaN;
        public double updateTime = Double.NaN;
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        enterTimes.put(event.getVehicleId(), event.getTime());
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        Double enterTime = enterTimes.remove(event.getVehicleId());

        if (enterTime != null) {
            LinkTravelTime travelTime = travelTimes.get(event.getLinkId());
            if (travelTime == null)
                travelTime = new LinkTravelTime();

            travelTime.travelTime = event.getTime() - enterTime;
            travelTime.updateTime = event.getTime();

            travelTimes.put(event.getLinkId(), travelTime);
        }
    }

    @Override
    public void handleEvent(VehicleLeavesTrafficEvent event) {
        enterTimes.remove(event.getVehicleId());
    }

    @Override
    public void reset(int iteration) {
        for (LinkTravelTime travelTime : travelTimes.values()) {
            travelTime.updateTime = Double.NaN;
        }
    }

    public LinkTravelTime getLinkTravelTime(Id<Link> linkId) {
        LinkTravelTime travelTime = travelTimes.get(linkId);
        if (travelTime == null) {
            return new LinkTravelTime();
        } else {
            GlobalAssert.that(travelTime.travelTime >= 0.0);
            return travelTime;
        }
    };
}
