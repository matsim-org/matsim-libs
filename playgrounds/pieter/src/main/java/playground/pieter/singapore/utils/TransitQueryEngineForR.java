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
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.pseudosimulation.util.CollectionUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by fouriep on 5/7/15.
 */
public class TransitQueryEngineForR {
    Scenario scenario;
    private AtomicInteger numThreads;
    private int threads = 4;
    private ArrayList<StopToStopInfo> outList;
    private double densityDistance;
    private double densityArea;

    public TransitQueryEngineForR(int threads, double densityDistance) {
        this.threads = threads;
        Config config = ConfigUtils.createConfig();
        config.scenario().setUseTransit(true);
        scenario = ScenarioUtils.createScenario(config);
        this.densityDistance = densityDistance;
        densityArea = Math.PI * Math.pow(densityDistance / 1000, 2);
    }

    public TransitQueryEngineForR(double densityDistance) {
        Config config = ConfigUtils.createConfig();
        config.scenario().setUseTransit(true);
        scenario = ScenarioUtils.createScenario(config);
        this.densityDistance = densityDistance;
        densityArea = Math.PI * Math.pow(densityDistance / 1000, 2);
    }

    public static void main(String[] args) {
        TransitQueryEngineForR transitQueryEngineForR = new TransitQueryEngineForR(4, 1000);
        transitQueryEngineForR.loadNetwork(args[0]);
        transitQueryEngineForR.loadTransitSchedule(args[1]);
        System.out.println(String.valueOf(transitQueryEngineForR.getInterStopDistance("46219", "46109", "170_weekday_2-p", "170")));

        //multi-threaded test
        String[] from = new String[]{"46219", "46109", "46069", "46088", "46091", "46059", "46049", "46039", "46029", "45139",
                "45129", "45119", "45109", "45099", "45079", "45069", "45059", "45049", "45039", "45029"};

        String[] to = new String[]{"46109", "46069", "46088", "46091", "46059", "46049", "46039", "46029", "45139", "45129",
                "45119", "45109", "45099", "45079", "45069", "45059", "45049", "45039", "45029", "45019"};

        String[] routes = new String[]{"170_weekday_2-p", "170_weekday_2-p", "170_weekday_2-p", "170_weekday_2-p",
                "170_weekday_2-p", "170_weekday_2-p", "170_weekday_2-p", "170_weekday_2-p", "170_weekday_2-p", "170_weekday_2-p",
                "170_weekday_2-p", "170_weekday_2-p", "170_weekday_2-p", "170_weekday_2-p", "170_weekday_2-p", "170_weekday_2-p",
                "170_weekday_2-p", "170_weekday_2-p", "170_weekday_2-p", "170_weekday_2-p"};

        String[] lines = new String[]{"170", "170", "170", "170", "170", "170", "170", "170", "170",
                "170", "170", "170", "170", "170", "170", "170", "170", "170", "170", "170"};
        double[] interStopDistances = convertDouble(transitQueryEngineForR.getInterStopDistances(from, to, routes, lines));
        double[] interStopDistances2 = convertDouble(transitQueryEngineForR.getInterStopDistancesMultiThreaded(from, to, routes, lines));
        transitQueryEngineForR.calculateStopStopInfo(from, to, routes, lines);

        System.out.println("Done");

    }

