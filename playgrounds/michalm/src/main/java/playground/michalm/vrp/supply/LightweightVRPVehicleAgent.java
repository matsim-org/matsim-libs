package playground.michalm.vrp.supply;

import java.util.*;

import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.model.Request.ReqStatus;
import pl.poznan.put.vrp.dynamic.data.schedule.*;
import pl.poznan.put.vrp.dynamic.data.schedule.Schedule.*;
import playground.michalm.vrp.data.network.*;
import playground.mzilske.withinday.*;


public class LightweightVRPVehicleAgent
    implements RealAgent
{
    private Vehicle vrpVehicle;
    private ShortestPath[][] shortestPaths;


    public LightweightVRPVehicleAgent(Vehicle vrpVehicle, ShortestPath[][] shortestPaths)
    {
        this.vrpVehicle = vrpVehicle;
        this.shortestPaths = shortestPaths;
    }


    private boolean firstSimStep = true;


    @Override
    public void doSimStep(World world)
    {
        Schedule schedule = vrpVehicle.schedule;

        if (firstSimStep) {
            firstSimStep = false;
            world.getActivityPlane().startDoing(
                    ScheduleActivityBehaviour.createWaitingBeforeSchedule(schedule));
        }
        else {
            ScheduleStatus status = schedule.getStatus();
            
            if (status.isCompleted() || status.isUnplanned()) {
                world.done();
                return;
            }
            
            Task task = schedule.nextTask();

            if (task == null) {//now the ScheduleStatus is changed COMPLETED
                world.getActivityPlane().startDoing(
                        ScheduleActivityBehaviour.createWaitingAfterSchedule(schedule));
                return;
            }

            switch (task.getType()) {
                case DRIVE:
                    world.getRoadNetworkPlane().startDriving(
                            VRPDrivingBehaviour.createDrivingAlongArc((DriveTask)task,
                                    shortestPaths, world.getTime()));
                    break;

                case SERVE:
                    world.getActivityPlane().startDoing(
                            StayTaskActivityBehaviour
                                    .createServeTaskActivityBehaviour((ServeTask)task));
                    break;

                case WAIT:
                    world.getActivityPlane().startDoing(
                            StayTaskActivityBehaviour
                                    .createWaitTaskActivityBehaviour((WaitTask)task));
                    break;

                default:
                    throw new IllegalStateException();
            }
        }

        // route.unplanned
        // route.planned
        //
        // route.started => for each request:
        // - request.veh_dispatched
        // - request.started
        // - [request.performed]
        //
        // route.completed

//        if (!status.le(ScheduleStatus.PLANNED)) {
//            ActivityBehavior vrpActivityBehaviour = VRPActivityBehaviour
//                    .createWaitingBeforeStartingRoute(schedule);
//            world.getActivityPlane().startDoing(vrpActivityBehaviour);
//        }
//        else if (status.isStarted()) {
//            Request req = route.getCurrentRequest();
//
//            if (req == null) {
//                if (arrived) {
//                    arrived = false;
//
//                    world.getActivityPlane().startDoing(
//                            VRPActivityBehaviour.createWaitingAfterCompetingRoute(route));
//                }
//                else {
//                    arrived = true;
//
//                    double time = world.getTime();
//
//                    List<Request> reqs = route.getRequests();
//                    Request lastServiced = reqs.get(reqs.size() - 1);
//                    MATSimVertex fromVertex = (MATSimVertex)lastServiced.toVertex;
//
//                    MATSimVertex toVertex = (MATSimVertex)route.vehicle.depot.vertex;
//
//                    world.getRoadNetworkPlane().startDriving(
//                            VRPDrivingBehaviour.createDrivingAlongArc(fromVertex, toVertex,
//                                    shortestPaths, time, route));
//                }
//            }
//            else if (req.status == ReqStatus.VEHICLE_DISPATCHED) {
//
//                // if (req.arrivalTime < time) {// TODO this may be ERROR-PRONE!!!
//                // // XXX the moment of arrival is be very changeable!
//                if (arrived) {
//                    arrived = false;
//                    world.getActivityPlane().startDoing(
//                            VRPActivityBehaviour.createWaitingBeforeServicingRequest(req));
//                }
//                else {
//                    arrived = true;
//
//                    double time = world.getTime();
//
//                    MATSimVertex fromVertex = null;
//
//                    int currReqIdx = route.getCurrentReqIdx();
//
//                    if (currReqIdx == 0) {
//                        fromVertex = (MATSimVertex)vrpVehicle.depot.getVertex();
//                    }
//                    else {
//                        fromVertex = (MATSimVertex)route.getRequests().get(currReqIdx - 1).toVertex;
//                    }
//
//                    world.getRoadNetworkPlane().startDriving(
//                            VRPDrivingBehaviour.createDrivingAlongArc(fromVertex,
//                                    (MATSimVertex)req.fromVertex, shortestPaths, time, route));
//                }
//            }
//            else if (req.status == ReqStatus.STARTED) {
//                world.getActivityPlane().startDoing(
//                        VRPActivityBehaviour.createServicingRequest(req));
//            }
//            else {// request is PERFORMED
//                world.getActivityPlane().startDoing(
//                        VRPActivityBehaviour.createWaitingAfterServicingRequest(req));
//            }
//        }
//        else { // route.isCompleted()
//            world.done();
//        }
    }
}
