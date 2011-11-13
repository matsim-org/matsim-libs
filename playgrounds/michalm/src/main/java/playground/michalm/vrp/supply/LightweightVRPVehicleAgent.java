package playground.michalm.vrp.supply;

import org.matsim.api.core.v01.*;
import org.matsim.core.api.experimental.events.*;
import org.matsim.core.events.*;
import org.matsim.core.mobsim.framework.*;
import org.matsim.ptproject.qsim.interfaces.*;

import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.schedule.*;
import pl.poznan.put.vrp.dynamic.data.schedule.Schedule.ScheduleStatus;
import playground.michalm.vrp.data.network.*;
import playground.michalm.vrp.demand.*;
import playground.michalm.withinday.*;


public class LightweightVRPVehicleAgent
    implements RealAgent
{
    private Vehicle vrpVehicle;
    private ShortestPath[][] shortestPaths;


    public LightweightVRPVehicleAgent(Vehicle vrpVehicle, ShortestPath[][] shortestPaths, Netsim netsim)
    {
        this.vrpVehicle = vrpVehicle;
        this.shortestPaths = shortestPaths;
        this.netsim = netsim;
    }


    private boolean firstSimStep = true;

    private Request currentRequest = null;

    private Netsim netsim = null;
    
    @Override
    public void doSimStep(World world)
    {
        Schedule schedule = vrpVehicle.getSchedule();

        if (firstSimStep) {
            firstSimStep = false;
            world.getActivityPlane().startDoing(
                    ScheduleActivityBehaviour.createWaitingBeforeSchedule(vrpVehicle));
        }
        else {
            ScheduleStatus status = schedule.getStatus();

            if (status.isCompleted() || status.isUnplanned()) {
                world.done();
                return;
            }

            Task task = schedule.nextTask();
            
            System.err.println("NEXT TASK: " + task);
            

            if (task == null) {// now the ScheduleStatus is changed COMPLETED
                world.getActivityPlane().startDoing(
                        ScheduleActivityBehaviour.createWaitingAfterSchedule(vrpVehicle));
                return;
            }

            switch (task.getType()) {
                case DRIVE:
                    if (currentRequest != null) {
                        world.getRoadNetworkPlane().startDriving(
                                TaxiDrivingBehaviour.createDeliveryBehavior((DriveTask)task,
                                        shortestPaths, world.getTime(), currentRequest));
                        currentRequest = null;
                    }
                    else {
                        world.getRoadNetworkPlane().startDriving(
                                VRPDrivingBehaviour.createDrivingAlongArc((DriveTask)task,
                                        shortestPaths, world.getTime()));
                    }
                    break;

                case SERVE:
                    currentRequest = ((ServeTask)task).getRequest();
                    
                    MobsimAgent passenger = ((TaxiCustomer)currentRequest.getCustomer()).getPassanger();
                    
                    Id currentLinkId = passenger.getCurrentLinkId();

                    NetsimLink link = netsim.getNetsimNetwork().getNetsimLink(currentLinkId);
                    
                    if (link.unregisterAdditionalAgentOnLink(passenger.getId()) == null) {
                        throw new RuntimeException("Passenger id=" + passenger.getId() +
                                "is not waiting for taxi");
                    }

                    //To run the following I need the taxi "vehicle"
                    
//                    EventsManager events = netsim.getEventsManager() ;
//                    EventsFactoryImpl evFac = (EventsFactoryImpl) events.getFactory() ;

//                    events.processEvent(evFac.createPersonEntersVehicleEvent(drivingWorld.getTime(),
//                            passenger.getId(), vehicle.getId(), getId()));


                    //just wait a few minutes to simulate that getting into taxi and giving a
                    //destination do not happen immediately
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
    }
}
