package playground.michalm.vrp.supply;

import org.matsim.api.core.v01.*;
import org.matsim.core.api.experimental.events.*;
import org.matsim.core.events.*;
import org.matsim.core.mobsim.framework.*;
import org.matsim.ptproject.qsim.interfaces.*;

import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.schedule.*;
import pl.poznan.put.vrp.dynamic.data.schedule.Schedule.ScheduleStatus;
import playground.michalm.dynamic.*;
import playground.michalm.vrp.data.network.*;
import playground.michalm.vrp.data.network.ShortestPath.SPEntry;
import playground.michalm.vrp.demand.*;


public class TaxiAgentLogic
    implements DynAgentLogic
{
    private Vehicle vrpVehicle;
    private ShortestPath[][] shortestPaths;

    private DynAgent agent;


    public TaxiAgentLogic(Vehicle vrpVehicle, ShortestPath[][] shortestPaths, Netsim netsim)
    {
        this.vrpVehicle = vrpVehicle;
        this.shortestPaths = shortestPaths;
        this.netsim = netsim;
    }


    private Request currentRequest = null;

    private Netsim netsim = null;


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
            ((TaxiLeg)oldLeg).notifyLegEnded(now);// handle passenger-related stuff
        }

        // TODO: NOTIFY OPTIMIZER !!!!
        // because some delays in DRIVE task may influence further execution of the schedule

        // VRPSimEngine

        // // compare actual vs. planned arrival time:
        // int actualArrivalTime = (int)drivingWorld.getTime();// I assume this is the current
        // time!XXX
        // int plannedArrivalTime = driveTask.getEndTime();
        // int timeDiff = actualArrivalTime - plannedArrivalTime;
        //
        // // TODO if ANY difference in time - update vrpData; "Scheduler" would be very useful here
        // // XXX [VRPSimEngine/Optimizer should be responsible for this]
        //
        // // if the difference is significant - consider reoptimization
        // // BUT: reoptimization can be only done after each time step in VRPSimEngine
        //
        // // TODO
        // // vrpSimEngine.timeDifferenceOccurred(route.vehicle, timeDiff);

        scheduleNextTask(now);
    }


    public void scheduleUpdated()
    {
        agent.scheduleUpdated();
    }


    private void scheduleNextTask(double now)
    {
        Schedule schedule = vrpVehicle.getSchedule();

        if (schedule.getStatus() == ScheduleStatus.UNPLANNED) {
            agent.startActivity(createAfterScheduleActivity(), now);// FINAL ACTIVITY
            return;
        }

        int time = (int)now;
        Task task = schedule.nextTask(time);

        if (schedule.getStatus() == ScheduleStatus.COMPLETED) {
            agent.startActivity(createAfterScheduleActivity(), now);// FINAL ACTIVITY
            return;
        }

        if (task.getSchedule().getVehicle().getId() == 5) {
            System.err.println("NEXT TASK: " + task + " time="
                    + now);
            System.out.println(".");
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
                    agent.startLeg(
                            createLegWithPassenger((DriveTask)task, shortestPaths, time,
                                    currentRequest), now);
                    currentRequest = null;
                }
                else {
                    agent.startLeg(createLeg((DriveTask)task, shortestPaths, time), now);
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

        if (netsim.unregisterAdditionalAgentOnLink(passenger.getId(), currentLinkId) == null) {
            throw new RuntimeException("Passenger id=" + passenger.getId()
                    + "is not waiting for taxi");
        }

        // event handling
        EventsManager events = netsim.getEventsManager();
        EventsFactoryImpl evFac = (EventsFactoryImpl)events.getFactory();
        events.processEvent(evFac.createPersonEntersVehicleEvent(now, passenger.getId(),
                agent.getId(), agent.getId()));

        return TaxiTaskActivity.createServeActivity(task);
    }


    // ========================================================================================

    private TaxiLeg createLegWithPassenger(DriveTask driveTask, ShortestPath[][] shortestPaths,
            int realDepartTime, final Request request)
    {
        SPEntry path = shortestPaths[driveTask.getFromVertex().getId()][driveTask.getToVertex()
                .getId()].getSPEntry(realDepartTime);

        Id destinationLinkId = ((MATSimVertex)driveTask.getToVertex()).getLink().getId();

        return new TaxiLeg(path, destinationLinkId) {
            @Override
            public void notifyLegEnded(double now)
            {
                MobsimAgent passenger = ((TaxiCustomer)request.getCustomer()).getPassanger();

                // deliver the passenger
                EventsManager events = netsim.getEventsManager();
                EventsFactoryImpl evFac = (EventsFactoryImpl)events.getFactory();
                events.processEvent(evFac.createPersonLeavesVehicleEvent(now, passenger.getId(),
                        agent.getId(), agent.getId()));

                passenger.notifyTeleportToLink(passenger.getDestinationLinkId());
                passenger.endLegAndAssumeControl(now);
            }
        };
    }


    private TaxiLeg createLeg(DriveTask driveTask, ShortestPath[][] shortestPaths,
            int realDepartTime)
    {
        SPEntry path = shortestPaths[driveTask.getFromVertex().getId()][driveTask.getToVertex()
                .getId()].getSPEntry(realDepartTime);

        Id destinationLinkId = ((MATSimVertex)driveTask.getToVertex()).getLink().getId();

        return new TaxiLeg(path, destinationLinkId);
    }
}
