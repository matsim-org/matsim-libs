/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.michalm.vrp.data;

import java.io.IOException;
import java.util.*;

import org.matsim.api.core.v01.*;

import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.network.VrpGraph;
import playground.michalm.vrp.data.network.*;


public class MatsimVrpDataCreator
{
    public static MatsimVrpData create(Scenario scenario)
        throws IOException
    {
        VrpData vrpData = new VrpData();

        List<Customer> customers = new ArrayList<Customer>();
        List<Request> requests = new ArrayList<Request>();

        vrpData.setCustomers(customers);
        vrpData.setRequests(requests);

        int vertexCount = scenario.getNetwork().getLinks().size();

        MatsimVertexBuilder vertexBuilder = MatsimVertexImpl.createFromLinkIdBuilder(scenario
                .getNetwork());

        VrpGraph graph = new FixedSizeMatsimVrpGraph(vertexCount);
        vrpData.setVrpGraph(graph);

        // build vertices for all links...
        for (Id id : scenario.getNetwork().getLinks().keySet()) {
            graph.addVertex(vertexBuilder.setLinkId(id).build());
        }

        return new MatsimVrpData(vrpData, scenario);
    }
}
