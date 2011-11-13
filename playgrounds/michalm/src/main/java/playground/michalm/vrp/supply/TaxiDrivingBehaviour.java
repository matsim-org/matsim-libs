package playground.michalm.vrp.supply;

import org.matsim.api.core.v01.*;
import org.matsim.core.api.experimental.events.*;
import org.matsim.core.events.*;
import org.matsim.core.mobsim.framework.*;
import org.matsim.ptproject.qsim.interfaces.*;

import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.schedule.*;
import playground.michalm.vrp.data.network.*;
import playground.michalm.vrp.data.network.ShortestPath.*;
import playground.michalm.vrp.demand.*;
import playground.michalm.withinday.*;


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

                if (netsim.getNetsimNetwork().getNetsimLink(currentLinkId)
                        .unregisterAdditionalAgentOnLink(passenger.getCurrentLinkId()) == null) {
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
