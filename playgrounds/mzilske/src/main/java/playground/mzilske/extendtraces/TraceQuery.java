/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * TraceQuery.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package playground.mzilske.extendtraces;

import org.matsim.api.core.v01.Id;
import playground.mzilske.cdr.Sighting;
import playground.mzilske.cdr.Sightings;

import java.util.*;

class TraceQuery {

    private Sightings sightings;
    private double temporalBufferSeconds;
    private double maxDistanceRatio;

    public TraceQuery(Sightings sightings, double epsilon, double maxDistanceRatio) {
        this.sightings = sightings;
        this.temporalBufferSeconds = epsilon;
        this.maxDistanceRatio = maxDistanceRatio;
    }

    private Collection<Id> findMatches(Map.Entry<Id, List<Sighting>> queryParam) {
        Collection<Id> result = new ArrayList<>();
        for (Map.Entry<Id, List<Sighting>> entry : sightings.getSightingsPerPerson().entrySet()) {
            if (matches(queryParam, entry)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    private boolean matches(Map.Entry<Id, List<Sighting>> queryParam, Map.Entry<Id, List<Sighting>> entry) {
        if (entry.getValue().size() <= queryParam.getValue().size()) {
            return false;
        }
        final List<List<Sighting>> matchingSightingsTime = new ArrayList<>();
        for (Sighting sighting : queryParam.getValue()) {
            List<Sighting> sightingsMatchingTime = getSightingsMatchingTime(entry.getValue(), sighting.getTime());
            if (sightingsMatchingTime.isEmpty()) {
                return false;
            }
            matchingSightingsTime.add(sightingsMatchingTime);
        }
        Sighting home = matchingSightingsTime.get(0).get(0);
        Sighting queryHome = queryParam.getValue().get(0);
        Iterator<Sighting> i = queryParam.getValue().iterator();
        for (List<Sighting> window : matchingSightingsTime) {
            if (!containsSightingsMatchingHomeDistance(window, home, distance(queryHome, i.next()))) {
                return false;
            }
        }
        return true;
    }

    private boolean containsSightingsMatchingHomeDistance(List<Sighting> window, Sighting home, double queryDistance) {
        double min = queryDistance * (1.0 - maxDistanceRatio);
        double max = queryDistance * (1.0 + maxDistanceRatio);
        for (Sighting sighting : window) {
            double distance = distance(home, sighting);
            if (distance >= min && distance <= max) {
                return true;
            }
        }
        return false;
    }

    private double distance(Sighting a, Sighting b) {
        return 0.0;
    }

    static class Event implements Comparable<Event> {
        Runnable action;
        Double time;
        Event(Double time, Runnable action) {
            this.action = action;
            this.time = time;
        }
        @Override
        public int compareTo(Event o) {
            return Double.compare(time, o.time);
        }
    }

    static class Window {
        List<Sighting> window;
        Window(List<Sighting> window) {
            this.window = window;
        }
    }

    private boolean matches2(Map.Entry<Id, List<Sighting>> queryParam, Map.Entry<Id, List<Sighting>> entry) {
        if (entry.getValue().size() <= queryParam.getValue().size()) {
            return false;
        }
        final List<List<Sighting>> matchingSightingsTime = new ArrayList<>();
        final List<Window> openWindows = new ArrayList<>();
//        final Set<Window> openWindows = new HashSet<>();
        final PriorityQueue<Event> events = new PriorityQueue<>();
        for (final Sighting sighting : queryParam.getValue()) {
            events.add(new Event(sighting.getTime() - temporalBufferSeconds, new Runnable() {
                @Override
                public void run() {
                    final Window window = new Window(new ArrayList<Sighting>());
                    matchingSightingsTime.add(window.window);
                    openWindows.add(window);
                    events.add(new Event(sighting.getTime() + temporalBufferSeconds, new Runnable() {
                        @Override
                        public void run() {
                            openWindows.remove(window);
                        }
                    }));
                }
            }));
        }
        for (final Sighting sighting : entry.getValue()) {
            events.add(new Event(sighting.getTime(), new Runnable() {
                @Override
                public void run() {
                    for (Window window : openWindows) {
                        window.window.add(sighting);
                    }
                }
            }));
        }
        while (!events.isEmpty()) {
            events.remove().action.run();
        }
        for (List<Sighting> window : matchingSightingsTime) {
            if (window.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private boolean containsSightingsMatchingTime(List<Sighting> value, double time) {
        List<Sighting> sightingsMatchingTime = getSightingsMatchingTime(value, time);
        return sightingsMatchingTime.size() > 0;
    }

    private List<Sighting> getSightingsMatchingTime(List<Sighting> value, double time) {
        List<Sighting> sightingsMatchingTime = new ArrayList<>();
        for (Sighting sighting : value) {
            if (sighting.getTime() > time + temporalBufferSeconds) {
                /*
                    x
                        x keinen gefunden und kommt auch keiner mehr
                        x      x         x
                 */
                break;
            }
            if (sighting.getTime() >= time - temporalBufferSeconds) {
                sightingsMatchingTime.add(sighting);
                break;
            }
            /*
                    x
                x keinen gefunden, aber kommt vielleicht noch einer
                x   x      x         x
            */
        }
        return sightingsMatchingTime;
    }

    void query() {
        int nMatches = 0;
        double time = System.currentTimeMillis();
        for (Map.Entry<Id, List<Sighting>> entry : sightings.getSightingsPerPerson().entrySet()) {
            Collection<Id> matches = findMatches(entry);
            if (!matches.isEmpty()) {
                nMatches++;
            }
        }
        System.out.printf("%d of %d\n", nMatches, sightings.getSightingsPerPerson().entrySet().size());
        System.out.printf("%d seconds\n", (int) ((System.currentTimeMillis() - time) / 1000.0));
    }

}
