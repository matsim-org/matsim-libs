package playground.michalm.vrp.supply;

import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.*;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

import pl.poznan.put.vrp.dynamic.data.schedule.*;
import playground.michalm.vrp.data.network.*;
import playground.mzilske.withinday.*;


class VRPDrivingBehaviour
    implements DrivingBehavior
{
    private DriveTask driveTask;
    private Iterator<Id> linkIdIter;


    private VRPDrivingBehaviour(DriveTask driveTask, Path path)
    {
        this.driveTask = driveTask;

        List<Id> linkIds = new ArrayList<Id>(path.links.size() + 1);

        if (path != ShortestPath.ZERO_PATH) {
            for (Link l : path.links) {
                linkIds.add(l.getId());
            }

            linkIds.add( ((MATSimVertex)driveTask.getToVertex()).getLink().getId());
        }

        this.linkIdIter = linkIds.iterator();
    }


    @Override
    public void doSimStep(DrivingWorld drivingWorld)
    {
        if (drivingWorld.requiresAction()) {
            if (linkIdIter.hasNext()) {
                Id linkId = linkIdIter.next();
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
        int plannedArrivalTime = driveTask.getEndTime();
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


    static VRPDrivingBehaviour createDrivingAlongArc(DriveTask driveTask,
            ShortestPath[][] shortestPaths, double realDepartTime)
    {
        Path path = shortestPaths[driveTask.getFromVertex().getId()][driveTask.getToVertex()
                .getId()].getPath((int)realDepartTime);

        return new VRPDrivingBehaviour(driveTask, path);
    }

    // void fireMonitoringEvent()
    // {
    // // MonitoringEvent monitoringEvent = new MonitoringEvent(type, (int)cause.getTime(),
    // // vrpVehAgent.getVrpVehicle(), req);
    // //
    // // VRPVehicleEvent event = new VRPVehicleEventImpl(cause.getTime(), vrpVehAgent, cause,
    // // monitoringEvent);
    //
    // // first notify (internal to MATSim) VRPVehicleEventHandlers, then notify (external to
    // // MATSim) MonitoringListener
    // // eventsManager.processEvent(event);
    // // vrpSimEngine.notifyMonitoringListeners(monitoringEvent);
    // }
}
