package playground.michalm.vrp.data;

import java.io.IOException;
import java.util.*;

import org.matsim.api.core.v01.*;

import pl.poznan.put.vrp.dynamic.data.*;
import pl.poznan.put.vrp.dynamic.data.VRPData.VRPType;
import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.network.*;
import playground.michalm.vrp.data.network.*;


public class MATSimVRPDataCreator
{
    public static MATSimVRPData create(Scenario scenario)
        throws IOException
    {
        VRPData vrpData = new VRPData();
        vrpData.setType(VRPType.TAXI);

        List<Customer> customers = new ArrayList<Customer>();
        List<Request> requests = new ArrayList<Request>();

        vrpData.setCustomers(customers);
        vrpData.setRequests(requests);

        int vertexCount = scenario.getNetwork().getLinks().size();

        MATSimVertexBuilder vertexBuilder = MATSimVertexImpl.createFromLinkIdBuilder(scenario
                .getNetwork());

        VRPGraph graph = new FixedSizeMatsimVrpGraph(vertexCount);
        vrpData.setVrpGraph(graph);

        // build vertices for all links...
        for (Id id : scenario.getNetwork().getLinks().keySet()) {
            graph.addVertex(vertexBuilder.setLinkId(id).build());
        }

        return new MATSimVRPData(vrpData, scenario);
    }
}
