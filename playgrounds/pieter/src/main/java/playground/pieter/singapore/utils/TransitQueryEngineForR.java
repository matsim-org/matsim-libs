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
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.pseudosimulation.util.CollectionUtils;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.pt.PtConstants;
import org.matsim.pt.transitSchedule.api.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Math.*;

/**
 * Created by fouriep on 5/7/15.
 */
public class TransitQueryEngineForR {
    static double minimumAngleDefiningIntersection = toRadians(1);
    Scenario scenario;
    private AtomicInteger numThreads;
    private int threads = 4;
    private ArrayList<StopToStopInfo> outList;
    private double densityDistance;
    private double densityArea;
    private HashSet<Id<Node>> trafficControlNodes;
    private Set<Id<Node>> interSections;
    private Map<Id<ActivityFacility>, FacilityActivityCounter> facilityToActivityLevelAtTimeMap;
    private Map<Node, Double> intersectionDensities = new HashMap<>();
    private Map<Node, Set<FacilityActivityCounter>> nodes2ActivityLevels = new HashMap<>();
    private Map<Link, Set<ActivityFacility>> links2Facilities = new HashMap<>();
    private int rateCalculationWindowSize;
    private boolean isWeightedAverageValues;

    public TransitQueryEngineForR(int threads, double densityDistance) {
        this(densityDistance);
        this.threads = threads;
    }

    public TransitQueryEngineForR(double densityDistance) {
        Config config = ConfigUtils.createConfig();
        config.scenario().setUseTransit(true);
        scenario = ScenarioUtils.createScenario(config);
        this.densityDistance = densityDistance;
        densityArea = PI * pow(densityDistance / 1000, 2);
    }

    public static void main(String[] args) {
        TransitQueryEngineForR transitQueryEngineForR = new TransitQueryEngineForR(4, 564);
        transitQueryEngineForR.loadNetwork(args[0]);
        transitQueryEngineForR.loadTransitSchedule(args[1]);
        transitQueryEngineForR.loadNodeAttrs(args[2]);
        transitQueryEngineForR.loadFacilities(args[3]);
        transitQueryEngineForR.loadEvents(args[4]);
        transitQueryEngineForR.setRateCalculationWindowSize(900);
        transitQueryEngineForR.setIsWeightedAverageValues(false);
        System.out.println(String.valueOf(transitQueryEngineForR.getInterStopDistance("46219", "46109", "170_weekday_2-p", "170")));

        //multi-threaded test
        String[] from = new String[]{"80039", "60161", "31191", "65231", "17389", "58021", "56091", "17181", "62121",
                "62011", "46779", "42071", "84629", "46391", "84231", "25359", "21211", "63369", "93199", "84041"};

        String[] to = new String[]{"80029", "60201", "30011", "65269", "17029", "58339", "57011", "20101", "66381",
                "62031", "46769", "42151", "84479", "46761", "84241", "25349", "21681", "66379", "93201", "84051"};

        String[] routes = new String[]{"13_weekday_2", "107_weekday_2", "172_weekday_2", "83_weekday_1", "51_weekday_2",
                "167_weekday_2", "138_weekday_1", "106_weekday_2", "133_weekday_2", "76_weekday_1", "965_weekday_1",
                "157_weekday_1", "65_weekday_1", "913_weekday_1", "222_weekday_1", "193_weekday_2", "30_weekday_1",
                "43_weekday_1", "135_weekday_1", "31_weekday_2"};

        String[] lines = new String[]{"13", "107", "172", "83", "51", "167", "138", "106", "133", "76", "965",
                "157", "65", "913", "222", "193", "30", "43", "135", "31"};

        //make some random departure times
        String[] times = new String[20];
        for (int i = 0; i < 20; i++) {
            times[i] = "" + (21600 + (int) (random() * 46800));
        }

//        double[] interStopDistances = convertDouble(transitQueryEngineForR.getInterStopDistances(from, to, routes, lines));
//        double[] interStopDistances2 = convertDouble(transitQueryEngineForR.getInterStopDistancesMultiThreaded(from, to, routes, lines));
        for (int i = 0; i < 100; i++) {
            transitQueryEngineForR.setIsWeightedAverageValues(!transitQueryEngineForR.isWeightedAverageValues());
            transitQueryEngineForR.calculateStopStopInfo(from, to, routes, lines, times);
            System.out.printf("%d..", i);
        }


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

    public void loadEvents(String arg) {
        facilityToActivityLevelAtTimeMap = new ConcurrentHashMap<>();
        EventsManager eventsManager = EventsUtils.createEventsManager();
        eventsManager.addHandler(new ActivityEndEventHandler() {
            @Override
            public void handleEvent(ActivityEndEvent event) {
                if (event.getActType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE))
                    return;

                FacilityActivityCounter facilityActivityCounter = facilityToActivityLevelAtTimeMap.get(event.getFacilityId());
                if (facilityActivityCounter == null) {
                    facilityActivityCounter = new FacilityActivityCounter();
                    facilityToActivityLevelAtTimeMap.put(event.getFacilityId(), facilityActivityCounter);
                }
                facilityActivityCounter.registerDeparture((int) event.getTime());
            }

            @Override
            public void reset(int iteration) {

            }
        });
        eventsManager.addHandler(new ActivityStartEventHandler() {
            @Override
            public void handleEvent(ActivityStartEvent event) {
                if (event.getActType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE))
                    return;

                FacilityActivityCounter facilityActivityCounter = facilityToActivityLevelAtTimeMap.get(event.getFacilityId());
                if (facilityActivityCounter == null) {
                    facilityActivityCounter = new FacilityActivityCounter();
                    facilityToActivityLevelAtTimeMap.put(event.getFacilityId(), facilityActivityCounter);
                }
                facilityActivityCounter.registerArrival((int) event.getTime());
            }

            @Override
            public void reset(int iteration) {

            }
        });

