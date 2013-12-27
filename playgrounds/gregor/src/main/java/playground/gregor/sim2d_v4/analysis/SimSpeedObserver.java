/* *********************************************************************** *
 * project: org.matsim.*
 * SimSpeedObserver.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.gregor.sim2d_v4.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;

import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.core.utils.collections.Tuple;

public class SimSpeedObserver implements LinkEnterEventHandler {



	LinkedList<Tuple<Double,Long>> times = new LinkedList<Tuple<Double,Long>>();

	@Override
	public void reset(int iteration) {
		//yes this is ugly, but we don't have the time to make it nifty
		if (this.times.size() == 0) {
			return;
		}
			try {
				BufferedWriter bf = new BufferedWriter(new FileWriter(new File("/Users/laemmel/devel/gct_TRB/small_single/runtime2.txt")));
				for (Tuple<Double, Long> t : this.times) {
					bf.append(t.getFirst() + " " + t.getSecond()+"\n");
				}
				bf.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			System.exit(-1);
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		double time = event.getTime();
		if (this.times.isEmpty()) {
			this.times.addLast(new Tuple<Double,Long>(time,System.currentTimeMillis()));
		} else if ((this.times.getLast().getFirst() + 60) <= time) {
			this.times.addLast(new Tuple<Double,Long>(time,System.currentTimeMillis()));
		}
		

	}

}
