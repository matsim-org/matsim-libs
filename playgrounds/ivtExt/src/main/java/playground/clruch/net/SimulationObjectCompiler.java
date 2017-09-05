// code by jph
// modified, revised by clruch
package playground.clruch.net;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.network.Link;

import ch.ethz.idsc.queuey.util.GlobalAssert;
import playground.clruch.dispatcher.core.RoboTaxi;
import playground.sebhoerl.avtaxi.passenger.AVRequest;

public class SimulationObjectCompiler {
    
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
    

    private final SimulationObject simulationObject;
    private final Map<String, VehicleContainer> vehicleMap = new HashMap<>();
    private final MatsimStaticDatabase db = MatsimStaticDatabase.INSTANCE;



    private SimulationObjectCompiler(SimulationObject simulationObject) {
        this.simulationObject = simulationObject;
    }

    public void insertRequests(Collection<AVRequest> avRequests) {
        avRequests.forEach(this::insertRequest);
    }

    public void insertVehicles(List<RoboTaxi> robotaxis) {
        robotaxis.forEach(this::insertVehicle);
    }

    public SimulationObject compile() {
        simulationObject.vehicles = vehicleMap.values().stream().collect(Collectors.toList());
        return simulationObject;
    }

    private void insertRequest(AVRequest avRequest) {
        RequestContainer requestContainer = new RequestContainer();
        requestContainer.requestIndex = db.getRequestIndex(avRequest);
        requestContainer.fromLinkIndex = db.getLinkIndex(avRequest.getFromLink());
        requestContainer.submissionTime = avRequest.getSubmissionTime();
        requestContainer.toLinkIndex = db.getLinkIndex(avRequest.getToLink());
        simulationObject.requests.add(requestContainer);
    }

    private void insertVehicle(RoboTaxi robotaxi) {
        VehicleContainer vehicleContainer = new VehicleContainer();
        final String key = robotaxi.getId().toString();
        vehicleContainer.vehicleIndex = db.getVehicleIndex(robotaxi);
        final Link fromLink = robotaxi.getLastKnownLocation();
        GlobalAssert.that(fromLink != null);
        vehicleContainer.linkIndex = db.getLinkIndex(fromLink);
        vehicleContainer.avStatus = robotaxi.getAVStatus();
        GlobalAssert.that(robotaxi.getCurrentDriveDestination() != null);
        vehicleContainer.destinationLinkIndex = db.getLinkIndex(robotaxi.getCurrentDriveDestination());
        vehicleMap.put(key, vehicleContainer);
    }
}
