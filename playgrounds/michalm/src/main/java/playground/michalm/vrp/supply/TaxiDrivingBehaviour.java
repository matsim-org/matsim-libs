package playground.michalm.vrp.supply;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.ptproject.qsim.interfaces.Netsim;

import pl.poznan.put.vrp.dynamic.data.model.Request;
import pl.poznan.put.vrp.dynamic.data.schedule.DriveTask;
import playground.michalm.vrp.data.network.ShortestPath;
import playground.michalm.vrp.data.network.ShortestPath.SPEntry;
import playground.michalm.vrp.demand.TaxiCustomer;
import playground.michalm.withinday.DrivingWorld;


public class TaxiDrivingBehaviour
    extends VRPDrivingBehaviour
{
//    public enum DrivingType
//    {
//        PICK_UP, // heading towards a customer (no passanger aboard)
//        DELIVERY, // serving a customer (passanger(s?) aboard)
//        RETURN; // returning to a depot/rank
//    }


    public TaxiDrivingBehaviour(DriveTask driveTask, SPEntry entry)
    {
        super(driveTask, entry);
    }


    public static TaxiDrivingBehaviour createPickUpBehavior(DriveTask driveTask,
            ShortestPath[][] shortestPaths, double realDepartTime, Request request,
            final Netsim netsim)
    {
        SPEntry path = shortestPaths[driveTask.getFromVertex().getId()][driveTask.getToVertex()
                .getId()].getSPEntry((int)realDepartTime);

        final MobsimAgent passenger = ((TaxiCustomer)request.getCustomer()).getPassanger();

        return new TaxiDrivingBehaviour(driveTask, path) {
            @Override
            public void drivingEnded(DrivingWorld drivingWorld)
            {
                super.drivingEnded(drivingWorld);

                // pickup the passenger

                Id currentLinkId = passenger.getCurrentLinkId();

                if (netsim.unregisterAdditionalAgentOnLink(passenger.getCurrentLinkId(), currentLinkId) == null) {
                    throw new RuntimeException("Passenger id=" + passenger.getId() +
                            "is not waiting for taxi");
                }

//                EventsManager events = netsim.getEventsManager() ;
//                EventsFactoryImpl evFac = (EventsFactoryImpl) events.getFactory() ;

//                events.processEvent(evFac.createPersonEntersVehicleEvent(drivingWorld.getTime(),
//                        passenger.getId(), vehicle.getId(), getId()));
            }
        };
    }


    public static TaxiDrivingBehaviour createDeliveryBehavior(DriveTask driveTask,
            ShortestPath[][] shortestPaths, double realDepartTime, Request request)
    {
        SPEntry path = shortestPaths[driveTask.getFromVertex().getId()][driveTask.getToVertex()
                .getId()].getSPEntry((int)realDepartTime);

        final MobsimAgent passenger = ((TaxiCustomer)request.getCustomer()).getPassanger();

        return new TaxiDrivingBehaviour(driveTask, path) {
            @Override
            public void drivingEnded(DrivingWorld drivingWorld)
            {
                super.drivingEnded(drivingWorld);

                // deliver the passenger
//                events.processEvent( evFac.createPersonLeavesVehicleEvent(now, this.currentPassenger.getId(), this.vehicle.getId(), 
//                        this.getId() ) ) ;
                
                passenger.notifyTeleportToLink(passenger.getDestinationLinkId());
                passenger.endLegAndAssumeControl(drivingWorld.getTime());
            }
        };
    }


    public static TaxiDrivingBehaviour createReturnBehavior(DriveTask driveTask,
            ShortestPath[][] shortestPaths, double realDepartTime)
    {
        SPEntry path = shortestPaths[driveTask.getFromVertex().getId()][driveTask.getToVertex()
                .getId()].getSPEntry((int)realDepartTime);

        return new TaxiDrivingBehaviour(driveTask, path);
    }
}
