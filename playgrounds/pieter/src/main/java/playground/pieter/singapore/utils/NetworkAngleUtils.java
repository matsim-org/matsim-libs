/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *  * ***********************************************************************
 */

package playground.pieter.singapore.utils;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.geometry.CoordImpl;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

import static java.lang.Math.*;

/**
 * Created by fouriep on 5/14/15.
 */
public class NetworkAngleUtils {

    private static final double PARALLEL_EPSILON = toRadians(0.5);

    private static Coord getVector(Link link) {
        double x = link.getToNode().getCoord().getX() - link.getFromNode().getCoord().getX();
        double y = link.getToNode().getCoord().getY() - link.getFromNode().getCoord().getY();
        return new CoordImpl(x, y);
    }

    private static Coord getUnityVector(Link link) {
        Coord vector = getVector(link);
        double x = vector.getX();
        double y = vector.getY();
        double m = sqrt(x * x + y * y);
        return new CoordImpl(x / m, y / m);
    }

    /**
     * @param fromLink
     * @param toLink
     * @return the angle in degrees between two links, using the dot-product of their unity vectors.
     * Positive is to the left, negative to the right.
     * @author pieterfourie
     */
    public static double getAngleBetweenLinks(Link fromLink, Link toLink) {
        Coord fromVector = getUnityVector(fromLink);
        double refAngle = atan2(fromVector.getY(), fromVector.getX());
        Coord toVector = getUnityVector(toLink);
        double angle2 = atan2(toVector.getY(), toVector.getX());
//        double dotprod = fromVector.getX() * toVector.getX() + fromVector.getY() * toVector.getY();
//        double angle = acos(dotprod);
        double angle = angle2 - refAngle;
        //positive to the left, negative to the right
//        return toVector.getX() * cos(refAngle) - toVector.getY() * sin(refAngle) < 0 ? -angle : angle;
        if (abs(angle) > PI)
            if (angle > 0)
                return angle - 2 * PI;
            else
                return angle + 2 * PI;
        return angle;
    }

    /**
     * Like org.matsim.lanes.data.CalculateAngle, but works relative to the inlink, not just north/south.
     * The last link is the one with the sharpest angle to the right (ie.most negative according to the euclidean convention).
     * Backlink (toNode == inLink.fromNode)  is ignored.
     *
     * @param inLink
     * @return A map of links sorted from leftmost to righmost angle, excluding backlink.
     */
    public static TreeMap<Double, Link> getOutLinksSortedByAngle(Link inLink) {

        TreeMap<Double, Link> sortedLinks = new TreeMap<Double, Link>();

        for (Link outLink : inLink.getToNode().getOutLinks().values()) {
            if (outLink.getToNode().equals(inLink.getFromNode()))
                continue;
            else {
                double angleBetweenLinks = getAngleBetweenLinks(inLink, outLink);
                if (abs(abs(angleBetweenLinks) - PI) > PARALLEL_EPSILON)
                    sortedLinks.put(angleBetweenLinks, outLink);
            }
        }
        return sortedLinks;
    }


    public static boolean isIntersection(Node node) {

        if (node.getInLinks().size() == 1 && node.getOutLinks().size() >= 1)
            return false;

        if (node.getInLinks().size() == 2 && node.getOutLinks().size() == 2) {
            int parallelCount = 0;
            for (Link inLink : node.getInLinks().values()) {
                for (Link outLink : node.getOutLinks().values()) {
                    parallelCount += outLink.getToNode().equals(inLink.getFromNode()) ? 1 : 0;
                }
            }
            return parallelCount < 2;
        }
        return true;
    }

    public static Set<Id<Node>> getInterSections(Network network) {
        Set<Id<Node>> output = new HashSet<>();
        for (Node n : network.getNodes().values()) {
            if (isIntersection(n))
                output.add(n.getId());
        }
        return output;
    }

    public static Set<Link> getNearlyParallelLinks(Link link, double tolerance) {
        if (tolerance < 0)
            throw new RuntimeException("Tolerance less than zero. Fix it.");

        Set<Link> output = new HashSet<>();


        for (Link l : link.getFromNode().getInLinks().values()) {
            if (abs(getAngleBetweenLinks(link, l)) < tolerance) {
                output.add(l);
            }
        }
        for (Link l : link.getFromNode().getOutLinks().values()) {
            if (link.equals(l))
                continue;
            if (abs(getAngleBetweenLinks(link, l)) < tolerance) {
                output.add(l);
            }
        }

        for (Link l : link.getToNode().getOutLinks().values()) {
            if (abs(getAngleBetweenLinks(link, l)) < tolerance) {
                output.add(l);
            }
        }
        for (Link l : link.getToNode().getInLinks().values()) {
            if (link.equals(l))
                continue;
            if (abs(getAngleBetweenLinks(link, l)) < tolerance) {
                output.add(l);
            }
        }

        return output;

    }

}
