package playground.michalm.vrp.sim;

/*
import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.api.experimental.events.*;
import org.matsim.core.api.experimental.events.handler.*;

import pl.poznan.put.vrp.dynamic.data.*;
import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.model.Request.ReqStatus;
import pl.poznan.put.vrp.dynamic.data.model.Route.RtStatus;
import pl.poznan.put.vrp.dynamic.data.model.Route;
import pl.poznan.put.vrp.dynamic.monitoring.*;
import pl.poznan.put.vrp.dynamic.monitoring.MonitoringEvent.METype;
import playground.michalm.vrp.events.*;
import playground.michalm.vrp.supply.*;
*/

/**
 * 
 * !!!!!!!!!!!!!!
 * 
 * THIS CODE IS NOW USELESS - THIS APROACH WAS OVERCOMPLICATED...
 * 
 * !!!!!!!!!!!!!!
 * 
 * Functionality:<br/>
 * 1. monitors {@link VRPVehicleAgent}s in the network<br/>
 * 2. updates {@link VRPData}<br/>
 * 3. fires corresponding {@link VRPVehicleEvent}s<br/>
 * 4. updates {@link VRPVehicleAgent}s (especially their plans)<br/>
 * <p>
 * According to my understanding of the MATSim code (and the current state of the MATSim code, May 11'):
 * <p>
 * - {@link ActivityEndEvent} and {@link AgentDepartureEvent} are fired almost one by one;<br/>
 *    a. generally, re-routing etc. can be carried after any of them;<br/>
 *    b. the difference is that on {@link ActivityEndEvent} the current {@link PlanElement} is this
 *       {@link Activity} whereas on {@link AgentDepartureEvent} the current {@link PlanElement} is
 *       the following {@link Leg}
 * <p>  
 * - {@link AgentArrivalEvent} and {@link ActivityStartEvent} are fired almost one by one;<br/>
 *    a. generally, re-calculating the {@link Activity} endTime can be carried after any of them;<br/>
 *    b. the difference is that on {@link AgentArrivalEvent} the current {@link PlanElement} is this
 *       {@link Leg} whereas on {@link ActivityStartEvent} the current {@link PlanElement} is
 *       the following {@link Activity}
 *
 *
 * FIXME Currently, there is no reaction to any shifts in the schedule. This version only updates
 * statuses of requests & routes, and it fires events. So it works like a monitoring (observer)
 * without any capability of influencing on the simulation. 
 *
 *
 * @author michalm
 *
 */
