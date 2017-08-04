// code by jph
package playground.clruch.net;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.network.Link;

import playground.clruch.dispatcher.core.RoboTaxi;
import playground.clruch.export.AVStatus;
import playground.clruch.utils.GlobalAssert;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.passenger.AVRequest;

public class SimulationObjectCompiler {

    private final SimulationObject simulationObject;
    private final Map<String, VehicleContainer> vehicleMap = new HashMap<>();
    private final MatsimStaticDatabase db = MatsimStaticDatabase.INSTANCE;

    public static SimulationObjectCompiler create( //
            long now, String infoLine, int total_matchedRequests) {
        final MatsimStaticDatabase db = MatsimStaticDatabase.INSTANCE;
        SimulationObject simulationObject = new SimulationObject();
        simulationObject.iteration = db.getIteration();
        simulationObject.now = now;
        simulationObject.infoLine = infoLine;
        simulationObject.total_matchedRequests = total_matchedRequests;
        return new SimulationObjectCompiler(simulationObject);
    }

    private SimulationObjectCompiler(SimulationObject simulationObject) {
        this.simulationObject = simulationObject;
    }

    public void addRequests(Collection<AVRequest> avRequests) {
        avRequests.forEach(this::insertRequest);
    }

    private void insertRequest(AVRequest avRequest) {
        RequestContainer requestContainer = new RequestContainer();
        requestContainer.requestIndex = db.getRequestIndex(avRequest);
        requestContainer.fromLinkIndex = db.getLinkIndex(avRequest.getFromLink());
        requestContainer.submissionTime = avRequest.getSubmissionTime();
        requestContainer.toLinkIndex = db.getLinkIndex(avRequest.getToLink());
        simulationObject.requests.add(requestContainer);
    }

    public void addVehiclesWithCustomer(Map<AVVehicle, Link> map, Map<AVVehicle, Link> vehicleLocations) {
        for (Entry<AVVehicle, Link> entry : map.entrySet()) {
            VehicleContainer vehicleContainer = new VehicleContainer();
            AVVehicle avVehicle = entry.getKey();
            final String key = avVehicle.getId().toString();
            final Link fromLink = vehicleLocations.get(avVehicle);
            GlobalAssert.that(fromLink != null);
            vehicleContainer.vehicleIndex = db.getVehicleIndex(avVehicle);
            vehicleContainer.linkIndex = db.getLinkIndex(fromLink);
            vehicleContainer.avStatus = AVStatus.DRIVEWITHCUSTOMER;
            vehicleContainer.destinationLinkIndex = db.getLinkIndex(entry.getValue());
            vehicleMap.put(key, vehicleContainer);
        }
    }

    public void addRebalancingVehicles(Map<AVVehicle, Link> rebalancingVehicles, Map<AVVehicle, Link> vehicleLocations) {
        for (Entry<AVVehicle, Link> entry : rebalancingVehicles.entrySet()) {
            VehicleContainer vehicleContainer = new VehicleContainer();
            AVVehicle avVehicle = entry.getKey();
            final String key = avVehicle.getId().toString();
            final Link fromLink = vehicleLocations.get(avVehicle);
            vehicleContainer.vehicleIndex = db.getVehicleIndex(avVehicle);
            vehicleContainer.linkIndex = db.getLinkIndex(fromLink);
            vehicleContainer.avStatus = AVStatus.REBALANCEDRIVE;
            vehicleContainer.destinationLinkIndex = db.getLinkIndex(entry.getValue());
            vehicleMap.put(key, vehicleContainer);
        }
    }

    private void addDivertableVehicles(Collection<RoboTaxi> divertableVehicles, Map<AVVehicle, Link> vehicleLocations) {
        for (RoboTaxi vlp : divertableVehicles) {
            final String key = vlp.getAVVehicle().getId().toString();
            if (!vehicleMap.containsKey(key)) {
                AVVehicle avVehicle = vlp.getAVVehicle();
                VehicleContainer vehicleContainer = new VehicleContainer();
                vehicleContainer.vehicleIndex = db.getVehicleIndex(avVehicle);
                final Link fromLink = vehicleLocations.get(avVehicle);
                GlobalAssert.that(fromLink == vlp.getDivertableLocation());
                vehicleContainer.linkIndex = db.getLinkIndex(vlp.getDivertableLocation());
                if (vlp.isVehicleInStayTask()) {
                    vehicleContainer.avStatus = AVStatus.STAY;
                } else {
                    vehicleContainer.avStatus = AVStatus.DRIVETOCUSTMER;
                    vehicleContainer.destinationLinkIndex = db.getLinkIndex(vlp.getCurrentDriveDestination());
                }
                vehicleMap.put(key, vehicleContainer);
            }
        }
    }

    public SimulationObject compile(Collection<RoboTaxi> divertableVehicles, Map<AVVehicle, Link> vehicleLocations) {
        addDivertableVehicles(divertableVehicles, vehicleLocations);
        simulationObject.vehicles = vehicleMap.values().stream().collect(Collectors.toList());
        return simulationObject;
    }
}
