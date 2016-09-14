/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.path;

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.*;
import org.matsim.core.router.*;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

import com.google.common.collect.Maps;


public abstract class AbstractOneToManyPathSearch
{
    public static class PathData
    {
        private final Node node;
        public final double delay;//at both the first and last links
        private Path path;//shortest path


        public PathData(Node node, double delay)
        {
            this.node = node;
            this.delay = delay;
        }


        public Path getPath()
        {
            return path;
        }
    }


    private static class ToNode
        extends InitialNode
    {
        private Path path;


        private ToNode(Node node, double initialCost, double initialTime)
        {
            super(node, initialCost, initialTime);
        }
    }


    private final FastMultiNodeDijkstra multiNodeDijkstra;//forward or backward


    public AbstractOneToManyPathSearch(FastMultiNodeDijkstra multiNodeDijkstra)
    {
        this.multiNodeDijkstra = multiNodeDijkstra;
    }


    public PathData[] calcPaths(Link fromLink, List<Link> toLinks, double startTime)
    {
        int size = toLinks.size();
        PathData[] pathDataArray = new PathData[size];
        Map<Id<Node>, ToNode> toNodes = Maps.newHashMapWithExpectedSize(size);

        for (int i = 0; i < size; i++) {
            PathData pathData = pathDataArray[i] = //
                    createPathData(fromLink, toLinks.get(i), startTime);
            if (!toNodes.containsKey(pathData.node.getId())) {
                toNodes.put(pathData.node.getId(), new ToNode(pathData.node, 0, 0));
            }
        }

        calculatePaths(getFromNode(fromLink), toNodes.values(), startTime);

        for (int i = 0; i < size; i++) {
            PathData pathData = pathDataArray[i];
            pathData.path = toNodes.get(pathData.node.getId()).path;
        }

        return pathDataArray;
    }


    protected void calculatePaths(Node fromNode, Collection<ToNode> toNodes, double time)
    {
        ImaginaryNode imaginaryNode = multiNodeDijkstra.createImaginaryNode(toNodes);
        multiNodeDijkstra.calcLeastCostPath(fromNode, imaginaryNode, time, null, null);

        //get path for each taxiNode 
        for (ToNode toNode : toNodes) {
            toNode.path = multiNodeDijkstra.constructPath(fromNode, toNode.node, time);
        }
    }


    protected abstract PathData createPathData(Link fromLink, Link toLink, double startTime);


    protected abstract Node getFromNode(Link fromLink);
}
