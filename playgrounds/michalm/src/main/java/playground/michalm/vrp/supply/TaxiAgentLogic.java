package playground.michalm.vrp.supply;

import org.matsim.api.core.v01.*;
import org.matsim.core.api.experimental.events.*;
import org.matsim.core.events.*;
import org.matsim.core.mobsim.framework.*;

import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.schedule.*;
import pl.poznan.put.vrp.dynamic.data.schedule.Schedule.ScheduleStatus;
import playground.michalm.dynamic.*;
import playground.michalm.vrp.data.network.*;
import playground.michalm.vrp.data.network.ShortestPath.SPEntry;
import playground.michalm.vrp.demand.*;
import playground.michalm.vrp.sim.*;


public class TaxiAgentLogic
    implements DynAgentLogic
{
    private VRPSimEngine vrpSimEngine;
    private ShortestPath[][] shortestPaths;

    private Vehicle vrpVehicle;
    private DynAgent agent;

    private Request currentRequest;


    public TaxiAgentLogic(Vehicle vrpVehicle, ShortestPath[][] shortestPaths,
            VRPSimEngine vrpSimEngine)
    {
        this.vrpVehicle = vrpVehicle;
        this.shortestPaths = shortestPaths;
        this.vrpSimEngine = vrpSimEngine;
    }


    @Override
    public DynActivity init(DynAgent adapterAgent)
    {
        this.agent = adapterAgent;
        return createBeforeScheduleActivity();
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


    public void scheduleUpdated()
    {
        agent.scheduleUpdated();
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
            // TODO: maybe in General DVRP but not here in simple TAXI...
            // optimizer.optimize();
            //
            // however, here just a simple update of this schedule
            vrpSimEngine.updateScheduleBeforeNextTask(vrpVehicle, now);
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
        currentRequest = ((ServeTask)task).getRequest();

        // serve the customer
        MobsimAgent passenger = ((TaxiCustomer)currentRequest.getCustomer()).getPassanger();
        Id currentLinkId = passenger.getCurrentLinkId();

        if (currentLinkId != agent.getCurrentLinkId()) {
            throw new IllegalStateException("Passanger and taxi on different links!");
        }

        if (vrpSimEngine.getMobsim().unregisterAdditionalAgentOnLink(passenger.getId(),
                currentLinkId) == null) {
            throw new RuntimeException("Passenger id=" + passenger.getId()
                    + "is not waiting for taxi");
        }

        // event handling
        EventsManager events = vrpSimEngine.getMobsim().getEventsManager();
        EventsFactoryImpl evFac = (EventsFactoryImpl)events.getFactory();
        events.processEvent(evFac.createPersonEntersVehicleEvent(now, passenger.getId(),
                agent.getId(), agent.getId()));

        return TaxiTaskActivity.createServeActivity(task);
    }


    // ========================================================================================

    private TaxiLeg createLegWithPassenger(DriveTask driveTask, int realDepartTime,
            final Request request)
    {
        SPEntry path = shortestPaths[driveTask.getFromVertex().getId()][driveTask.getToVertex()
                .getId()].getSPEntry(realDepartTime);

        Id destinationLinkId = ((MATSimVertex)driveTask.getToVertex()).getLink().getId();

        return new TaxiLeg(path, destinationLinkId) {
            @Override
            public void endLeg(double now)
            {
                MobsimAgent passenger = ((TaxiCustomer)request.getCustomer()).getPassanger();

                // deliver the passenger
                EventsManager events = vrpSimEngine.getMobsim().getEventsManager();
                EventsFactoryImpl evFac = (EventsFactoryImpl)events.getFactory();
                events.processEvent(evFac.createPersonLeavesVehicleEvent(now, passenger.getId(),
                        agent.getId(), agent.getId()));

                passenger.notifyTeleportToLink(passenger.getDestinationLinkId());
                passenger.endLegAndAssumeControl(now);
            }
        };
    }


    private TaxiLeg createLeg(DriveTask driveTask, int realDepartTime)
    {
        SPEntry path = shortestPaths[driveTask.getFromVertex().getId()][driveTask.getToVertex()
                .getId()].getSPEntry(realDepartTime);

        Id destinationLinkId = ((MATSimVertex)driveTask.getToVertex()).getLink().getId();

        return new TaxiLeg(path, destinationLinkId);
    }
}
