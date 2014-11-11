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

package playground.gregor.casim.simulation.physics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import org.apache.log4j.Logger;

/**
 * A priority queue implementation based on: G. Paul, "A complexity O(1)
 * priority queue for event driven molecular dynamics simulations" J.
 * Computational Physics, vol. 221, pp. 615-625, 2007.
 * 
 * @author laemmel
 *
 */
public class CAEventsPaulPriorityQueue {

	private static final Logger log = Logger
			.getLogger(CAEventsPaulPriorityQueue.class);

	private static final int DESIRED_PQ_SZ = 30;

	private double dT = 0.0001;
	private final PriorityQueue<CAEvent> pQ = new PriorityQueue<>();
	private final Map<Integer, LinkedList<CAEvent>> largeQ = new HashMap<>();

	private double tLast = 0;

	public void add(final CAEvent e) {
		final double execTime = e.getEventExcexutionTime();
		double diff = execTime - tLast;
		if (diff < dT) {
			this.pQ.add(e);
		} else {
			int i = (int) (execTime / dT);
			LinkedList<CAEvent> es = this.largeQ.get(i);
			if (es == null) {// lazy initialization
				es = new LinkedList<CAEvent>();
				this.largeQ.put(i, es);
			}
			es.add(e);
		}
	}

	public CAEvent peek() {
		CAEvent peek = pQ.peek();
		if (peek != null) {
			return peek;
		}
		maintain();
		return pQ.peek();

	}

	public CAEvent poll() {
		CAEvent poll = pQ.poll();
		if (poll != null) {
			// this.tLast = poll.getEventExcexutionTime();
			return poll;
		}
		maintain();
		poll = pQ.poll();
		return poll;
	}

	private void maintain() {
		int currentIdx = (int) (tLast / dT);
		currentIdx++;
		while (pQ.peek() == null && largeQ.size() > 0) {
			LinkedList<CAEvent> next = largeQ.remove(currentIdx);
			if (next != null) {
				this.tLast = next.getFirst().getEventExcexutionTime();
				pQ.addAll(next);
				break;
			}
			currentIdx++;
		}
		// if (this.dT > D_T_MIN && pQ.size() > 2 * DESIRED_PQ_SZ) {
		// log.info("PQ size = " + pQ.size() + " performing calibration...");
		// calibrate();
		// }
		// log.info("Calibration done. New dT = " + this.dT + " PQ size = "
		// + this.pQ.size());
		// System.out.println(pQ.size());
	}

	public void rebuilt() {
		List<CAEvent> pseudoSortedCAEvents = new ArrayList<CAEvent>();
		pseudoSortedCAEvents.addAll(pQ);

		int currentIdx = (int) (tLast / dT);
		while (largeQ.size() > 0) {
			LinkedList<CAEvent> next = largeQ.remove(currentIdx);
			if (next != null) {
				pseudoSortedCAEvents.addAll(next);
			}
			currentIdx++;
		}
		Collections.sort(pseudoSortedCAEvents);
		this.tLast = pQ.peek().getEventExcexutionTime();
		pQ.clear();
		Iterator<CAEvent> it = pseudoSortedCAEvents.iterator();
		while (it.hasNext()) {
			CAEvent next = it.next();
			it.remove();
			add(next);
		}
	}
}
