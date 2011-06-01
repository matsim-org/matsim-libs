package playground.michalm.vrp.supply;

import java.util.*;

import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.model.Request.ReqStatus;
import playground.michalm.vrp.data.*;
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


    private boolean arrived = false;


    @Override
    public void doSimStep(World world)
    {
        Route route = vrpVehicle.route;

        // route.unplanned
        // route.planned
        //
        // route.started => for each request:
        // - request.veh_dispatched
        // - request.started
        // - [request.performed]
        //
        // route.completed

        if (!route.hasBeenStarted()) {
            VRPActivityBehaviour vrpActivityBehaviour = VRPActivityBehaviour
                    .createWaitingBeforeStartingRoute(route);
            world.getActivityPlane().startDoing(vrpActivityBehaviour);
        }
        else if (route.isStarted()) {
            Request req = route.getCurrentRequest();

            if (req == null) {
                if (arrived) {
                    arrived = false;

                    world.getActivityPlane().startDoing(
                            VRPActivityBehaviour.createWaitingAfterCompetingRoute(route));
                }
                else {
                    arrived = true;

                    double time = world.getTime();
    
                    List<Request> reqs = route.getRequests();
                    Request lastServiced = reqs.get(reqs.size() - 1);
                    MATSimVertex fromVertex = (MATSimVertex)lastServiced.toVertex;
    
                    MATSimVertex toVertex = (MATSimVertex)route.vehicle.depot.vertex;
    
                    world.getRoadNetworkPlane().startDriving(
                            VRPDrivingBehaviour.createDrivingAlongArc(fromVertex, toVertex,
                                    shortestPaths, time, route));
                }
            }
            else if (req.status == ReqStatus.VEHICLE_DISPATCHED) {

                // if (req.arrivalTime < time) {// TODO this may be ERROR-PRONE!!!
                // // XXX the moment of arrival is be very changeable!
                if (arrived) {
                    arrived = false;
                    world.getActivityPlane().startDoing(
                            VRPActivityBehaviour.createWaitingBeforeServicingRequest(req));
                }
                else {
                    arrived = true;

                    double time = world.getTime();

                    MATSimVertex fromVertex = null;

                    int currReqIdx = route.getCurrentReqIdx();

                    if (currReqIdx == 0) {
                        fromVertex = (MATSimVertex)vrpVehicle.depot.getVertex();
                    }
                    else {
                        fromVertex = (MATSimVertex)route.getRequests().get(currReqIdx - 1).toVertex;
                    }

                    world.getRoadNetworkPlane().startDriving(
                            VRPDrivingBehaviour.createDrivingAlongArc(fromVertex,
                                    (MATSimVertex)req.fromVertex, shortestPaths, time, route));
                }
            }
            else if (req.status == ReqStatus.STARTED) {
                world.getActivityPlane().startDoing(
                        VRPActivityBehaviour.createServicingRequest(req));
            }
            else {// request is PERFORMED
                world.getActivityPlane().startDoing(
                        VRPActivityBehaviour.createWaitingAfterServicingRequest(req));
            }
        }
        else { // route.isCompleted()
            world.done();
        }
    }
}
