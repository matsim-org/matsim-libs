/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.gregor.ctsim.simulation;

import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * A priority queue implementation based on: G. Paul, "A complexity O(1)
 * priority queue for event driven molecular dynamics simulations" J.
 * Computational Physics, vol. 221, pp. 615-625, 2007.
 *
 * @author laemmel
 */
public class CTEventsPaulPriorityQueue {


    private static final Logger log = Logger
            .getLogger(CTEventsPaulPriorityQueue.class);
    private final PriorityQueue<CTEvent> pQ = new PriorityQueue<>();
    private final Map<Integer, LinkedList<CTEvent>> largeQ = new HashMap<>();
    private double dT = 0.1;
    private double tLast = 0;

    public void add(final CTEvent e) {
        final double execTime = e.getExecTime();
        double diff = execTime - tLast;
        if (diff < dT) {
            this.pQ.add(e);
        }
        else {
            int i = (int) (execTime / dT);
            LinkedList<CTEvent> es = this.largeQ.get(i);
            if (es == null) {// lazy initialization
                es = new LinkedList<CTEvent>();
                this.largeQ.put(i, es);
            }
            es.add(e);
        }
    }

    public CTEvent peek() {
        CTEvent peek = pQ.peek();
        if (peek != null) {
            return peek;
        }
        maintain();
        return pQ.peek();

    }

    private void maintain() {
        int currentIdx = (int) (tLast / dT);
        currentIdx++;
        while (pQ.peek() == null && largeQ.size() > 0) {
            LinkedList<CTEvent> next = largeQ.remove(currentIdx);
            if (next != null) {
                this.tLast = next.getFirst().getExecTime();
                for (CTEvent e : next) {
                    if (!e.isInvalid()) {
                        pQ.add(e);
                    }
                }
//                pQ.addAll(next);
                if (pQ.size() > 0) {
                    break;
                }
            }
            currentIdx++;
        }


//        log.info("PQ size: " + this.pQ.size());
    }

    public CTEvent poll() {
        CTEvent poll = pQ.poll();
        if (poll != null) {
            // this.tLast = poll.getEventExcexutionTime();
            return poll;
        }
        maintain();
        poll = pQ.poll();
        return poll;
    }

}
