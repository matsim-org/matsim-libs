package playground.michalm.vrp.taxi.taxicab;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsFactoryImpl;
import org.matsim.core.mobsim.framework.MobsimAgent;

import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.schedule.*;
import pl.poznan.put.vrp.dynamic.data.schedule.Schedule.ScheduleStatus;
import playground.michalm.dynamic.*;
import playground.michalm.vrp.data.model.TaxiCustomer;
import playground.michalm.vrp.data.network.*;
import playground.michalm.vrp.data.network.shortestpath.ShortestPath.SPEntry;
import playground.michalm.vrp.taxi.TaxiSimEngine;


public class TaxiAgentLogic
    implements DynAgentLogic
{
    private TaxiSimEngine taxiSimEngine;
    private MATSimVRPGraph vrpGraph;

    private Vehicle vrpVehicle;
    private DynAgent agent;

    private Request currentRequest;


    public TaxiAgentLogic(Vehicle vrpVehicle, MATSimVRPGraph vrpGraph, TaxiSimEngine taxiSimEngine)
    {
        this.vrpVehicle = vrpVehicle;
        this.vrpGraph = vrpGraph;
        this.taxiSimEngine = taxiSimEngine;
    }


    @Override
    public DynActivity init(DynAgent adapterAgent)
    {
        this.agent = adapterAgent;
        return createBeforeScheduleActivity();
    }


    @Override
    public DynAgent getDynAgent()
    {
        return agent;
    }


    @Override
    public void endActivityAndAssumeControl(DynActivity oldActivity, double now)
    {
        scheduleNextTask(now);
    }


    @Override
    public void endLegAndAssumeControl(DynLeg oldLeg, double now)
    {
        if (oldLeg instanceof TaxiLeg) {
            ((TaxiLeg)oldLeg).endLeg(now);// handle passenger-related stuff
        }

        scheduleNextTask(now);
    }


    public void schedulePossiblyChanged()
    {
        agent.update();
    }


    private void scheduleNextTask(double now)
    {
        Schedule schedule = vrpVehicle.getSchedule();
        ScheduleStatus status = schedule.getStatus();

        if (status == ScheduleStatus.UNPLANNED) {
            agent.startActivity(createAfterScheduleActivity(), now);// FINAL ACTIVITY
            return;
        }

        int time = (int)now;

        if (status == ScheduleStatus.STARTED) {
            taxiSimEngine.updateScheduleBeforeNextTask(vrpVehicle, now);
            taxiSimEngine.optimize(now);// TODO: this may be optional (depending on the algorithm)
        }

        Task task = schedule.nextTask();
        status = schedule.getStatus();// REFRESH status!!!

        if (status == ScheduleStatus.COMPLETED) {
            agent.startActivity(createAfterScheduleActivity(), now);// FINAL ACTIVITY
            return;
        }

        if (task.getSchedule().getVehicle().getId() == printVehId) {
            System.err.println("NEXT TASK: " + task + " time=" + now);
        }

        switch (task.getType()) {
            case DRIVE:
                // ======DEBUG PRINTOUTS======
                // DriveTask dt = (DriveTask)task;
                // Id fromLinkId = ((MATSimVertex)dt.getFromVertex()).getLink().getId();
                // Id toLinkId = ((MATSimVertex)dt.getToVertex()).getLink().getId();
                //
                // System.err.println("************");
                // System.err.println("Drive task: " + task + " for Veh: " + vrpVehicle.getId()
                // + " agent: " + agent.getId());
                // System.err.println("fromLink" + fromLinkId + " toLink: " + toLinkId);

                if (currentRequest != null) {
                    agent.startLeg(createLegWithPassenger((DriveTask)task, time, currentRequest),
                            now);
                    currentRequest = null;
                }
                else {
                    agent.startLeg(createLeg((DriveTask)task, time), now);
                }
                break;

            case SERVE:
                agent.startActivity(createServeActivity((ServeTask)task, now), now);

                break;

            case WAIT:
                agent.startActivity(TaxiTaskActivity.createWaitActivity((WaitTask)task), now);
                break;

            default:
                throw new IllegalStateException();
        }
    }


    private final int printVehId = 13;


    // ========================================================================================

    private DynActivity createBeforeScheduleActivity()
    {
        return new DynActivityImpl("Before schedule: " + vrpVehicle.getId(), -1) {

            @Override
            public double getEndTime()
            {
                Schedule s = vrpVehicle.getSchedule();

                switch (s.getStatus()) {
                    case PLANNED:
                        return s.getBeginTime();
                    case UNPLANNED:
                        return vrpVehicle.getT1();
                    default:
                        throw new IllegalStateException();
                }
            }
        };

    }


    private DynActivity createAfterScheduleActivity()
    {
        return new DynActivityImpl("After schedule: " + vrpVehicle.getId(),
                Double.POSITIVE_INFINITY);
    }


    // ========================================================================================

    // picking-up a passenger
    private TaxiTaskActivity createServeActivity(ServeTask task, double now)
    {
        currentRequest = task.getRequest();

        // serve the customer
        MobsimAgent passenger = ((TaxiCustomer)currentRequest.getCustomer()).getPassanger();
        Id currentLinkId = passenger.getCurrentLinkId();

        if (currentLinkId != agent.getCurrentLinkId()) {
            throw new IllegalStateException("Passanger and taxi on different links!");
        }

        // if
        // (taxiSimEngine.getMobsim().unregisterAdditionalAgentOnLink(passenger.getId(),currentLinkId)
        // == null) {
        if (taxiSimEngine.internalInterface.unregisterAdditionalAgentOnLink(passenger.getId(),
                currentLinkId) == null) {
            throw new RuntimeException("Passenger id=" + passenger.getId()
                    + "is not waiting for taxi");
        }

        // event handling
        EventsManager events = taxiSimEngine.getMobsim().getEventsManager();
        EventsFactoryImpl evFac = (EventsFactoryImpl)events.getFactory();
        events.processEvent(evFac.createPersonEntersVehicleEvent(now, passenger.getId(),
                agent.getId()));

        return TaxiTaskActivity.createServeActivity(task);
    }


    // ========================================================================================

    private TaxiLeg createLegWithPassenger(DriveTask driveTask, int realDepartTime,
            final Request request)
    {
        SPEntry path = vrpGraph.getShortestPath(driveTask.getFromVertex(), driveTask.getToVertex())
                .getSPEntry(realDepartTime);

        Id destinationLinkId = ((MATSimVertex)driveTask.getToVertex()).getLink().getId();

        return new TaxiLeg(path, destinationLinkId) {
            @Override
            public void endLeg(double now)
            {
                MobsimAgent passenger = ((TaxiCustomer)request.getCustomer()).getPassanger();

                // deliver the passenger
                EventsManager events = taxiSimEngine.getMobsim().getEventsManager();
                EventsFactoryImpl evFac = (EventsFactoryImpl)events.getFactory();
                events.processEvent(evFac.createPersonLeavesVehicleEvent(now, passenger.getId(),
                        agent.getId()));

                passenger.notifyTeleportToLink(passenger.getDestinationLinkId());
                passenger.endLegAndComputeNextState(now);
                TaxiAgentLogic.this.taxiSimEngine.internalInterface
                        .arrangeNextAgentState(passenger);
            }
        };
    }


    private TaxiLeg createLeg(DriveTask driveTask, int realDepartTime)
    {
        SPEntry path = vrpGraph.getShortestPath(driveTask.getFromVertex(), driveTask.getToVertex())
                .getSPEntry(realDepartTime);

        Id destinationLinkId = ((MATSimVertex)driveTask.getToVertex()).getLink().getId();

        return new TaxiLeg(path, destinationLinkId);
    }
}
