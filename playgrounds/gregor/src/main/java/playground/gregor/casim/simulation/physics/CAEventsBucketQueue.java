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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * a PriorityQueue backed bucket queue implementation, probably it should be
 * defined in a different package and it should be implement the Queue interface
 * .
 * 
 * @author laemmel
 *
 */
public class CAEventsBucketQueue {

	private final Map<Double, Bucket> bucketsMap = new HashMap<>();
	private final PriorityQueue<Bucket> bucketsQueue = new PriorityQueue<>();

	private static final class Bucket {

		LinkedList<CAEvent> events = new LinkedList<CAEvent>();

		public CAEvent peek() {
			return events.peek();
		}

		public CAEvent poll() {
			return events.poll();
		}

		public void add(CAEvent e) {
			this.events.add(e);
		}

	}

}
