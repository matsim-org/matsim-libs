package playground.gregor.analysis;/* *********************************************************************** *
 * project: org.matsim.*
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class ArrivalCurves implements PersonArrivalEventHandler, PersonDepartureEventHandler {


    private final List<TimeSlot> lst;
    private Map<Id<Person>, PersonDepartureEvent> deps = new HashMap<>();
    private TimeSlot current;

    public ArrivalCurves(List<TimeSlot> lst) {
        this.lst = lst;
        current = new TimeSlot();
        lst.add(current);
    }


    public static void main(String[] args) throws IOException {

        TreeMap<Double, Map<String, TimeSlot>> arrivals = new TreeMap<>();

        List<String> runs = new ArrayList<>();
        String run1358Sp = "/Users/laemmel/svn/runs-svn/run1358/output/ITERS/it.0/0.events.txt.gz";
        runs.add(run1358Sp);
        String run1358Nash = "/Users/laemmel/svn/runs-svn/run1358/output/ITERS/it.1000/1000.events.txt.gz";
        runs.add(run1358Nash);
        String rerun1358Sp = "/Users/laemmel/scenarios/misanthrope/paper/sp/output/ITERS/it.0/0.events.xml.gz";
        runs.add(rerun1358Sp);
        String rerun1358Nash = "/Users/laemmel/scenarios/misanthrope/paper/nash/output/ITERS/it.0/0.events.xml.gz";
        runs.add(rerun1358Nash);

        for (String run : runs) {
            List<TimeSlot> ts = new ArrayList<>();
            ArrivalCurves arrivalCurves = new ArrivalCurves(ts);

            EventsManager em = new EventsManagerImpl();
            em.addHandler(arrivalCurves);
            try {
                new MatsimEventsReader(em).readFile(run);
            } catch (Exception e) {

                try {
                    new TxtEventsFileReader(em).runEventsFile(run);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }


            ts.forEach(timeSlot -> {
                Map<String, TimeSlot> map = arrivals.computeIfAbsent(timeSlot.time, k -> new HashMap<>());
                map.put(run, timeSlot);
            });
        }


        BufferedWriter bw = new BufferedWriter(new FileWriter(new File("/Users/laemmel/scenarios/misanthrope/paper/evac_curves")));

        Map<String, Integer> last = new HashMap<>();
        for (Map.Entry<Double, Map<String, TimeSlot>> e : arrivals.entrySet()) {
            StringBuffer bf = new StringBuffer();
            bf.append(e.getKey());

            for (String run : runs) {
                TimeSlot ts = e.getValue().get(run);
                bf.append('\t');
                if (ts != null) {
                    bf.append(ts.arrived);
                    last.put(run, ts.arrived);
                } else {
                    Integer arr = last.computeIfAbsent(run, k -> 0);
                    bf.append(arr);
                }
            }
            bw.append(bf.toString());
            bw.append('\n');
            System.out.println(bf.toString());
        }

        bw.close();


    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        PersonDepartureEvent dep = deps.remove(event.getPersonId());
        double tt = event.getTime() - dep.getTime();

        if (tt > current.time) {
            TimeSlot timeSlot = new TimeSlot();
            timeSlot.time = tt;
            timeSlot.arrived = current.arrived;
            lst.add(timeSlot);
            current = timeSlot;
        }
        current.arrived++;
    }

    @Override
    public void reset(int iteration) {

    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        deps.put(event.getPersonId(), event);

    }

    private static final class TimeSlot {
        double time;
        int arrived = 0;

        @Override
        public String toString() {
            return arrived + " ";
        }
    }
}
