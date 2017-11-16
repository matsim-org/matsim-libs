// code by jph
package playground.clruch.io.fleet;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.collections.QuadTree;

import ch.ethz.idsc.queuey.util.GlobalAssert;
import ch.ethz.idsc.tensor.Tensors;
import playground.clruch.dispatcher.core.AVStatus;
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

<<<<<<< HEAD
    public static void of(DayTaxiRecord dayTaxiRecord, Network network, MatsimStaticDatabase db, //
            StorageUtils storageUtils) {

        // final int MAXTIME = 180000; // TODO magic const take this end time from the last info in the file...
//        final int MAXTIME = (int) Long.parseLong(dayTaxiRecord.lastTimeStamp)/1000;
        final int MAXTIME = dayTaxiRecord.getNow(dayTaxiRecord.lastTimeStamp);
        final int TIMESTEP = 10;
=======
    public static void of(List<DayTaxiRecord> dayTaxiRecords, Network network, MatsimStaticDatabase db, //
            List<File> outputFolders) {
>>>>>>> master

        final double[] networkBounds = NetworkUtils.getBoundingBox(network.getNodes().values());
        final QuadTree<Link> quadTree = new QuadTree<>( //
                networkBounds[0], networkBounds[1], networkBounds[2], networkBounds[3]);

        System.out.println("INFO bounding box = " + Tensors.vectorDouble(networkBounds));
        // ---
        for (Link link : db.getLinkInteger().keySet())
            quadTree.put(link.getCoord().getX(), link.getCoord().getY(), link);

<<<<<<< HEAD
        int dropped = 0;
        int cancelledRequests = 0;
        int requestIndex = 0;
        // NavigableMap<Integer, Integer> requestMap = new TreeMap<>();
        for (int now = 0; now < MAXTIME; now += TIMESTEP) {
            if (now % 10000 == 0)
                System.out.println("now=" + now);
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
//                    vc.avStatus = AVStatus.DRIVETOCUSTOMER; // TODO change this hack later...  = taxiStamp.avStatus;
                    vc.avStatus = taxiStamp.avStatus;

                    // Parse requests
                    RequestContainerUtils rcParser = new RequestContainerUtils(taxiTrail);
                    RequestStatus requestStatus = RequestStatusParser.parseRequestStatus(now, taxiTrail);
                    // System.out.println("Parsing RequestStatus for vehicle " + vehicleIndex + ": " + requestStatus.toString());
                    taxiTrail.setRequestStatus(now, requestStatus);

                    // Create requestContainer if there is any requests
                    if (requestStatus != RequestStatus.EMPTY && requestStatus != RequestStatus.CANCELLED) {
                        // System.out.println("Trying to populate RequestContainer of vehicle: " + vehicleIndex + " at time: " + now);
                        RequestContainer rc = rcParser.populate(now, requestIndex, quadTree, db);
                        GlobalAssert.that(Objects.nonNull(rc.submissionTime));
                        simulationObject.requests.add(rc);
                    } else if (requestStatus == RequestStatus.CANCELLED) {
                        // System.out.println("Abort populating requestContainer.");
                        cancelledRequests++;
=======
        // Iterate through all dayTaxiRecords
        int iteration = 0;
        for (DayTaxiRecord dayTaxiRecord : dayTaxiRecords) {

            System.out.println("\nINFO processing: " + outputFolders.get(iteration).getName().substring(0, 10));
            StorageUtils storageUtils = new StorageUtils(outputFolders.get(iteration));

            final int MAXTIME = dayTaxiRecord.getNow(dayTaxiRecord.lastTimeStamp);
            final int TIMESTEP = 10;

            int dropped = 0;
            int cancelledRequests = 0;
            int requestIndex = 0;
            // NavigableMap<Integer, Integer> requestMap = new TreeMap<>();
            for (int now = 0; now < MAXTIME; now += TIMESTEP) {
                if (now % 10000 == 0)
                    System.out.println("INFO processing timestep = " + now + "\r");
                SimulationObject simulationObject = new SimulationObject();
                simulationObject.now = now;
                simulationObject.vehicles = new ArrayList<>();
                simulationObject.requests = new ArrayList<>();
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

                        // Check if there is valid requests and populate requestContainer
                        RequestContainerUtils rcParser = new RequestContainerUtils(taxiTrail);
                        if (rcParser.isValidRequest(now)) {
                            RequestStatus requestStatus = taxiStamp.requestStatus;
                            // System.out.println("Parsing RequestStatus for vehicle " + vehicleIndex + ": " + requestStatus.toString());

                            if (requestStatus != RequestStatus.CANCELLED) {
                                RequestContainer rc = rcParser.populate(now, requestIndex, quadTree, db);
                                simulationObject.requests.add(rc);
                            } else if (requestStatus == RequestStatus.CANCELLED) {
                                cancelledRequests++;
                            }
                        }
                        GlobalAssert.that(Objects.nonNull(vc.avStatus));
                        simulationObject.vehicles.add(vc);
                    } catch (Exception exception) {
                        System.err.println("WARN failed to convert vehicle " + vehicleIndex + " at time: " + now);
                        ++dropped;
>>>>>>> master
                    }
                }
                // sorting should be obsolete, since already sorted
                SimulationObjects.sortVehiclesAccordingToIndex(simulationObject);
                new StorageSubscriber(storageUtils).handle(simulationObject);
            }
            ++iteration;
            System.out.println("INFO dropped total: " + dropped);
            System.out.println("INFO total requests: " + requestIndex);
            System.out.println("INFO canceled requests: " + cancelledRequests);
        }
    }
<<<<<<< HEAD

}
=======
}
>>>>>>> master