/*public class VRPVehicleAgentMonitoring
    implements AgentDepartureEventHandler, AgentArrivalEventHandler, ActivityStartEventHandler,
    ActivityEndEventHandler
{
    private EventsManager eventsManager;
    private Map<Id, VRPVehicleAgent> personIdToVehAgent;

    private VRPSimEngine vrpSimEngine;


    public VRPVehicleAgentMonitoring(EventsManager eventsManager,
            Map<Id, VRPVehicleAgent> personIdToVehAgent, VRPSimEngine vrpSimEngine)
    {
        this.eventsManager = eventsManager;
        this.personIdToVehAgent = personIdToVehAgent;
        this.vrpSimEngine = vrpSimEngine;

        eventsManager.addHandler(this);
    }


    @Override
    public void handleEvent(AgentArrivalEvent event)
    {
        VRPVehicleAgent vrpVehAgent = personIdToVehAgent.get(event.getPersonId());

        if (vrpVehAgent == null) {
            return;
        }

        // compare actual vs. planned arrival time:
        int actualArrivalTime = (int)event.getTime();// I assume this is the current time!XXX
        int plannedArrivalTime;

        Route route = vrpVehAgent.getVrpVehicle().route;
        Request currReq = route.getCurrentRequest();

        if (currReq == null) {// vehicle arrived at the destination depot
            plannedArrivalTime = route.endTime;
            route.endTime = actualArrivalTime;

            route.setStatus(RtStatus.COMPLETED);

            fireMonitoringEvent(METype.ROUTE_COMPLETED, event, vrpVehAgent);
        }
        else {
            plannedArrivalTime = currReq.arrivalTime;
            currReq.arrivalTime = actualArrivalTime;

            fireMonitoringEvent(METype.VEH_ARRIVED, event, vrpVehAgent);
        }

        int timeDiff = actualArrivalTime - plannedArrivalTime;

        // TODO if ANY difference in time - update vrpData; "Scheduler" would be very useful here
        // XXX [VRPSimEngine/Optimizer should be responsible for this]

        // TODO update the agent's current Leg, i.e. travelTime, arrivalTime
        // XXX [is it really necessary? I think - NOT! (at least not now...)] - ask MATSim
        // developers!

        // if the difference is significant - consider reoptimization
        // BUT: reoptimization can be only done after each time step in VRPSimEngine
        vrpSimEngine.timeDifferenceOccurred(vrpVehAgent.getVrpVehicle(), timeDiff);
    }


    @Override
    public void handleEvent(ActivityStartEvent event)
    {
        VRPVehicleAgent vrpVehAgent = personIdToVehAgent.get(event.getPersonId());

        if (vrpVehAgent == null) {
            return;
        }

        // find out the request
        Route route = vrpVehAgent.getVrpVehicle().route;
        Request req = route.getCurrentRequest();

        if (req == null) {// starts waiting at a depot
        }
        else {// starts servicing a client
            req.status = ReqStatus.STARTED;

            fireMonitoringEvent(METype.REQ_STARTED, event, vrpVehAgent, req);
        }
    }


    @Override
    public void handleEvent(ActivityEndEvent event)
    {
        VRPVehicleAgent vrpVehAgent = personIdToVehAgent.get(event.getPersonId());

        if (vrpVehAgent == null) {
            return;
        }

        // update VRPData
        Route route = vrpVehAgent.getVrpVehicle().route;
        Request req = route.getCurrentRequest();

        if (req == null) {// ends waiting at a depot
        }
        else {// ends servicing a client
            req.status = ReqStatus.PERFORMED;

            fireMonitoringEvent(METype.REQ_PERFORMED, event, vrpVehAgent, req);
        }
    }


    @Override
    public void handleEvent(AgentDepartureEvent event)
    {
        VRPVehicleAgent vrpVehAgent = personIdToVehAgent.get(event.getPersonId());

        if (vrpVehAgent == null) {
            return;
        }

        // update VRPData
        Route route = vrpVehAgent.getVrpVehicle().route;
        Request prevReq = route.getCurrentRequest();

        if (prevReq == null) {// starting out at the depot
            route.setStatus(Route.RtStatus.STARTED);
            route.setCurrentRequestIdx(0);
            Request req = route.getCurrentRequest();
            req.status = Request.ReqStatus.VEHICLE_DISPATCHED;

            fireMonitoringEvent(METype.ROUTE_STARTED, event, vrpVehAgent);
        }
        else {
            route.nextRequestAsCurrent();
            Request req = route.getCurrentRequest();

            if (req != null) {// not last request
                req.status = Request.ReqStatus.VEHICLE_DISPATCHED;
            }

            fireMonitoringEvent(METype.VEH_DEPARTURED, event, vrpVehAgent);
        }
    }


    private void fireMonitoringEvent(METype type, PersonEvent cause, VRPVehicleAgent vrpVehAgent)
    {
        fireMonitoringEvent(type, cause, vrpVehAgent, null);
    }


    private void fireMonitoringEvent(METype type, PersonEvent cause, VRPVehicleAgent vrpVehAgent,
            Request req)
    {
        MonitoringEvent monitoringEvent = new MonitoringEvent(type, (int)cause.getTime(),
                vrpVehAgent.getVrpVehicle(), req);

        VRPVehicleEvent event = new VRPVehicleEventImpl(cause.getTime(), vrpVehAgent, cause,
                monitoringEvent);

        // first notify (internal to MATSim) VRPVehicleEventHandlers, then notify (external to
        // MATSim) MonitoringListener
        eventsManager.processEvent(event);
        vrpSimEngine.notifyMonitoringListeners(monitoringEvent);
    }


    @Override
    public void reset(int iteration)
    {}
}*/