    public static double[] convertDouble(Double[] dists) {
        double[] out = new double[dists.length];
        int i = 0;
        for (double d : dists) {
            out[i] = d;
            i++;
        }
        return out;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    public void loadNetwork(String networkFile) {
        new MatsimNetworkReader(scenario).readFile(networkFile);
    }

    public void loadTransitSchedule(String transitScheduleFile) {
        new TransitScheduleReader(scenario).readFile(transitScheduleFile);
    }

    public double getInterStopDistance(String fromStop, String toStop, String route, String line) {
        Id routeId = Id.create(route, TransitRoute.class);
        Id lineId = Id.create(line, TransitLine.class);
        Id fromStopId = Id.create(fromStop, TransitStopFacility.class);
        Id toStopId = Id.create(toStop, TransitStopFacility.class);
        List<TransitRouteStop> stops;
        try {
            stops = scenario.getTransitSchedule().getTransitLines().get(lineId).getRoutes().get(routeId).getStops();
        } catch (NullPointerException ne) {
            System.err.printf("Couldnt find distance for from: %s, to: %s, route: %s, line: %s\n", fromStop, toStop, route, line);
            return Double.POSITIVE_INFINITY;
        }
        Link fromLink = null;
        Link toLink = null;
        for (TransitRouteStop tss : stops) {
            if (tss.getStopFacility().getId().equals(fromStopId))
                fromLink = scenario.getNetwork().getLinks().get(tss.getStopFacility().getLinkId());
            if (tss.getStopFacility().getId().equals(toStopId))
                toLink = scenario.getNetwork().getLinks().get(tss.getStopFacility().getLinkId());
        }

        if (fromLink == null || toLink == null)
            return Double.POSITIVE_INFINITY;
        else {
            NetworkRoute networkRoute = scenario.getTransitSchedule().getTransitLines().get(lineId)
                    .getRoutes().get(routeId).getRoute();
            try {
                NetworkRoute subRoute = networkRoute.getSubRoute(fromLink.getId(), toLink.getId());
                return RouteUtils.calcDistance(subRoute, scenario.getNetwork()) + toLink.getLength();

            } catch (IllegalArgumentException e) {
                //instead of going th elong way round, throw an err
                //                double distance = RouteUtils.calcDistance(
                //                        route.getSubRoute(fromLink.getId(), route.getEndLinkId()), scenario.getNetwork())
                //                        + scenario.getNetwork().getLinks().get(route.getEndLinkId()).getLength();
                //                return distance
                //                        + RouteUtils.calcDistance(route.getSubRoute(route.getStartLinkId(), toLink.getId()),
                //                        scenario.getNetwork()) + toLink.getLength();
                return Double.POSITIVE_INFINITY;
            }

        }

    }

    public Double[] getInterStopDistances(String[] fromStops, String[] toStops, String[] routes, String[] lines) {
        Double[] out = new Double[fromStops.length];
        for (int i = 0; i < fromStops.length; i++) {
            out[i] = getInterStopDistance(fromStops[i], toStops[i], routes[i], lines[i]);
            if (i % 1000 == 0)
                System.out.print(i + "..");
        }
        return out;
    }

    public boolean[] getSuccess() {
        if (outList != null) {
            int i = 0;
            boolean[] out = new boolean[outList.size()];
            for (StopToStopInfo s : outList) {
                out[i] = s.isSuccess();
                i++;
            }

            return out;
        }
        return new boolean[0];
    }

    public double[] getMinCap() {
        if (outList != null) {
            int i = 0;
            double[] out = new double[outList.size()];
            for (StopToStopInfo s : outList) {
                out[i] = s.getMinCap();
                i++;
            }
            return out;
        }
        return new double[0];
    }

    public double[] getDistance() {
        if (outList != null) {
            int i = 0;
            double[] out = new double[outList.size()];
            for (StopToStopInfo s : outList) {
                out[i] = s.getDistance();
                i++;
            }
            return out;
        }
        return new double[0];
    }

    public double[] getNoCarsDistance() {
        if (outList != null) {
            int i = 0;
            double[] out = new double[outList.size()];
            for (StopToStopInfo s : outList) {
                out[i] = s.getNoCarsDistance();
                i++;
            }
            return out;
        }
        return new double[0];
    }

    public double[] getLengthWeightedAverageLaneCount() {
        if (outList != null) {
            int i = 0;
            double[] out = new double[outList.size()];
            for (StopToStopInfo s : outList) {
                out[i] = s.getLengthWeightedAverageLaneCount();
                i++;
            }
            return out;
        }
        return new double[0];
    }

    public double[] getLengthWeightedAverageCapacity() {
        if (outList != null) {
            int i = 0;
            double[] out = new double[outList.size()];
            for (StopToStopInfo s : outList) {
                out[i] = s.getLengthWeightedAverageCapacity();
                i++;
            }
            return out;
        }
        return new double[0];
    }

    public double[] getSqueezeCap() {
        if (outList != null) {
            int i = 0;
            double[] out = new double[outList.size()];
            for (StopToStopInfo s : outList) {
                out[i] = s.getSqueezeCap();
                i++;
            }
            return out;
        }
        return new double[0];
    }

    public double[] getFromX() {
        if (outList != null) {
            int i = 0;
            double[] out = new double[outList.size()];
            for (StopToStopInfo s : outList) {
                try {
                    out[i] = s.getFromCoord().getX();
                } catch (NullPointerException e) {
                    out[i] = Double.POSITIVE_INFINITY;
                }
                i++;
            }
            return out;
        }
        return new double[0];
    }

    public double[] getToX() {
        if (outList != null) {
            int i = 0;
            double[] out = new double[outList.size()];
            for (StopToStopInfo s : outList) {
                try {
                    out[i] = s.getToCoord().getX();
                } catch (NullPointerException e) {
                    out[i] = Double.POSITIVE_INFINITY;
                }
                i++;
            }
            return out;
        }
        return new double[0];
    }

    public double[] getToY() {
        if (outList != null) {
            int i = 0;
            double[] out = new double[outList.size()];
            for (StopToStopInfo s : outList) {
                try {
                    out[i] = s.getToCoord().getY();
                } catch (NullPointerException e) {
                    out[i] = Double.POSITIVE_INFINITY;
                }
                i++;
            }
            return out;
        }
        return new double[0];
    }

    public double[] getFromY() {
        if (outList != null) {
            int i = 0;
            double[] out = new double[outList.size()];
            for (StopToStopInfo s : outList) {
                try {
                    out[i] = s.getFromCoord().getY();
                } catch (NullPointerException e) {
                    out[i] = Double.POSITIVE_INFINITY;
                }
                i++;
            }
            return out;
        }
        return new double[0];
    }

    public double[] getInterSectionDensity() {
        if (outList != null) {
            int i = 0;
            double[] out = new double[outList.size()];
            for (StopToStopInfo s : outList) {
                try {
                    out[i] = s.getInterSectionDensity();
                } catch (NullPointerException e) {
                    out[i] = Double.POSITIVE_INFINITY;
                }
                i++;
            }
            return out;
        }
        return new double[0];
    }

    public double[] getFreeSpeedTravelTime() {
        if (outList != null) {
            int i = 0;
            double[] out = new double[outList.size()];
            for (StopToStopInfo s : outList) {
                out[i] = s.getFreeSpeedTravelTime();
                i++;
            }
            return out;
        }
        return new double[0];
    }

    public int[] getIntersectionCount() {
        if (outList != null) {
            int i = 0;
            int[] out = new int[outList.size()];
            for (StopToStopInfo s : outList) {
                out[i] = s.getIntersectionCount();
                i++;
            }
            return out;
        }
        return new int[0];
    }

    public int[] getNodeCount() {
        if (outList != null) {
            int i = 0;
            int[] out = new int[outList.size()];
            for (StopToStopInfo s : outList) {
                out[i] = s.getNodeCount();
                i++;
            }
            return out;
        }
        return new int[0];
    }

    public Double[] getInterStopDistancesMultiThreaded(String[] fromStops, String[] toStops, String[] routes, String[] lines) {
        List<String[]> fromStopsList = CollectionUtils.split(fromStops, threads);
        List<String[]> toStopsList = CollectionUtils.split(toStops, threads);
        List<String[]> routesList = CollectionUtils.split(routes, threads);
        List<String[]> linesList = CollectionUtils.split(lines, threads);
        numThreads = new AtomicInteger(threads);
        List<ParallelQuery> queries = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            ParallelQuery pq = new ParallelQuery(fromStopsList.get(i), toStopsList.get(i), routesList.get(i), linesList.get(i));
            queries.add(pq);
            Thread slaveThread = new Thread(pq);
            slaveThread.setName("slave_" + i);
            slaveThread.start();
        }
        while (numThreads.get() > 0)
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new RuntimeException();
            }
        //reassemble the output from threads
        List<Double> outList = new ArrayList<>();
        for (ParallelQuery q : queries) {
            outList.addAll(new ArrayList<Double>(Arrays.asList(q.out)));
        }
        return outList.toArray(new Double[outList.size()]);
    }

    public void calculateStopStopInfo(String[] fromStops, String[] toStops, String[] routes, String[] lines) {
        List<String[]> fromStopsList = CollectionUtils.split(fromStops, threads);
        List<String[]> toStopsList = CollectionUtils.split(toStops, threads);
        List<String[]> routesList = CollectionUtils.split(routes, threads);
        List<String[]> linesList = CollectionUtils.split(lines, threads);
        numThreads = new AtomicInteger(threads);
        List<ParallelQueryExtended> queries = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            ParallelQueryExtended pq = new ParallelQueryExtended(fromStopsList.get(i), toStopsList.get(i), routesList.get(i), linesList.get(i));
            queries.add(pq);
            Thread slaveThread = new Thread(pq);
            slaveThread.setName("slave_" + i);
            slaveThread.start();
        }
        while (numThreads.get() > 0)
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new RuntimeException();
            }
        //reassemble the output from threads
        outList = new ArrayList<>();
        for (ParallelQueryExtended q : queries) {
            outList.addAll(new ArrayList<StopToStopInfo>(Arrays.asList(q.out)));
        }
    }

    public double calculateIntersectionDensity(Node toNode) {
        double density = 0;

        NetworkImpl net = (NetworkImpl) scenario.getNetwork();
        double intersectionCount = (net.getNearestNodes(toNode.getCoord(), densityDistance)).size();
        density = intersectionCount / getDensityArea();

        return density;
    }

    private double getDensityArea() {
        return densityArea;
    }

    private class ParallelQuery implements Runnable {
        String[] fromStops;
        String[] toStops;
        String[] routes;
        String[] lines;
        Double[] out;

        public ParallelQuery(String[] fromStops, String[] toStops, String[] routes, String[] lines) {
            this.fromStops = fromStops;
            this.toStops = toStops;
            this.routes = routes;
            this.lines = lines;
        }

        @Override
        public void run() {
            out = getInterStopDistances(fromStops, toStops, routes, lines);
            System.err.println(numThreads.decrementAndGet());
        }
    }

    private class ParallelQueryExtended implements Runnable {
        String[] fromStops;
        String[] toStops;
        String[] routes;
        String[] lines;
        StopToStopInfo[] out;

        public ParallelQueryExtended(String[] fromStops, String[] toStops, String[] routes, String[] lines) {
            this.fromStops = fromStops;
            this.toStops = toStops;
            this.routes = routes;
            this.lines = lines;
            this.out = new StopToStopInfo[fromStops.length];
        }

        @Override
        public void run() {
            for (int i = 0; i < fromStops.length; i++) {
                out[i] = new StopToStopInfo();
                out[i].setAll(fromStops[i], toStops[i], routes[i], lines[i]);
                if(i%1000 == 0)
                    System.out.printf(i+"..");
            }
            System.err.println(numThreads.decrementAndGet());
        }
    }

    class StopToStopInfo {
        boolean success = false;
        private NetworkRoute subRoute;
        private double minCap = Double.POSITIVE_INFINITY;
        private double distance = 0;
        private double noCarsDistance = 0;
        private double lengthWeightedAverageLaneCount = 0; //only calculated for sections where there is car traffic
        private double lengthWeightedAverageCapacity = 0; //only calculated for sections where there is car traffic
        private int intersectionCount = 0; //only calculated for sections where there is car traffic
        private double squeezeCap = 0;//
        private double interSectionDensity = 0;
        private int nodeCount = 0;
        private Coord fromCoord;
        private Coord toCoord;
        private double freeSpeedTravelTime = 0;

        public double getInterSectionDensity() {
            return interSectionDensity;
        }

        public int getNodeCount() {
            return nodeCount;
        }

        public Coord getFromCoord() {
            return fromCoord;
        }

        public Coord getToCoord() {
            return toCoord;
        }

        public double getFreeSpeedTravelTime() {
            return freeSpeedTravelTime;
        }

        public NetworkRoute getSubRoute() {
            return subRoute;
        }

        public boolean isSuccess() {
            return success;
        }

        public double getMinCap() {
            return minCap;
        }

        public double getDistance() {
            return distance;
        }

        public double getNoCarsDistance() {
            return noCarsDistance;
        }

        public double getLengthWeightedAverageLaneCount() {
            return lengthWeightedAverageLaneCount;
        }

        public double getLengthWeightedAverageCapacity() {
            return lengthWeightedAverageCapacity;
        }

        public int getIntersectionCount() {
            return intersectionCount;
        }

        public double getSqueezeCap() {
            return squeezeCap;
        }

        public boolean setAll(String fromStop, String toStop, String route, String line) {
            Id routeId = Id.create(route, TransitRoute.class);
            Id lineId = Id.create(line, TransitLine.class);
            Id fromStopId = Id.create(fromStop, TransitStopFacility.class);
            Id toStopId = Id.create(toStop, TransitStopFacility.class);
            //get all the stops in this route
            List<TransitRouteStop> stops;
            try {
                stops = scenario.getTransitSchedule().getTransitLines().get(lineId).getRoutes().get(routeId).getStops();
            } catch (NullPointerException ne) {
                System.err.printf("Couldnt find distance for from: %s, to: %s, route: %s, line: %s\n", fromStop, toStop, route, line);
                return false;
            }
            if (stops == null)
                return false;
            //find the links for the stops to do calculations
            Link fromLink = null;
            Link toLink = null;
            for (TransitRouteStop tss : stops) {
                if (tss.getStopFacility().getId().equals(fromStopId)) {
                    fromLink = scenario.getNetwork().getLinks().get(tss.getStopFacility().getLinkId());
                    fromCoord = tss.getStopFacility().getCoord();
                }
                if (tss.getStopFacility().getId().equals(toStopId)) {
                    toLink = scenario.getNetwork().getLinks().get(tss.getStopFacility().getLinkId());
                    toCoord = tss.getStopFacility().getCoord();
                }
            }
            if (fromLink == null || toLink == null)
                return false;
            else {
                NetworkRoute networkRoute = scenario.getTransitSchedule().getTransitLines().get(lineId)
                        .getRoutes().get(routeId).getRoute();
                try {
                    subRoute = networkRoute.getSubRoute(fromLink.getId(), toLink.getId());
                    distance = 0;
                    for (Id<Link> id : subRoute.getLinkIds()) {
                        Link link = scenario.getNetwork().getLinks().get(id);
                        if (!link.getAllowedModes().contains(TransportMode.car)) {
                            noCarsDistance += link.getLength();
                        } else {
                            minCap = Math.min(minCap, link.getCapacity());
                            lengthWeightedAverageLaneCount += link.getLength() * link.getNumberOfLanes();
                            lengthWeightedAverageCapacity += link.getLength() * link.getCapacity();
                            Node toNode = link.getToNode();
                            nodeCount++;
                            interSectionDensity += calculateIntersectionDensity(toNode);
                            if (!(
                                    (toNode.getInLinks().size() == 2 && toNode.getOutLinks().size() == 2) ||
                                            (toNode.getInLinks().size() == 1 && toNode.getOutLinks().size() == 1)
                            )) {
                                intersectionCount++;
                                //check intersection for squeeze (more inflow than outflow capacity)
                                double incap = 0;
                                double outcap = 0;
                                for (Link l : toNode.getInLinks().values()) {
                                    incap += l.getCapacity();
                                }
                                for (Link l : toNode.getOutLinks().values()) {
                                    outcap += l.getCapacity();
                                }
                                squeezeCap = Math.max(incap - outcap, squeezeCap);
                            }
                        }
                        distance += link.getLength();
                        freeSpeedTravelTime += link.getLength() > 0 ? link.getLength() / link.getFreespeed() : 0;
                    }
                    //add the last link
                    if (!toLink.getAllowedModes().contains(TransportMode.car)) {
                        noCarsDistance += toLink.getLength();
                    } else {
                        minCap = Math.min(minCap, toLink.getCapacity());
                        lengthWeightedAverageLaneCount += toLink.getLength() * toLink.getNumberOfLanes();
                        lengthWeightedAverageCapacity += toLink.getLength() * toLink.getCapacity();
                        Node toNode = toLink.getToNode();
                        nodeCount++;
                        interSectionDensity += calculateIntersectionDensity(toNode);
                        if (!(
                                (toNode.getInLinks().size() == 2 && toNode.getOutLinks().size() == 2) ||
                                        (toNode.getInLinks().size() == 1 && toNode.getOutLinks().size() == 1)
                        )) {
                            intersectionCount++;
                            //check intersection for squeeze (more inflow than outflow capacity)
                            double incap = 0;
                            double outcap = 0;
                            for (Link l : toNode.getInLinks().values()) {
                                incap += l.getCapacity();
                            }
                            for (Link l : toNode.getOutLinks().values()) {
                                outcap += l.getCapacity();
                            }
                            squeezeCap = Math.max(incap - outcap, 0);
                        }
                    }
                    distance += toLink.getLength();
                    freeSpeedTravelTime += toLink.getLength() > 0 ? toLink.getLength() / toLink.getFreespeed() : 0;
                    lengthWeightedAverageLaneCount /= distance;
                    lengthWeightedAverageCapacity /= distance;
                    interSectionDensity /= nodeCount;
                    success = true;
                    return success;
                } catch (IllegalArgumentException e) {
                    return false;
                }
            }
        }

    }
}
