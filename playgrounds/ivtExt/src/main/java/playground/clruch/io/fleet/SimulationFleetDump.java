// code by jph
package playground.clruch.io.fleet;

import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Objects;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.collections.QuadTree;

import ch.ethz.idsc.queuey.util.GlobalAssert;
import ch.ethz.idsc.tensor.Tensors;
import playground.clruch.dispatcher.core.RequestStatus;
import playground.clruch.net.MatsimStaticDatabase;
import playground.clruch.net.RequestContainer;
import playground.clruch.net.RequestContainerUtils;
import playground.clruch.net.SimulationObject;
import playground.clruch.net.SimulationObjects;
import playground.clruch.net.StorageSubscriber;
import playground.clruch.net.StorageUtils;
import playground.clruch.net.VehicleContainer;

enum SimulationFleetDump {
    ;

    public static void of(DayTaxiRecord dayTaxiRecord, Network network, MatsimStaticDatabase db,//
            StorageUtils storageUtils) {

        // final int MAXTIME = 180000; // TODO magic const take this end time from the last info in the file... 
        final int MAXTIME = dayTaxiRecord.getNow(dayTaxiRecord.lastTimeStamp);
        final int TIMESTEP = 10;

        final double[] networkBounds = NetworkUtils.getBoundingBox(network.getNodes().values());
        final QuadTree<Link> quadTree = new QuadTree<>( //
                networkBounds[0], networkBounds[1], networkBounds[2], networkBounds[3]);

        System.out.println("bounding box = " + Tensors.vectorDouble(networkBounds));
        // ---
        for (Link link : db.getLinkInteger().keySet())
            quadTree.put(link.getCoord().getX(), link.getCoord().getY(), link);

        int dropped = 0;
        int canceledRequests = 0;
        for (int now = 0; now < MAXTIME; now += TIMESTEP) {
            if (now % 10000 == 0)
                System.out.println("now=" + now);
            int requestIndex = 0;
            SimulationObject simulationObject = new SimulationObject();
            simulationObject.now = now;
            simulationObject.vehicles = new ArrayList<>();
            // simulationObject.requests are already initialize in SimulationObject

            for (int vehicleIndex = 0; vehicleIndex < dayTaxiRecord.size(); ++vehicleIndex) {
            	
                // Check and propagate offservice status
                // dayTaxiRecord.get(vehicleIndex).check_offservice(now);
                
            	// Get corresponding dayTaxiRecord entry according to time now
                TaxiTrail taxiTrail = dayTaxiRecord.get(vehicleIndex);
            	Entry<Integer, TaxiStamp> dayTaxiRecordEntry = taxiTrail.interp(now);
            	TaxiStamp taxiStamp = dayTaxiRecordEntry.getValue();
                try {
                    Coord xy = db.referenceFrame.coords_fromWGS84.transform(taxiStamp.gps);
                    // getClosest(...) may fail if xy is outside boundingbox
                    Link center = quadTree.getClosest(xy.getX(), xy.getY());
                    int linkIndex = db.getLinkIndex(center);

                    // ---
                    VehicleContainer vc = new VehicleContainer();
                    vc.vehicleIndex = vehicleIndex;
                    vc.linkIndex = linkIndex;
                    vc.avStatus = taxiStamp.avStatus;
                    
                    RequestContainer rc = new RequestContainer();
                    RequestContainerUtils rcMapper = new RequestContainerUtils(now, linkIndex, taxiTrail);
                    rcMapper.populate(rc);
                    if (rc.requestStatus == RequestStatus.REQUEST) {
                        requestIndex++;
                        rc.requestIndex = requestIndex;
                    }
                    else if (rc.requestStatus == RequestStatus.CANCELED)
                        canceledRequests++;
                    
                    GlobalAssert.that(Objects.nonNull(vc.avStatus));
                    simulationObject.vehicles.add(vc);
                    simulationObject.requests.add(rc);
                } catch (Exception exception) {
                    System.err.println("failed to convert vehicle " + vehicleIndex + " at time: " + now);
                    ++dropped;
                }
            }
            // sorting should be obsolete, since already sorted
            SimulationObjects.sortVehiclesAccordingToIndex(simulationObject);
            new StorageSubscriber(storageUtils).handle(simulationObject);
        }
        System.out.println("dropped total: " + dropped);
        System.out.println("canceled requests: " + canceledRequests);

    }

}
