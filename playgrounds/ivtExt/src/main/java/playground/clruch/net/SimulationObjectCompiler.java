// code by jph
package playground.clruch.net;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.network.Link;

import playground.clruch.dispatcher.core.RoboTaxi;
import playground.clruch.utils.GlobalAssert;
import playground.sebhoerl.avtaxi.passenger.AVRequest;

// TODO general cleanup, can be made shorter! 

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

    public void addVehicles(List<RoboTaxi> robotaxis) {
        for (RoboTaxi robotaxi : robotaxis) {
            VehicleContainer vehicleContainer = new VehicleContainer();
            final String key = robotaxi.getId().toString();
            vehicleContainer.vehicleIndex = db.getVehicleIndex(robotaxi);
            final Link fromLink = robotaxi.getCurrentLocation();
            GlobalAssert.that(fromLink != null);
            vehicleContainer.linkIndex = db.getLinkIndex(fromLink);
            vehicleContainer.avStatus = robotaxi.getAVStatus();
            GlobalAssert.that(robotaxi.getCurrentDriveDestination() != null);
            vehicleContainer.destinationLinkIndex = db.getLinkIndex(robotaxi.getCurrentDriveDestination());
            vehicleMap.put(key, vehicleContainer);
        }
    }

    public SimulationObject compile() {
        simulationObject.vehicles = vehicleMap.values().stream().collect(Collectors.toList());
        return simulationObject;
    }
}

// @Deprecated
// public void addRebalancingVehiclesOld(Map<AVVehicle, Link> rebalancingVehicles, Map<AVVehicle,
// Link> vehicleLocations) {
// for (Entry<AVVehicle, Link> entry : rebalancingVehicles.entrySet()) {
// VehicleContainer vehicleContainer = new VehicleContainer();
// AVVehicle avVehicle = entry.getKey();
// final String key = avVehicle.getId().toString();
// final Link fromLink = vehicleLocations.get(avVehicle);
// vehicleContainer.vehicleIndex = db.getVehicleIndex(avVehicle);
// vehicleContainer.linkIndex = db.getLinkIndex(fromLink);
// vehicleContainer.avStatus = AVStatus.REBALANCEDRIVE;
// vehicleContainer.destinationLinkIndex = db.getLinkIndex(entry.getValue());
// vehicleMap.put(key, vehicleContainer);
// }
// }
//

// @Deprecated
// public void addRebalancingVehicles(Map<RoboTaxi, Link> rebalancingVehicles, Map<AVVehicle, Link>
// vehicleLocations) {
// for (Entry<RoboTaxi, Link> entry : rebalancingVehicles.entrySet()) {
// VehicleContainer vehicleContainer = new VehicleContainer();
// AVVehicle avVehicle = entry.getKey().getAVVehicle();
// final String key = avVehicle.getId().toString();
// final Link fromLink = vehicleLocations.get(avVehicle);
// vehicleContainer.vehicleIndex = db.getVehicleIndex(avVehicle);
// vehicleContainer.linkIndex = db.getLinkIndex(fromLink);
// vehicleContainer.avStatus = AVStatus.REBALANCEDRIVE;
// vehicleContainer.destinationLinkIndex = db.getLinkIndex(entry.getValue());
// vehicleMap.put(key, vehicleContainer);
// }
// }

// @Deprecated
// public void addVehiclesWithCustomer(Map<AVVehicle, Link> map, Map<AVVehicle, Link>
// vehicleLocations) {
// for (Entry<AVVehicle, Link> entry : map.entrySet()) {
// VehicleContainer vehicleContainer = new VehicleContainer();
// AVVehicle avVehicle = entry.getKey();
// final String key = avVehicle.getId().toString();
// final Link fromLink = vehicleLocations.get(avVehicle);
// GlobalAssert.that(fromLink != null);
// vehicleContainer.vehicleIndex = db.getVehicleIndex(avVehicle);
// vehicleContainer.linkIndex = db.getLinkIndex(fromLink);
// vehicleContainer.avStatus = AVStatus.DRIVEWITHCUSTOMER;
// vehicleContainer.destinationLinkIndex = db.getLinkIndex(entry.getValue());
// vehicleMap.put(key, vehicleContainer);
// }
// }

// public void addVehiclesWithCustomerNew(List<RoboTaxi> robotaxisWithCustomer) {
// for (RoboTaxi robotaxi : robotaxisWithCustomer) {
// VehicleContainer vehicleContainer = new VehicleContainer();
// AVVehicle avVehicle = robotaxi.getAVVehicle();
// final String key = avVehicle.getId().toString();
// final Link fromLink = robotaxi.getCurrentLocation();
// GlobalAssert.that(fromLink != null);
// vehicleContainer.vehicleIndex = db.getVehicleIndex(avVehicle);
// vehicleContainer.linkIndex = db.getLinkIndex(fromLink);
// vehicleContainer.avStatus = AVStatus.DRIVEWITHCUSTOMER;
// vehicleContainer.destinationLinkIndex = db.getLinkIndex(robotaxi.getCurrentDriveDestination());
// vehicleMap.put(key, vehicleContainer);
// }
// }
//
// public void addRebalancingVehiclesNew(List<RoboTaxi> rebalancingVehicles) {
// for (RoboTaxi robotaxi : rebalancingVehicles) {
// VehicleContainer vehicleContainer = new VehicleContainer();
// AVVehicle avVehicle = robotaxi.getAVVehicle();
// final String key = avVehicle.getId().toString();
// final Link fromLink = robotaxi.getCurrentLocation();
// vehicleContainer.vehicleIndex = db.getVehicleIndex(avVehicle);
// vehicleContainer.linkIndex = db.getLinkIndex(fromLink);
// vehicleContainer.avStatus = AVStatus.REBALANCEDRIVE;
// vehicleContainer.destinationLinkIndex = db.getLinkIndex(robotaxi.getCurrentDriveDestination());
// vehicleMap.put(key, vehicleContainer);
// }
// }
//
// private void addDivertableVehicles(Collection<RoboTaxi> divertableVehicles) {
// for (RoboTaxi robotaxi: divertableVehicles) {
// final String key = robotaxi.getAVVehicle().getId().toString();
// if (!vehicleMap.containsKey(key)) {
// AVVehicle avVehicle = robotaxi.getAVVehicle();
// VehicleContainer vehicleContainer = new VehicleContainer();
// vehicleContainer.vehicleIndex = db.getVehicleIndex(avVehicle);
// final Link fromLink = robotaxi.getDivertableLocation();
// GlobalAssert.that(fromLink == robotaxi.getDivertableLocation());
// vehicleContainer.linkIndex = db.getLinkIndex(robotaxi.getDivertableLocation());
// if (robotaxi.isVehicleInStayTask()) {
// vehicleContainer.avStatus = AVStatus.STAY;
// } else {
// vehicleContainer.avStatus = AVStatus.DRIVETOCUSTMER;
// vehicleContainer.destinationLinkIndex = db.getLinkIndex(robotaxi.getCurrentDriveDestination());
// }
// vehicleMap.put(key, vehicleContainer);
// }
// }
// }

// public SimulationObject compile(Collection<RoboTaxi> divertableVehicles) {
// addDivertableVehicles(divertableVehicles);
