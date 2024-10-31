package org.matsim.dsim.simulation.pt;

import org.matsim.core.mobsim.framework.Steppable;
import org.matsim.dsim.simulation.net.SimLink;
import org.matsim.dsim.simulation.net.SimNetwork;
import org.matsim.dsim.simulation.net.SimVehicle;

import java.util.PriorityQueue;

public class DummyPtEngine implements Steppable {

    private final PriorityQueue<VehicleAtStop> stoppingVehicles = new PriorityQueue<>();

    DummyPtEngine(SimNetwork simNetwork) {
        // This is probably something smarter, but for now, we can do it like this I guess
        for (var link : simNetwork.getLinks().values()) {
            link.addLeaveHandler(this::handleLeave);
        }
    }

    /**
     * This would be analogue to TransitQLink::handleTransitStop
     */
    private SimLink.OnLeaveQueueInstruction handleLeave(SimVehicle vehicle, SimLink link, double now) {

        if (vehicle instanceof PtVehicle ptVehicle) {

            if (isNotStopping(ptVehicle)) return SimLink.OnLeaveQueueInstruction.MoveToBuffer;

            // adjust exit time to account for boarding delay
            var expectedBoardingTime = ptVehicle.startBoarding();
            var earliestExitTime = ptVehicle.getEarliestExitTime();
            ptVehicle.setEarliestExitTime(earliestExitTime + expectedBoardingTime);

            var stop = ptVehicle.getNextTransitStop();
            if (stop.getIsBlockingLane()) {

                return SimLink.OnLeaveQueueInstruction.BlockQueue;
            } else {
                link.popVehicle(); // ignore result as we have already cast the vehicle.
                stoppingVehicles.add(new VehicleAtStop(ptVehicle, link));
                return SimLink.OnLeaveQueueInstruction.RemoveVehicle;
            }
        }
        return SimLink.OnLeaveQueueInstruction.MoveToBuffer;
    }

    private boolean isNotStopping(PtVehicle ptVehicle) {
        var currentLinkId = ptVehicle.getCurrentRouteElement();
        var nextStop = ptVehicle.getNextTransitStop();
        return !currentLinkId.equals(nextStop.getLinkId());
    }

    @Override
    public void doSimStep(double now) {
        while (true) {
            var nextVehicleAtStop = stoppingVehicles.peek();
            if (nextVehicleAtStop == null) break;

            // TODO think about whether this is > or >= now
            if (nextVehicleAtStop.ptVehicle().getEarliestExitTime() <= now) {
                nextVehicleAtStop = stoppingVehicles.poll();
                nextVehicleAtStop.link().pushVehicle(nextVehicleAtStop.ptVehicle(), SimLink.LinkPosition.QEnd, now);
            } else {
                break;
            }
        }
    }

    record VehicleAtStop(PtVehicle ptVehicle, SimLink link) {
    }
}
