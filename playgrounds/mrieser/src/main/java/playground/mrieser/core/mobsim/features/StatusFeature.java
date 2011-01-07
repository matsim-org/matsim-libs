/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.mrieser.core.mobsim.features;

import org.apache.log4j.Logger;
import org.matsim.core.utils.misc.Time;

public class StatusFeature implements MobsimFeature {

	private final static Logger log = Logger.getLogger(StatusFeature.class);

	private double nextTime = 0.0;

	private boolean isFirst = true;
	private double firstTime = Double.NaN;
	private long firstRealTime = 0;

	@Override
	public void beforeMobSim() {
	}

	@Override
	public void doSimStep(final double time) {
		if (this.isFirst) {
			this.firstTime = time;
			this.isFirst = false;
			this.firstRealTime = System.currentTimeMillis();
		}
		if (time >= this.nextTime) {
			log.info("Simulation time: " + Time.writeTime(time) + " speed-up: " + ((time - this.firstTime) / ((System.currentTimeMillis() - this.firstRealTime) / 1000.0)));
		}
		while (this.nextTime <= time) {
			this.nextTime += 3600.0;
		}
	}

	@Override
	public void afterMobSim() {
	}

}