        try {
            new MatsimEventsReader(eventsManager).readFile("/home/fouriep/actOnlyEvents.xml");
            System.out.println("...Reading from pre-processed events...");

        } catch (Exception e) {

            EventWriterXMLFiltered eventWriterXML = new EventWriterXMLFiltered("/home/fouriep/actOnlyEvents.xml");
            eventsManager.addHandler(eventWriterXML);
            new MatsimEventsReader(eventsManager).readFile(arg);
            eventWriterXML.closeFile();
        }

        final int total = facilityToActivityLevelAtTimeMap.size();
        int i = 0;
        for (FacilityActivityCounter counter : facilityToActivityLevelAtTimeMap.values()) {
            counter.finalizeCounters();
            i++;
            if (i % (total / 10) == 0)
                System.out.printf("%d/%d..", i, total);
        }

        //the node calculations take long, so made the maps they write to thread-safe:
        final List<? extends Node>[] split = CollectionUtils.split(scenario.getNetwork().getNodes().values(), threads);
        numThreads = new AtomicInteger(threads);
        DensityCalculator[] calculators = new DensityCalculator[threads];
        for (int j = 0; j < threads; j++) {
            calculators[j] = new DensityCalculator(split[j]);
            new Thread(calculators[j]).start();
        }

        while (numThreads.get() > 0)
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        for (DensityCalculator d : calculators) {
            nodes2ActivityLevels.putAll(d.getNodes2ActLevels());
            intersectionDensities.putAll(d.getIntDensities());
        }
    }

    public void loadFacilities(String fileName) {
        new MatsimFacilitiesReader(scenario).readFile(fileName);
        NetworkImpl network = (NetworkImpl) scenario.getNetwork();
        for (ActivityFacility facility : scenario.getActivityFacilities().getFacilities().values())
            ((ActivityFacilityImpl) facility).setLinkId(network.getNearestLinkExactly(facility.getCoord()).getId());
        //create a lookup map that relates all facilities to links that provide access
        links2Facilities = new HashMap<>();
        int total = scenario.getActivityFacilities().getFacilities().size();
        int i = 0;
        for (ActivityFacility facility : scenario.getActivityFacilities().getFacilities().values()) {
            Link link = network.getLinks().get(facility.getLinkId());
            Set<ActivityFacility> activityFacilities = links2Facilities.get(link);
            if (activityFacilities == null) {
                activityFacilities = new HashSet<>();
                links2Facilities.put(link, activityFacilities);
            }
            activityFacilities.add(facility);
            i++;
            if (i % (total / 10) == 0)
                System.out.printf("%d/%d..", i, total);
        }

    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    public void loadNetwork(String networkFile) {
        new MatsimNetworkReader(scenario).readFile(networkFile);
        this.interSections = NetworkAngleUtils.getInterSections(scenario.getNetwork());
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

    public double[] getEuclideanDistance() {
        if (outList != null) {
            int i = 0;
            double[] out = new double[outList.size()];
            for (StopToStopInfo s : outList) {
                out[i] = s.getEuclideanDistance();
                i++;
            }
            return out;
        }
        return new double[0];
    }

    public double[] getWeightedAvgIntersectionComplexity() {
        if (outList != null) {
            int i = 0;
            double[] out = new double[outList.size()];
            for (StopToStopInfo s : outList) {
                out[i] = s.getWeightedAvgIntersectionComplexity();
                i++;
            }
            return out;
        }
        return new double[0];
    }

    public double[] getTotalIntersectionComplexity() {
        if (outList != null) {
            int i = 0;
            double[] out = new double[outList.size()];
            for (StopToStopInfo s : outList) {
                out[i] = s.getTotalIntersectionComplexity();
                i++;
            }
            return out;
        }
        return new double[0];
    }

    public double[] getTotalWeightedDegreesTurned() {
        if (outList != null) {
            int i = 0;
            double[] out = new double[outList.size()];
            for (StopToStopInfo s : outList) {
                out[i] = s.getTotalWeightedDegreesTurned();
                i++;
            }
            return out;
        }
        return new double[0];
    }

    public double[] getTrafficControlCount() {
        if (outList != null) {
            int i = 0;
            double[] out = new double[outList.size()];
            for (StopToStopInfo s : outList) {
                out[i] = s.getTrafficControlCount();
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

    public int[] getLeftTurnsMadeAtIntersections() {
        if (outList != null) {
            int i = 0;
            int[] out = new int[outList.size()];
            for (StopToStopInfo s : outList) {
                out[i] = s.getLeftTurnsMadeAtIntersections();
                i++;
            }
            return out;
        }
        return new int[0];
    }

    public int[] getRightTurnsMadeAtIntersections() {
        if (outList != null) {
            int i = 0;
            int[] out = new int[outList.size()];
            for (StopToStopInfo s : outList) {
                out[i] = s.getRightTurnsMadeAtIntersections();
                i++;
            }
            return out;
        }
        return new int[0];
    }

    public int[] getRightTurnsPassedAtIntersection() {
        if (outList != null) {
            int i = 0;
            int[] out = new int[outList.size()];
            for (StopToStopInfo s : outList) {
                out[i] = s.getRightTurnsPassedAtIntersection();
                i++;
            }
            return out;
        }
        return new int[0];
    }

    public int[] getLeftTurnsPassedAtIntersection() {
        if (outList != null) {
            int i = 0;
            int[] out = new int[outList.size()];
            for (StopToStopInfo s : outList) {
                out[i] = s.getLeftTurnsPassedAtIntersection();
                i++;
            }
            return out;
        }
        return new int[0];
    }

    public int[] getIntersectionsWithOneOrMoreLEFTTurnsPassed() {
        if (outList != null) {
            int i = 0;
            int[] out = new int[outList.size()];
            for (StopToStopInfo s : outList) {
                out[i] = s.getIntersectionsWithOneOrMoreLEFTTurnsPassed();
                i++;
            }
            return out;
        }
        return new int[0];
    }

    public int[] getIntersectionsWithOneOrMoreRIGHTTurnsPassed() {
        if (outList != null) {
            int i = 0;
            int[] out = new int[outList.size()];
            for (StopToStopInfo s : outList) {
                out[i] = s.getIntersectionsWithOneOrMoreRIGHTTurnsPassed();
                i++;
            }
            return out;
        }
        return new int[0];
    }

    public double[] getAverageActivityArrivalRate() {
        if (outList != null) {
            int i = 0;
            double[] out = new double[outList.size()];
            for (StopToStopInfo s : outList) {
                out[i] = s.getAverageActivityArrivalRate();
                i++;
            }
            return out;
        }
        return new double[0];
    }

    public double[] getAverageActivityDepartureRate() {
        if (outList != null) {
            int i = 0;
            double[] out = new double[outList.size()];
            for (StopToStopInfo s : outList) {
                out[i] = s.getAverageActivityDepartureRate();
                i++;
            }
            return out;
        }
        return new double[0];
    }

    public double[] getAverageActivityCountInProgress() {
        if (outList != null) {
            int i = 0;
            double[] out = new double[outList.size()];
            for (StopToStopInfo s : outList) {
                out[i] = s.getAverageActivityCountInProgress();
                i++;
            }
            return out;
        }
        return new double[0];
    }

    public double[] getAverageActivityGrowthRate() {
        if (outList != null) {
            int i = 0;
            double[] out = new double[outList.size()];
            for (StopToStopInfo s : outList) {
                out[i] = s.getAverageActivityGrowthRate();
                i++;
            }
            return out;
        }
        return new double[0];
    }

    public double[] getAverageActivityTurnOverRate() {
        if (outList != null) {
            int i = 0;
            double[] out = new double[outList.size()];
            for (StopToStopInfo s : outList) {
                out[i] = s.getAverageActivityTurnOverRate();
                i++;
            }
            return out;
        }
        return new double[0];
    }

    public double[] getAverageFacilityCount() {
        if (outList != null) {
            int i = 0;
            double[] out = new double[outList.size()];
            for (StopToStopInfo s : outList) {
                out[i] = s.getAverageFacilityCount();
                i++;
            }
            return out;
        }
        return new double[0];
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

    public void calculateStopStopInfo(String[] fromStops, String[] toStops, String[] routes, String[] lines, String[] times) {
        List<String[]> fromStopsList = CollectionUtils.split(fromStops, threads);
        List<String[]> toStopsList = CollectionUtils.split(toStops, threads);
        List<String[]> routesList = CollectionUtils.split(routes, threads);
        List<String[]> linesList = CollectionUtils.split(lines, threads);
        List<String[]> timesList = CollectionUtils.split(times, threads);
        numThreads = new AtomicInteger(threads);
        List<ParallelQueryExtended> queries = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            ParallelQueryExtended pq = new ParallelQueryExtended(fromStopsList.get(i), toStopsList.get(i), routesList.get(i), linesList.get(i), timesList.get(i));
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

    public void loadNodeAttrs(String file) {
        BufferedReader reader = IOUtils.getBufferedReader(file);
        trafficControlNodes = new HashSet<>();
        boolean done = false;
        try {
            while (!done) {
                String line = reader.readLine();
                if (line != null)
                    trafficControlNodes.add(Id.createNodeId(line));
                else
                    done = true;
            }
        } catch (IOException e) {
            System.err.println("Nodes attribute file not found or something.");
        }
    }

    public double getIntersectionDensity(Node toNode) {
        return intersectionDensities.get(toNode);
    }

    private double getDensityArea() {
        return densityArea;
    }

    public int getRateCalculationWindowSize() {
        return rateCalculationWindowSize;
    }

    public void setRateCalculationWindowSize(int rateCalculationWindowSize) {
        this.rateCalculationWindowSize = rateCalculationWindowSize;
    }

    public void setIsWeightedAverageValues(boolean isWeightedAverageValues) {
        this.isWeightedAverageValues = isWeightedAverageValues;
    }

    public boolean isWeightedAverageValues() {
        return isWeightedAverageValues;
    }

    public void setWeightedAverageValues(boolean isWeightedAverageValues) {
        this.isWeightedAverageValues = isWeightedAverageValues;
    }

    private class DensityCalculator implements Runnable {
        private final List<? extends Node> myNodes;
        private Map<Node, Double> intDensities = new HashMap<>();
        private Map<Node, Set<FacilityActivityCounter>> nodes2ActLevels = new HashMap<>();

        private DensityCalculator(List<? extends Node> myNodes) {
            this.myNodes = myNodes;
        }

        public Map<Node, Set<FacilityActivityCounter>> getNodes2ActLevels() {
            return nodes2ActLevels;
        }

        public Map<Node, Double> getIntDensities() {
            return intDensities;
        }

        private void calculateIntersectionDensityTypeStuff(Node toNode) {
            //nodes per area from the current node
            double density = 0;
            //prevent repeated calculation
            if (this.intDensities.get(toNode) == null) {
                NetworkImpl net = (NetworkImpl) scenario.getNetwork();
                Set<Node> nearestNodes = new HashSet<>();
                nearestNodes.addAll(net.getNearestNodes(toNode.getCoord(), densityDistance));

                double intersectionCount = nearestNodes.size();
                density = intersectionCount / getDensityArea();
                intDensities.put(toNode, density);

                //now, evaluate the various facility activity count measures
//            for all facilities falling within the area
                Set<Link> linksToEvaluate = new HashSet<>();
                for (Node n : nearestNodes) {
                    linksToEvaluate.addAll(n.getOutLinks().values());
                    linksToEvaluate.addAll(n.getInLinks().values());
                }

                //find all facilities to evaluate
                Set<ActivityFacility> facilities = new HashSet<>();
                for (Link l : linksToEvaluate) {
                    if (nearestNodes.contains(l.getFromNode()) && nearestNodes.contains(l.getToNode()) && links2Facilities.containsKey(l)) {
                        facilities.addAll(links2Facilities.get(l));
                    }
                }

//            connect the node with its FacilityActivityCounters
                Set<FacilityActivityCounter> activityCounters = nodes2ActLevels.get(toNode);
                if (activityCounters == null) {
                    activityCounters = new HashSet<>();
                    this.nodes2ActLevels.put(toNode, activityCounters);
                }
                for (ActivityFacility facility : facilities) {
                    activityCounters.add(facilityToActivityLevelAtTimeMap.get(facility.getId()));
                }
            }
        }

        @Override
        public void run() {
            int i = 0;
            int total = myNodes.size();
            for (Node node : myNodes) {
                calculateIntersectionDensityTypeStuff(node);
                i++;
                if (i % (total / 10) == 0)
                    System.out.printf("%d/%d..", i, total);
            }
            numThreads.decrementAndGet();
        }
    }

    private class ActivityLevelAggregator {
        /**
         * For all the nodes between stops, calculate the average activity level encoutnered along the way
         *
         * @param nodesInSequence   all the nodes traversed in between the stops
         * @param time              the departure time for the stop-to-stop combo
         * @param isWeightedAverage use a sum of squares approach if true, otherwise return the sum divided by number of nodes
         * @return
         */
        public double getAverageActivityCountInProgress(Set<Node> nodesInSequence, int time, boolean isWeightedAverage) {
            double out = 0;
            double sum = 0;
            for (Node node : nodesInSequence) {
                Set<FacilityActivityCounter> activityCounters = nodes2ActivityLevels.get(node);
                for (FacilityActivityCounter counter : activityCounters) {
                    //TODO: why are there nulls?
                    if (counter == null)
                        continue;
                    int countInProgress = counter.getActivityCountInProgress(time);
                    out += countInProgress * countInProgress;
                    sum += countInProgress;
                }
            }
            if (sum > 0) {
                if (isWeightedAverage)
                    return out / sum;
                else
                    return sum / (double) nodesInSequence.size();
            }

            return 0;
        }

        /**
         * For all the nodes between stops, calculate the average number of facilities in the nearest neighbour radius
         *
         * @param nodesInSequence   all the nodes traversed in between the stops
         * @param isWeightedAverage use a sum of squares approach if true, otherwise return the sum divided by number of nodes
         * @return
         */
        public double getAverageFacilityCount(Set<Node> nodesInSequence, boolean isWeightedAverage) {
            double out = 0;
            double sum = 0;
            for (Node node : nodesInSequence) {
                Set<FacilityActivityCounter> activityCounters = nodes2ActivityLevels.get(node);
                int facilityCount = activityCounters.size();
                out += facilityCount * facilityCount;
                sum += facilityCount;

            }
            if (sum > 0) {
                if (isWeightedAverage)
                    return out / sum;
                else
                    return sum / (double) nodesInSequence.size();
            }

            return 0;
        }

        /**
         * For all the nodes between stops, calculate the average rate at which number of activities in
         * progress are growing/ declining along the way.
         *
         * @param nodesInSequence   : all the nodes traversed in between the stops
         * @param time              : the departure time for the stop-to-stop combo
         * @param windowSize        : the time in seconds around the used for rate calculation
         * @param isWeightedAverage : use a sum of squares approach if true, otherwise return the sum divided by number of nodes
         * @return
         */
        public double getAverageActivityGrowthRate(Set<Node> nodesInSequence, int time, int windowSize, boolean isWeightedAverage) {
            double out = 0;
            double sum = 0;
            for (Node node : nodesInSequence) {
                Set<FacilityActivityCounter> activityCounters = nodes2ActivityLevels.get(node);
                for (FacilityActivityCounter counter : activityCounters) {
                    //TODO: why are there nulls?
                    if (counter == null)
                        continue;
                    double growthRate = counter.getActivityGrowthRateAtTime(time, windowSize);
                    out += growthRate * growthRate;
                    sum += growthRate;
                }
            }
            if (sum > 0) {
                if (isWeightedAverage)
                    return out / sum;
                else
                    return sum / (double) nodesInSequence.size();
            }

            return 0;
        }

        /**
         * For all the nodes between stops, calculate the average rate at which activities are starting  and stopping along the way.
         *
         * @param nodesInSequence   : all the nodes traversed in between the stops
         * @param time              : the departure time for the stop-to-stop combo
         * @param windowSize        : the time in seconds around the used for rate calculation
         * @param isWeightedAverage : use a sum of squares approach if true, otherwise return the sum divided by number of nodes
         * @return
         */
        public double getAverageActivityTurnOverRate(Set<Node> nodesInSequence, int time, int windowSize, boolean isWeightedAverage) {
            double out = 0;
            double sum = 0;
            for (Node node : nodesInSequence) {
                Set<FacilityActivityCounter> activityCounters = nodes2ActivityLevels.get(node);
                for (FacilityActivityCounter counter : activityCounters) {
                    //TODO: why are there nulls?
                    if (counter == null)
                        continue;
                    double turnOverRate = counter.getActivityTurnOverRateAtTime(time, windowSize);
                    out += turnOverRate * turnOverRate;
                    sum += turnOverRate;
                }
            }
            if (sum > 0) {
                if (isWeightedAverage)
                    return out / sum;
                else
                    return sum / (double) nodesInSequence.size();
            }

            return 0;
        }

        /**
         * For all the nodes between stops, calculate the average rate at which activities are starting  along the way.
         *
         * @param nodesInSequence   : all the nodes traversed in between the stops
         * @param time              : the departure time for the stop-to-stop combo
         * @param windowSize        : the time in seconds around the used for rate calculation
         * @param isWeightedAverage : use a sum of squares approach if true, otherwise return the sum divided by number of nodes
         * @return
         */
        public double getAverageActivityArrivalRate(Set<Node> nodesInSequence, int time, int windowSize, boolean isWeightedAverage) {
            double out = 0;
            double sum = 0;
            for (Node node : nodesInSequence) {
                List<FacilityActivityCounter> activityCounters = new ArrayList<>();
                activityCounters.addAll(nodes2ActivityLevels.get(node));
                for (FacilityActivityCounter counter : activityCounters) {
                    //TODO: why are there nulls?
                    if (counter == null)
                        continue;
                    double arrivalRate = counter.getArrivalRateAtTime(time, windowSize);
                    out += arrivalRate * arrivalRate;
                    sum += arrivalRate;
                }
            }
            if (sum > 0) {
                if (isWeightedAverage)
                    return out / sum;
                else
                    return sum / (double) nodesInSequence.size();
            }

            return 0;
        }

        /**
         * For all the nodes between stops, calculate the average rate at which activities are ending  along the way.
         *
         * @param nodesInSequence   : all the nodes traversed in between the stops
         * @param time              : the departure time for the stop-to-stop combo
         * @param windowSize        : the time in seconds around the used for rate calculation
         * @param isWeightedAverage : use a sum of squares approach if true, otherwise return the sum divided by number of nodes
         * @return
         */
        public double getAverageActivityDepartureRate(Set<Node> nodesInSequence, int time, int windowSize, boolean isWeightedAverage) {
            double out = 0;
            double sum = 0;
            for (Node node : nodesInSequence) {
                Set<FacilityActivityCounter> activityCounters = nodes2ActivityLevels.get(node);
                for (FacilityActivityCounter counter : activityCounters) {
                    //TODO: why are there nulls?
                    if (counter == null)
                        continue;
                    double departureRate = counter.getDepartureRateAtTime(time, windowSize);
                    out += departureRate * departureRate;
                    sum += departureRate;
                }
            }
            if (sum > 0) {
                if (isWeightedAverage)
                    return out / sum;
                else
                    return sum / (double) nodesInSequence.size();
            }

            return 0;
        }
    }

    private class FacilityActivityCounter {
        boolean zeroed = false;

        private TreeMap<Integer, Integer> arrivals = new TreeMap<>();
        private TreeMap<Integer, Integer> departures = new TreeMap<>();
        private TreeMap<Integer, Integer> activityCountinProgress = new TreeMap<>();
        private TreeMap<Integer, Integer> activityCountIncrement = new TreeMap<>();
        private TreeMap<Integer, Integer> turnOverRate;

        private int currentSecondArrivalCount = 0;
        private int currentSecondDepartureCount = 0;
        private int currentTime = 0;

        public void registerArrival(int time) {
            if (currentTime == time && currentSecondArrivalCount + currentSecondDepartureCount > 0)
                currentSecondArrivalCount++;
            else {
                writeTotalsAndResetCounters();
                currentTime = time;
                currentSecondArrivalCount++;
            }
        }

        public void registerDeparture(int time) {
            if (currentTime == time && currentSecondArrivalCount + currentSecondDepartureCount > 0)
                currentSecondDepartureCount++;
            else {
                writeTotalsAndResetCounters();
                currentTime = time;
                currentSecondDepartureCount++;
            }
        }

        /**
         * No more transactions at this facility for the current timestep, so update the treemaps
         */
        public void writeTotalsAndResetCounters() {
            arrivals.put(currentTime, currentSecondArrivalCount);
            departures.put(currentTime, currentSecondDepartureCount);
            activityCountIncrement.put(currentTime, currentSecondArrivalCount - currentSecondDepartureCount);
            activityCountinProgress.put(
                    currentTime,
                    (activityCountinProgress.size() > 0 ? activityCountinProgress.lastEntry().getValue() : 0) +
                            currentSecondArrivalCount - currentSecondDepartureCount
            );
            currentSecondDepartureCount = 0;
            currentSecondArrivalCount = 0;
            currentTime = 0;
        }

        /**
         * Find the arrival/departure rate in a time window of binSize in arrivals/sec by looking at all arrivals registered before the
         * time, or after it if there are not enough arrivals registered in time preceiding the required time.
         *
         * @param time    the time ofthe event
         * @param binSize the number of seconds around time used for rate calculation
         * @return the number of occcurences counted in the window, divided by the window size
         */
        public double getRateAtTime(int time, int binSize, TreeMap<Integer, Integer> treeMap) {
            double out = 0;
            int lastTime = 0;

            Map.Entry<Integer, Integer> timedvalueSeeker = treeMap.lowerEntry(time);

            if (time - timedvalueSeeker.getKey() > binSize)
                return out;

            if (time - timedvalueSeeker.getKey() == binSize)
                return ((double) timedvalueSeeker.getValue()) / (double) binSize;

            while (timedvalueSeeker != null && time - timedvalueSeeker.getKey() < binSize) {
                out += timedvalueSeeker.getValue();
                lastTime = timedvalueSeeker.getKey();
                timedvalueSeeker = treeMap.lowerEntry(lastTime);
            }

            //check if there are enough entries to fit in the window,
//            else repeat the search in the other direction until the window is filled
            if (time - lastTime < binSize) {
                time = lastTime;
                timedvalueSeeker = treeMap.higherEntry(time);
                while (timedvalueSeeker != null && timedvalueSeeker.getKey() - time < binSize) {
                    out += timedvalueSeeker.getValue();
                    lastTime = timedvalueSeeker.getKey();
                    timedvalueSeeker = treeMap.higherEntry(lastTime);
                }
            }

            return out / (double) binSize;
        }

        /**
         * Find the arrival rate in a time window of binSize in arrivals/sec by looking at all arrivals registered before the
         * time, or after it if there are not enough arrivals registered in time preceiding the required time.
         *
         * @param time    the time ofthe event
         * @param binSize the number of seconds around time used for rate calculation
         * @return the number of occcurences counted in the window, divided by the window size
         */
        public double getArrivalRateAtTime(int time, int binSize) {
            return getRateAtTime(time, binSize, arrivals);
        }

        /**
         * Find the departure rate in a time window of binSize
         *
         * @param time    the time ofthe event
         * @param binSize the number of seconds around time used for rate calculation
         * @return the number of occcurences counted in the window, divided by the window size
         */
        public double getDepartureRateAtTime(int time, int binSize) {
            return getRateAtTime(time, binSize, departures);
        }

        /**
         * Find the rate at which a facility is growing/declining in number of activities performed. Calls getRateAtTime().
         *
         * @param time    the time ofthe event
         * @param binSize the number of seconds around time used for rate calculation
         * @return the number of occcurences counted in the window, divided by the window size
         */
        public double getActivityGrowthRateAtTime(int time, int binSize) {
            return getRateAtTime(time, binSize, activityCountIncrement);
        }

        /**
         * Find the rate at which people enter and leave a facility (total rate). Calls getRateAtTime().
         *
         * @param time    the time ofthe event
         * @param binSize the number of seconds around time used for rate calculation
         * @return the number of occcurences counted in the window, divided by the window size
         */
        public double getActivityTurnOverRateAtTime(int time, int binSize) {
            return getRateAtTime(time, binSize, turnOverRate);
        }

        public int getActivityCountInProgress(int time) {
            //find the lowest value of activity count and add it to all values

            return activityCountinProgress.lowerEntry(time) == null ?
                    0 :
                    activityCountinProgress.lowerEntry(time).getValue();
        }

        public void finalizeCounters() {
            if (!zeroed) {
                writeTotalsAndResetCounters();
                int lowestLevel = 0;
                for (int v : activityCountinProgress.values())
                    lowestLevel = min(lowestLevel, v);

                TreeMap<Integer, Integer> zeroedValues = new TreeMap<>();
                for (int k : activityCountinProgress.keySet())
                    zeroedValues.put(k, activityCountinProgress.get(k) + abs(lowestLevel));

                activityCountinProgress = zeroedValues;

                if (turnOverRate == null) {
                    turnOverRate = new TreeMap<>();
                    for (int key : departures.keySet()) {
                        turnOverRate.put(key, departures.get(key) + arrivals.get(key));
                    }
                }
            }
            zeroed = true;

        }
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
        private final String[] times;
        private final String[] fromStops;
        private final String[] toStops;
        private final String[] routes;
        private final String[] lines;
        private final StopToStopInfo[] out;

        public ParallelQueryExtended(String[] fromStops, String[] toStops, String[] routes, String[] lines, String[] times) {
            this.fromStops = fromStops;
            this.toStops = toStops;
            this.routes = routes;
            this.lines = lines;
            this.times = times;
            this.out = new StopToStopInfo[fromStops.length];
        }

        @Override
        public void run() {
            for (int i = 0; i < fromStops.length; i++) {
                out[i] = new StopToStopInfo();
                out[i].setAll(fromStops[i], toStops[i], routes[i], lines[i], times[i]);
                if (i % 1000 == 0)
                    System.out.printf(i + "..");
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
        private double trafficControlCount = 0;
        private double euclideanDistance = 0;
        private int leftTurnsMadeAtIntersections = 0;
        private int rightTurnsMadeAtIntersections = 0;
        private int leftTurnsPassedAtIntersection = 0;
        private int rightTurnsPassedAtIntersection = 0;
        private int intersectionsWithOneOrMoreLEFTTurnsPassed = 0;
        private int intersectionsWithOneOrMoreRIGHTTurnsPassed = 0;
        private double weightedAvgIntersectionComplexity = 0;
        private double totalIntersectionComplexity = 0;
        private double totalWeightedDegreesTurned = 0;
        private Set<Node> nodesTraversed;
        private double averageFacilityCount;
        private double averageActivityDepartureRate;
        private double averageActivityCountInProgress;
        private double averageActivityGrowthRate;
        private double averageActivityTurnOverRate;
        private double averageActivityArrivalRate;

        public int getIntersectionsWithOneOrMoreLEFTTurnsPassed() {
            return intersectionsWithOneOrMoreLEFTTurnsPassed;
        }

        public int getIntersectionsWithOneOrMoreRIGHTTurnsPassed() {
            return intersectionsWithOneOrMoreRIGHTTurnsPassed;
        }

        public double getWeightedAvgIntersectionComplexity() {
            return weightedAvgIntersectionComplexity;
        }

        public double getTotalIntersectionComplexity() {
            return totalIntersectionComplexity;
        }

        public int getLeftTurnsMadeAtIntersections() {
            return leftTurnsMadeAtIntersections;
        }

        public int getRightTurnsMadeAtIntersections() {
            return rightTurnsMadeAtIntersections;
        }

        public int getLeftTurnsPassedAtIntersection() {
            return leftTurnsPassedAtIntersection;
        }

        public int getRightTurnsPassedAtIntersection() {
            return rightTurnsPassedAtIntersection;
        }

        public double getTotalWeightedDegreesTurned() {
            return totalWeightedDegreesTurned;
        }

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

        public double getAverageActivityArrivalRate() {
            return averageActivityArrivalRate;
        }

        public double getAverageActivityDepartureRate() {
            return averageActivityDepartureRate;
        }

        public double getAverageActivityCountInProgress() {
            return averageActivityCountInProgress;
        }

        public double getAverageActivityGrowthRate() {
            return averageActivityGrowthRate;
        }

        public double getAverageActivityTurnOverRate() {
            return averageActivityTurnOverRate;
        }

        public double getAverageFacilityCount() {
            return averageFacilityCount;
        }

        public boolean setAll(String fromStop, String toStop, String route, String line, String time) {
            int departureTime = Integer.parseInt(time);
            nodesTraversed = new HashSet<>();

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

            NetworkRoute networkRoute = scenario.getTransitSchedule().getTransitLines().get(lineId)
                    .getRoutes().get(routeId).getRoute();

            //check a bunch of things all at once while we're at it
            boolean isPreviousNodeControlled = false;
            try {

                subRoute = networkRoute.getSubRoute(fromLink.getId(), toLink.getId());

                distance = 0;

                Link prevLinkInRoute = null;

                int i = 0;

                List<Id<Link>> linkIds = new ArrayList<>();
                linkIds.addAll(subRoute.getLinkIds());
                linkIds.add(toLink.getId());

                for (Id<Link> id : linkIds) {

                    Link link = scenario.getNetwork().getLinks().get(id);

                    if (!link.getAllowedModes().contains(TransportMode.car)) {
                        noCarsDistance += link.getLength();
                    }

                    minCap = min(minCap, link.getCapacity());

                    lengthWeightedAverageLaneCount += link.getLength() * link.getNumberOfLanes();

                    lengthWeightedAverageCapacity += link.getLength() * link.getCapacity();

                    Node toNode = link.getToNode();
                    nodesTraversed.add(toNode);
                    if (trafficControlNodes != null && trafficControlNodes.contains(toNode.getId()) && !isPreviousNodeControlled) {
                        trafficControlCount++;
                        isPreviousNodeControlled = false;
                    }
                    nodeCount++;
                    interSectionDensity += getIntersectionDensity(toNode);

                    if (interSections.contains(toNode.getId())) {
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
                        squeezeCap = max(incap - outcap, squeezeCap);


                    }

                    distance += link.getLength();
                    euclideanDistance += NetworkUtils.getEuclidianDistance(link.getToNode().getCoord(), link.getFromNode().getCoord());
                    freeSpeedTravelTime += link.getLength() > 0 ? link.getLength() / link.getFreespeed() : 0;

                    //link angles
                    if (prevLinkInRoute != null) {
                        totalWeightedDegreesTurned += sqrt(pow(NetworkAngleUtils.getAngleBetweenLinks(prevLinkInRoute, link), 2));
                        if (interSections.contains(prevLinkInRoute.getToNode().getId())) {
                            boolean passedLeftTurn = false;
                            boolean passedRightTurn = false;

                            TreeMap<Double, Link> outLinksSortedByAngle = NetworkAngleUtils.getOutLinksSortedByAngle(prevLinkInRoute);

                            //some links split into bus lane links and messes up the numbers, so add nearly parallel links to the list of links to disregard
                            double straightestLineAbsValue = PI;
                            double straightestLineValue = PI;
                            List<Link> straightestLineLinks = new ArrayList<>();
                            for (double k : outLinksSortedByAngle.keySet()) {
                                if (straightestLineAbsValue > abs(k)) {
                                    straightestLineAbsValue = abs(k);
                                    straightestLineValue = k;
                                }
                            }
                            straightestLineLinks.add(outLinksSortedByAngle.get(straightestLineValue));

                            straightestLineLinks.addAll(NetworkAngleUtils.getNearlyParallelLinks(straightestLineLinks.get(0), minimumAngleDefiningIntersection));

                            for (Map.Entry<Double, Link> e : outLinksSortedByAngle.entrySet()) {
//                            System.out.printf("from, %s to: %s : %f \n",link.getId().toString(), e.getValue().getId().toString(),Math.toDegrees(e.getKey()));
                                if (!straightestLineLinks.contains(e.getValue())) {
                                    if (!e.getValue().getId().equals(link.getId())) {
                                        if (e.getKey() < 0) {
                                            rightTurnsPassedAtIntersection++;
                                            passedRightTurn = true;
                                        } else {
                                            leftTurnsPassedAtIntersection++;
                                            passedLeftTurn = true;
                                        }
                                    } else {
                                        if (e.getKey() < 0)
                                            rightTurnsMadeAtIntersections++;
                                        else
                                            leftTurnsMadeAtIntersections++;
                                    }
                                }
                            }
                            totalIntersectionComplexity += outLinksSortedByAngle.size();
                            weightedAvgIntersectionComplexity += pow(outLinksSortedByAngle.size(), 2);
                            intersectionsWithOneOrMoreLEFTTurnsPassed += passedLeftTurn ? 1 : 0;
                            intersectionsWithOneOrMoreRIGHTTurnsPassed += passedRightTurn ? 1 : 0;
                        }
                    }

                    prevLinkInRoute = link;
                    i++;
                }

                //finalizeCounters some variables
                lengthWeightedAverageLaneCount /= distance;
                lengthWeightedAverageCapacity /= distance;
                weightedAvgIntersectionComplexity /= max(1, totalIntersectionComplexity);
                interSectionDensity /= nodeCount;
                totalWeightedDegreesTurned /= nodeCount;

                //calculate activity-related stuff
                ActivityLevelAggregator aggregator = new ActivityLevelAggregator();
                averageFacilityCount = aggregator.getAverageFacilityCount(nodesTraversed, isWeightedAverageValues);
                averageActivityArrivalRate = aggregator.getAverageActivityArrivalRate(nodesTraversed, departureTime, rateCalculationWindowSize, isWeightedAverageValues);
                averageActivityDepartureRate = aggregator.getAverageActivityDepartureRate(nodesTraversed, departureTime, rateCalculationWindowSize, isWeightedAverageValues);
                averageActivityCountInProgress = aggregator.getAverageActivityCountInProgress(nodesTraversed, departureTime, isWeightedAverageValues);
                averageActivityGrowthRate = aggregator.getAverageActivityGrowthRate(nodesTraversed, departureTime, rateCalculationWindowSize, isWeightedAverageValues);
                averageActivityTurnOverRate = aggregator.getAverageActivityTurnOverRate(nodesTraversed, departureTime, rateCalculationWindowSize, isWeightedAverageValues);

                success = true;
                return success;

            } catch (IllegalArgumentException e) {
                return false;
            }
        }

        public double getTrafficControlCount() {
            return trafficControlCount;
        }

        public double getEuclideanDistance() {
            return euclideanDistance;
        }
    }

    private class EventWriterXMLFiltered extends EventWriterXML {
        public EventWriterXMLFiltered(String filename) {
            super(filename);
        }

        @Override
        public void handleEvent(Event event) {
            if (event.getEventType().equals(ActivityEndEvent.EVENT_TYPE) ||
                    event.getEventType().equals(ActivityStartEvent.EVENT_TYPE))
                super.handleEvent(event);
        }
    }
}


