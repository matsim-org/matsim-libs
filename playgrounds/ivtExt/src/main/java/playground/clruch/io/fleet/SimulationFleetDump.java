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

    public static void of(DayTaxiRecord dayTaxiRecord, Network network, MatsimStaticDatabase db, //
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
        int requestIndex = 0;
        // NavigableMap<Integer, Integer> requestMap = new TreeMap<>();
        for (int now = 0; now < MAXTIME; now += TIMESTEP) {
            if (now % 10000 == 0)
                System.out.println("now=" + now);
            SimulationObject simulationObject = new SimulationObject();
            simulationObject.now = now;
            simulationObject.vehicles = new ArrayList<>();
            simulationObject.requests = new ArrayList<>();

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

                    // Parse requests
                    RequestContainerUtils rcParser = new RequestContainerUtils(taxiTrail);
                    RequestStatus requestStatus = rcParser.parseRequestStatus(now);
                    System.out.println("Parsing RequestStatus for vehicle " + vehicleIndex + ": " + requestStatus.toString());
                    taxiTrail.setRequestStatus(now, requestStatus);

                    // Create requestContainer if there is any requests
                    if (requestStatus != RequestStatus.EMPTY) {
                        int submissionTime = -1;
                        if (rcParser.isNewRequest(now, requestIndex, taxiTrail)) {
                            submissionTime = now;
                            requestIndex++;
                        } else {
                            System.out.println("Trying to find submission time for vehicle: " + vehicleIndex);
                            submissionTime = rcParser.findSubmissionTime(now);
                        }

                        // TODO Add Counter for cancelled Requests
                        RequestContainer rc = new RequestContainer();

                        // Populate RequestContainer
                        System.out.println("Trying to find from/to Coords of vehicle: " + vehicleIndex);
                        Coord from = rcParser.getCoordAt(now, RequestStatus.PICKUP);
                        if (Objects.nonNull(from)) {
                            from = db.referenceFrame.coords_fromWGS84.transform(from);
                            rc.fromLinkIndex = db.getLinkIndex(quadTree.getClosest(from.getX(), from.getY()));
                        }
                        Coord to = rcParser.getCoordAt(now, RequestStatus.DROPOFF);
                        if (Objects.nonNull(to)) {
                            to = db.referenceFrame.coords_fromWGS84.transform(to);
                            rc.fromLinkIndex = db.getLinkIndex(quadTree.getClosest(to.getX(), to.getY()));
                        }
                        rc.requestIndex = taxiTrail.interp(now).getValue().requestIndex;
                        rc.requestStatus = taxiTrail.interp(now).getValue().requestStatus;
                        rc.submissionTime = submissionTime;
                        simulationObject.requests.add(rc);
                    }

                    GlobalAssert.that(Objects.nonNull(vc.avStatus));
                    simulationObject.vehicles.add(vc);
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
        System.out.println("total requests: " + requestIndex);
        System.out.println("canceled requests: " + canceledRequests);

    }

}
