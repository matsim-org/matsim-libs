package playground.michalm.vrp.supply;

import java.util.*;

import org.matsim.api.core.v01.*;

import pl.poznan.put.vrp.dynamic.data.schedule.*;
import playground.michalm.vrp.data.network.*;
import playground.michalm.vrp.data.network.ShortestPath.SPEntry;
import playground.michalm.withinday.*;

import com.google.common.collect.*;


class VRPDrivingBehaviour
    implements DrivingBehavior
{
    private DriveTask driveTask;
    private Iterator<Id> linkIdIter;


    public VRPDrivingBehaviour(DriveTask driveTask, SPEntry entry)
    {
        this.driveTask = driveTask;
        this.linkIdIter = Iterators.forArray(entry.linkIds);
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
            }
        }
    }

    
    @Override
    public void drivingEnded(DrivingWorld drivingWorld)
    {
        // compare actual vs. planned arrival time:
        int actualArrivalTime = (int)drivingWorld.getTime();// I assume this is the current time!XXX
        int plannedArrivalTime = driveTask.getEndTime();
        int timeDiff = actualArrivalTime - plannedArrivalTime;

        // TODO if ANY difference in time - update vrpData; "Scheduler" would be very useful here
        // XXX [VRPSimEngine/Optimizer should be responsible for this]

        // if the difference is significant - consider reoptimization
        // BUT: reoptimization can be only done after each time step in VRPSimEngine

        // TODO
        // vrpSimEngine.timeDifferenceOccurred(route.vehicle, timeDiff);
    }


    static VRPDrivingBehaviour createDrivingAlongArc(DriveTask driveTask,
            ShortestPath[][] shortestPaths, double realDepartTime)
    {
        SPEntry path = shortestPaths[driveTask.getFromVertex().getId()][driveTask.getToVertex()
                .getId()].getSPEntry((int)realDepartTime);

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
