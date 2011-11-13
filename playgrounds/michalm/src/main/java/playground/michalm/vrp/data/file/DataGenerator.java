package playground.michalm.vrp.data.file;

import java.io.*;
import java.util.*;

import org.matsim.api.core.v01.*;

import pl.poznan.put.vrp.dynamic.data.*;
import pl.poznan.put.vrp.dynamic.data.VRPData.VRPType;
import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.network.*;
import playground.michalm.vrp.data.network.*;


public class DataGenerator
{
    public static VRPData generate(Scenario scenario)
        throws IOException
    {
        VRPData data = new VRPData();
        data.setType(VRPType.TAXI);

        List<Depot> depots = new ArrayList<Depot>(1);
        List<Customer> customers = new ArrayList<Customer>(0);
        List<Vehicle> vehicles = new ArrayList<Vehicle>(1);

        List<Request> requests = new ArrayList<Request>();

        data.setDepots(depots);
        data.setCustomers(customers);
        data.setVehicles(vehicles);

        data.setRequests(requests);

        int vertexCount = scenario.getNetwork().getLinks().size();

        MATSimVertexBuilder vertexBuilder = MATSimVertexImpl.createFromLinkIdBuilder(scenario
                .getNetwork());

        MATSimVRPGraph graph = new MATSimVRPGraph(vertexCount);
        data.setVrpGraph(graph);

        // build vertices for all links...
        for (Id id : scenario.getNetwork().getLinks().keySet()) {
            graph.addVertex(vertexBuilder.setLinkId(id).build());
        }

        Vertex vertex = graph.getVertex(scenario.createId("227"));

        Depot depot = new DepotImpl(0, "D_0", vertex);
        depots.add(depot);

        vehicles.add(new VehicleImpl(0, "V_0", depot, 1, 0, 0, 24 * 3600, 24 * 3600));
        vehicles.add(new VehicleImpl(1, "V_1", depot, 1, 0, 0, 24 * 3600, 24 * 3600));
        vehicles.add(new VehicleImpl(2, "V_2", depot, 1, 0, 0, 24 * 3600, 24 * 3600));
        vehicles.add(new VehicleImpl(3, "V_3", depot, 1, 0, 0, 24 * 3600, 24 * 3600));
        vehicles.add(new VehicleImpl(4, "V_4", depot, 1, 0, 0, 24 * 3600, 24 * 3600));

        return data;
    }
}
