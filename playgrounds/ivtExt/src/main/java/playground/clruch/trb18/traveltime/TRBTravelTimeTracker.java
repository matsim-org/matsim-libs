package playground.clruch.trb18.traveltime;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

public class TRBTravelTimeTracker implements LinkEnterEventHandler, LinkLeaveEventHandler, VehicleLeavesTrafficEventHandler, MobsimBeforeSimStepListener {
    static private Logger logger = Logger.getLogger(TRBTravelTimeTracker.class);

    final private TravelTime delegate;
    final private Network network;

    final private Map<Id<Vehicle>, VehicleTrace> traces = new HashMap<>();
    final private Map<Id<Link>, Double> travelTimes = new HashMap<>();

    private double now;

    long numberOfEnterEvents = 0;
    long numberOfLeaveEvents = 0;

    public TRBTravelTimeTracker(Network network, TravelTime delegate) {
        this.delegate = delegate;
        this.network = network;

        for (Link link : network.getLinks().values()) {
            travelTimes.put(link.getId(), 0.0);
        }

        logger.info("Created instance: " + travelTimes.size() + " links");
    }

    @Override
    public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
        for (Link link : network.getLinks().values()) {
            travelTimes.put(link.getId(), delegate.getLinkTravelTime(link, e.getSimulationTime(), null, null));
        }

        for (VehicleTrace trace : traces.values()) {
            travelTimes.put(trace.linkId, Math.max(travelTimes.get(trace.linkId), e.getSimulationTime() - trace.enterTime));
        }

        logger.info("New step: enter = " + numberOfEnterEvents + ", leave = " + numberOfLeaveEvents);

        now = e.getSimulationTime();
        numberOfEnterEvents = 0;
        numberOfLeaveEvents = 0;
    }

    public double getTravelTime(Link link, double time) {
        if (time - now > 300.0) {
            return delegate.getLinkTravelTime(link, time, null, null);
        } else {
            return travelTimes.get(link.getId());
        }
    }

    private class VehicleTrace {
        public double enterTime;
        public Id<Link> linkId;

        public VehicleTrace(double enterTime, Id<Link> linkId) {
            this.enterTime = enterTime;
            this.linkId = linkId;
        }
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        traces.put(event.getVehicleId(), new VehicleTrace(event.getTime(), event.getLinkId()));
        numberOfEnterEvents++;
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        if (traces.remove(event.getVehicleId()) != null) {
            numberOfLeaveEvents++;
        }
    }

    @Override
    public void handleEvent(VehicleLeavesTrafficEvent event) {
        traces.remove(event.getVehicleId());
    }

    @Override
    public void reset(int iteration) {
        traces.clear();
    }
}
