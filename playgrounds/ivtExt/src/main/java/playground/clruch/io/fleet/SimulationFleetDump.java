// code by jph
package playground.clruch.io.fleet;

import java.util.ArrayList;
import java.util.Objects;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.collections.QuadTree;

import ch.ethz.idsc.queuey.util.GlobalAssert;
import ch.ethz.idsc.tensor.Tensors;
import playground.clruch.net.MatsimStaticDatabase;
import playground.clruch.net.SimulationObject;
import playground.clruch.net.SimulationObjects;
import playground.clruch.net.StorageSubscriber;
import playground.clruch.net.VehicleContainer;

enum SimulationFleetDump {
    ;
    
    public static void of(DayTaxiRecord dayTaxiRecord, Network network, MatsimStaticDatabase db) {
        
        final int MAXTIME = 216000; // TODO magic const
        final int TIMESTEP = 10;

        final double[] networkBounds = NetworkUtils.getBoundingBox(network.getNodes().values());
        final QuadTree<Link> quadTree = new QuadTree<>( //
                networkBounds[0], networkBounds[1], networkBounds[2], networkBounds[3]);

        System.out.println("bounding box = " + Tensors.vectorDouble(networkBounds));
        // ---
        for (Link link : db.getLinkInteger().keySet())
            quadTree.put(link.getCoord().getX(), link.getCoord().getY(), link);

        int dropped = 0;
        for (int now = 0; now < MAXTIME; now += TIMESTEP) {
            if (now % 10000 == 0)
                System.out.println("now=" + now);
            SimulationObject simulationObject = new SimulationObject();
            simulationObject.now = now;
            simulationObject.vehicles = new ArrayList<>();

            for (int vehicleIndex = 0; vehicleIndex < dayTaxiRecord.size(); ++vehicleIndex) {
                TaxiStamp taxiStamp = dayTaxiRecord.get(vehicleIndex).interp(now);
                try {
                    Coord xy = db.referenceFrame.coords_fromWGS84.transform(taxiStamp.gps);
                    // getClosest(...) may fail if xy is outside boundingbox
                    Link center = quadTree.getClosest(xy.getX(), xy.getY());
                    // ---
                    VehicleContainer vc = new VehicleContainer();
                    vc.vehicleIndex = vehicleIndex;
                    vc.linkIndex = db.getLinkIndex(center);
                    vc.avStatus = taxiStamp.avStatus;
                    GlobalAssert.that(Objects.nonNull(vc.avStatus));
                    simulationObject.vehicles.add(vc);
                } catch (Exception exception) {
                    System.err.println("fail " + taxiStamp.gps);
                    ++dropped;
                }
            }
            // sorting should be obsolete, since already sorted
            SimulationObjects.sortVehiclesAccordingToIndex(simulationObject);
            new StorageSubscriber().handle(simulationObject);
        }
        System.out.println("dropped total " + dropped);

    }

}
