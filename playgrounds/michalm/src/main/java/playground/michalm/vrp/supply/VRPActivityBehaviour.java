package playground.michalm.vrp.supply;

import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.model.Request.*;
import pl.poznan.put.vrp.dynamic.data.model.Route.*;
import pl.poznan.put.vrp.dynamic.monitoring.MonitoringEvent.METype;
import playground.mzilske.withinday.*;


abstract class VRPActivityBehaviour
    implements ActivityBehavior
{
    private String activityType;

    private int counter = 0;

    VRPActivityBehaviour(String activityType)
    {
        this.activityType = activityType;
        System.err.println(activityType);
    }


    @Override
    public void doSimStep(ActivityWorld world)
    {
        if (world.getTime() >= getEndTime()) {
            world.stopActivity();
            onFinish(world);

            counter++;
            if (counter > 1) {
                System.err.println("Second onFinish() for: " + this);
                System.err.flush();
            }
        }
    }


    abstract double getEndTime();


    // for overriding
    void onFinish(ActivityWorld world)
    {
        // fireEvents?
    }


    @Override
    public String getActivityType()
    {
        return activityType;
    }


    static VRPActivityBehaviour createWaitingBeforeStartingRoute(final Route route)
    {
        return new VRPActivityBehaviour("BeforeRt_" + route.id) {
            @Override
            double getEndTime()
            {
                return route.beginTime - 1;
            }


            @Override
            void onFinish(ActivityWorld world)
            {
                route.setStatus(Route.RtStatus.STARTED);
                route.setCurrentRequestIdx(0);
                Request req = route.getCurrentRequest();
                req.status = Request.ReqStatus.VEHICLE_DISPATCHED;
                fireMonitoringEvent(METype.ROUTE_STARTED, route);
            }
        };
    }


    static VRPActivityBehaviour createWaitingAfterCompetingRoute(final Route route)
    {
        return new VRPActivityBehaviour("AfterRt_" + route.id) {
            @Override
            double getEndTime()
            {
                return route.endTime;//without -1!
            }
            
            @Override
            void onFinish(ActivityWorld world)
            {
                route.setStatus(RtStatus.COMPLETED);
                fireMonitoringEvent(METype.ROUTE_COMPLETED, route);
            }
        };
    }


    static VRPActivityBehaviour createWaitingBeforeServicingRequest(final Request request)
    {
        return new VRPActivityBehaviour("BeforeReq_" + request.id) {
            @Override
            double getEndTime()
            {
                return request.startTime - 1;
            }


            @Override
            void onFinish(ActivityWorld world)
            {
                request.status = ReqStatus.STARTED;
                fireMonitoringEvent(METype.REQ_STARTED, request);
            }
        };
    }


    static VRPActivityBehaviour createWaitingAfterServicingRequest(final Request request)
    {
        return new VRPActivityBehaviour("AfterReq_" + request.id) {
            @Override
            double getEndTime()
            {
                return request.departureTime - 1;
            }


            @Override
            void onFinish(ActivityWorld world)
            {
                Route route = request.getRoute();
                route.nextRequestAsCurrent();
                Request req = route.getCurrentRequest();

                if (req != null) {// not last request
                    req.status = Request.ReqStatus.VEHICLE_DISPATCHED;
                }

                fireMonitoringEvent(METype.VEH_DEPARTURED, route);
            }
        };
    }


    static VRPActivityBehaviour createServicingRequest(final Request request)
    {
        return new VRPActivityBehaviour("DuringReq_" + request.id) {
            @Override
            double getEndTime()
            {
                return request.finishTime - 1;
            }


            @Override
            void onFinish(ActivityWorld world)
            {
                request.status = ReqStatus.PERFORMED;
                fireMonitoringEvent(METype.REQ_PERFORMED, request);
            }
        };
    }


    // private EventsManager eventsManager;
    // private VRPSimEngine vrpSimEngine;

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
