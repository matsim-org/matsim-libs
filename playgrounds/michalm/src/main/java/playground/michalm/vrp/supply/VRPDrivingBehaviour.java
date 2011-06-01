package playground.michalm.vrp.supply;

import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.*;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.model.Route.RtStatus;
import pl.poznan.put.vrp.dynamic.monitoring.MonitoringEvent.METype;
import playground.michalm.vrp.data.*;
import playground.michalm.vrp.data.network.*;
import playground.michalm.vrp.sim.*;
import playground.mzilske.withinday.*;


class VRPDrivingBehaviour
    implements DrivingBehavior
{
    private Iterator<Id> linkIdIter;
    private Route route;


    private StringBuilder getSB(String header)
    {
        return new StringBuilder(header).append(" veh_").append(route.vehicle.id + " ");
    }


    VRPDrivingBehaviour(MATSimVertex toVertex, Path path, Route route)
    {
        this.route = route;
        
        
//        StringBuilder sb = getSB("NEW_ROUTE").append(" req_" + route.getCurrentRequest().id)
//        .append(" v_" + route.getCurrentRequest().fromVertex.getId()).append("\n\t\t: ");

        List<Id> linkIds = new ArrayList<Id>(path.links.size() + 1);

        if (path != ShortestPath.ZERO_PATH) {
            for (Link l : path.links) {
                linkIds.add(l.getId());
//                sb.append(l.getId() + "[" + l.getFromNode().getId() + ":" + l.getToNode().getId()
//                        + "]->");
            }

            linkIds.add(toVertex.getLink().getId());
        }

        linkIdIter = linkIds.iterator();
        Link l = toVertex.getLink();
//        System.err.println(sb.append(
//                l.getId() + "[" + l.getFromNode().getId() + ":" + l.getToNode().getId() + "]END")
//                .toString());

        if (route.getCurrentRequest() != null) {
            System.err.println("DispatchedReq_" + route.getCurrentRequest().id + " Veh_" + route.id);
        }
        else {
            System.err.println("Back to depot: Veh_" + route.id);
        }
    }


    @Override
    public void doSimStep(DrivingWorld drivingWorld)
    {
        if (drivingWorld.requiresAction()) {
            if (linkIdIter.hasNext()) {
                Id linkId = linkIdIter.next();

                //System.err.println(getSB("NEXT_LINK").append(linkId).toString());

                drivingWorld.nextTurn(linkId);
            }
            else {
                drivingWorld.park();
                onFinish(drivingWorld);
            }
        }
    }


    void onFinish(DrivingWorld world)
    {
        // compare actual vs. planned arrival time:
        int actualArrivalTime = (int)world.getTime();// I assume this is the current time!XXX
        int plannedArrivalTime;

        Request req = route.getCurrentRequest();

        if (req == null) {
            plannedArrivalTime = route.endTime;
            route.endTime = actualArrivalTime;

            // TODO problem of "interpretation", as for now, let's assume that the route
            // ends at this moment; [to be improved after changes in Route class - introduction
            // of Waiting and Traveling]

        }
        else {
            plannedArrivalTime = req.arrivalTime;
            req.arrivalTime = actualArrivalTime;

            fireMonitoringEvent(METype.VEH_ARRIVED, req);
        }

        int timeDiff = actualArrivalTime - plannedArrivalTime;

        // TODO if ANY difference in time - update vrpData; "Scheduler" would be very useful here
        // XXX [VRPSimEngine/Optimizer should be responsible for this]

        // TODO update the agent's current Leg, i.e. travelTime, arrivalTime
        // XXX [is it really necessary? I think - NOT! (at least not now...)] - ask MATSim
        // developers!

        // if the difference is significant - consider reoptimization
        // BUT: reoptimization can be only done after each time step in VRPSimEngine

        // TODO
        // vrpSimEngine.timeDifferenceOccurred(route.vehicle, timeDiff);
    }


    static VRPDrivingBehaviour createDrivingAlongArc(MATSimVertex fromVertex,
            MATSimVertex toVertex, ShortestPath[][] shortestPaths, double departTime, Route route)
    {
        ShortestPath shortestPath = shortestPaths[fromVertex.getId()][toVertex.getId()];
        Path path = shortestPath.getPath((int)departTime);

        return new VRPDrivingBehaviour(toVertex, path, route);
    }


    void fireMonitoringEvent(METype type, Route route)
    {}


    void fireMonitoringEvent(METype type, Request req)
    {
        // MonitoringEvent monitoringEvent = new MonitoringEvent(type, (int)cause.getTime(),
        // vrpVehAgent.getVrpVehicle(), req);
        //
        // VRPVehicleEvent event = new VRPVehicleEventImpl(cause.getTime(), vrpVehAgent, cause,
        // monitoringEvent);

        // first notify (internal to MATSim) VRPVehicleEventHandlers, then notify (external to
        // MATSim) MonitoringListener
        // eventsManager.processEvent(event);
        // vrpSimEngine.notifyMonitoringListeners(monitoringEvent);
    }
}
