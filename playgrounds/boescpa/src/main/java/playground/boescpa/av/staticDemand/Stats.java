/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
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
 * *********************************************************************** *
 */

package playground.boescpa.av.staticDemand;

import org.apache.log4j.Logger;

/**
 * WHAT IS IT FOR?
 * WHAT DOES IT?
 *
 * @author boescpa
 */
public class Stats {
	private static Logger log = Logger.getLogger(Stats.class);

	private final long initialDemand;
	private long unmetDemand;
	private long metDemand;
	private double waitingTimeMetDemand;
	private double maxWaitingTimeMetDemand;

	public Stats(int sizeInitialDemand) {
		initialDemand = sizeInitialDemand;
		unmetDemand = 0;
		metDemand = 0;
		waitingTimeMetDemand = 0;
		maxWaitingTimeMetDemand = 0;
	}

	public void incMetDemand() {
		metDemand++;
	}

	public void incUnmetDemand() {
		unmetDemand++;
	}

	public void incWaitingTimeMetDemand(double waitingTime) {
		if (waitingTime >= 0) {
			waitingTimeMetDemand += waitingTime;
		} else {
			throw new IllegalArgumentException("Negative waiting time!");
		}
	}

	public void setMaxWaitingTimeMetDemand(double waitingTime) {
		if (waitingTime > maxWaitingTimeMetDemand) {
			maxWaitingTimeMetDemand = waitingTime;
		}
	}

	public void printResults() {
		log.info("");
		log.info("RESULTS:");
		log.info(" - Total demand: " + initialDemand);
		log.info(" - Met demand: " + metDemand);
		log.info(" - Average waiting time met demand: " + 0.01*(Math.round(100*(waitingTimeMetDemand / metDemand / 60))) + " min");
		log.info(" - Unmet demand: " + unmetDemand);
	}
}
