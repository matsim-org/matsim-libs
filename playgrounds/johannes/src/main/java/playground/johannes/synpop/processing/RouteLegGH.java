/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.johannes.synpop.processing;

import com.graphhopper.GraphHopper;
import com.graphhopper.routing.AlgorithmOptions;
import com.graphhopper.routing.Path;
import com.graphhopper.routing.QueryGraph;
import com.graphhopper.routing.RoutingAlgorithm;
import com.graphhopper.routing.ch.PrepareContractionHierarchies;
import com.graphhopper.routing.util.DefaultEdgeFilter;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.TraversalMode;
import com.graphhopper.routing.weighting.FastestWeighting;
import com.graphhopper.storage.CHGraphImpl;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.storage.index.LocationIndex;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.Parameters;
import gnu.trove.list.TIntList;
import org.matsim.api.core.v01.Id;
import org.matsim.facilities.ActivityFacility;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Segment;
import playground.johannes.synpop.gis.FacilityData;

/**
 * @author johannes
 */
public class RouteLegGH implements SegmentTask {

    private static final String SEPARATOR = " ";

//    private RoutingAlgorithm router;

    private final FacilityData facilityData;

    private final LocationIndex index;

    private final MathTransform transform;

    private final PrepareContractionHierarchies pch;

    private final AlgorithmOptions algoOpts;

    private final GraphHopperStorage graph;

    private final EdgeFilter edgeFilter;

    public RouteLegGH(GraphHopper graphHopper, FlagEncoder encoder, FacilityData facilityData, MathTransform transform) {

        this.facilityData = facilityData;
        this.index = graphHopper.getLocationIndex();
        this.transform = transform;

        graph = graphHopper.getGraphHopperStorage();
        pch = new PrepareContractionHierarchies(graph.getDirectory(),
                graph,
                graph.getGraph(CHGraphImpl.class),
                new FastestWeighting(encoder),
                TraversalMode.NODE_BASED);

        algoOpts = AlgorithmOptions.start().algorithm(Parameters.Algorithms.ASTAR_BI).
//        algoOpts = AlgorithmOptions.start().algorithm(Parameters.Algorithms.DIJKSTRA_BI).
                traversalMode(TraversalMode.NODE_BASED).
                weighting(new FastestWeighting(encoder)).
                build();

        edgeFilter = new DefaultEdgeFilter(encoder);
    }

    @Override
    public void apply(Segment segment) {
        Segment prev = segment.previous();
        Segment next = segment.next();

        if(prev != null && next != null) {
            String prevId = prev.getAttribute(CommonKeys.ACTIVITY_FACILITY);
            String nextId = next.getAttribute(CommonKeys.ACTIVITY_FACILITY);

            if(prevId != null && nextId != null) {
                ActivityFacility startFac = facilityData.getAll().getFacilities().get(Id.create(prevId, ActivityFacility.class));
                ActivityFacility endFac = facilityData.getAll().getFacilities().get(Id.create(nextId, ActivityFacility.class));

                double[] fromCoord = calcWGS84Coords(startFac);
                double[] toCoord = calcWGS84Coords(endFac);

                QueryResult fromQuery = index.findClosest(fromCoord[1], fromCoord[0], edgeFilter);
                QueryResult toQuery = index.findClosest(toCoord[1], toCoord[0], edgeFilter);

                if(!fromQuery.isValid() || !toQuery.isValid()) {
                    return;
                }

                QueryGraph queryGraph = new QueryGraph(graph.getGraph(CHGraphImpl.class));
                queryGraph.lookup(fromQuery, toQuery);

                RoutingAlgorithm algorithm = pch.createAlgo(queryGraph, algoOpts);
                Path path = algorithm.calcPath(fromQuery.getClosestNode(), toQuery.getClosestNode());
                TIntList nodes = path.calcNodes();

                if(nodes.size() > 0) {
                    StringBuilder builder = new StringBuilder();
                    builder.append(String.valueOf(nodes.get(0)));
                    for (int i = 1; i < nodes.size(); i++) {
                        builder.append(SEPARATOR);
                        builder.append(String.valueOf(nodes.get(i)));
                    }
                    segment.setAttribute(CommonKeys.LEG_ROUTE, builder.toString());
                } else {
                    System.err.println("Ooops");
                }
            }
        }
    }

    private double[] calcWGS84Coords(ActivityFacility facility) {
        double[] points = new double[] { facility.getCoord().getX(), facility.getCoord().getY() };
        try {
            transform.transform(points, 0, points, 0, 1);
        } catch (TransformException e) {
            e.printStackTrace();
        }
        return points;
    }
}
