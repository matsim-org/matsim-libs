/* *********************************************************************** *
 * project: org.matsim.*
 * CalcLegTimesKTI.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.meisterk.org.matsim.analysis;

import java.io.PrintStream;
import java.util.TreeMap;

import org.apache.commons.math.stat.Frequency;
import org.apache.commons.math.stat.StatUtils;
import org.apache.commons.math.util.ResizableDoubleArray;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;

import playground.meisterk.org.matsim.population.algorithms.AbstractClassifiedFrequencyAnalysis;

/**
 * Calculates average trip durations by mode.
 *
 * @author meisterk
 *
 */
public class CalcLegTimesKTI extends AbstractClassifiedFrequencyAnalysis implements PersonDepartureEventHandler, PersonArrivalEventHandler {

	private Population population = null;
	private final TreeMap<Id, Double> agentDepartures = new TreeMap<Id, Double>();

	public CalcLegTimesKTI(Population pop, PrintStream out) {
		super(out);
		this.population = pop;
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		this.agentDepartures.put(event.getPersonId(), event.getTime());
	}

	@Override
	public void reset(int iteration) {
		this.rawData.clear();
		this.frequencies.clear();
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		Double depTime = this.agentDepartures.remove(event.getPersonId());
		Person agent = this.population.getPersons().get(event.getPersonId());
		if (depTime != null && agent != null) {

			double travelTime = event.getTime() - depTime;
			String mode = event.getLegMode();

			Frequency frequency = null;
			ResizableDoubleArray rawData = null;
			if (!this.frequencies.containsKey(mode)) {
				frequency = new Frequency();
				this.frequencies.put(mode, frequency);
				rawData = new ResizableDoubleArray();
				this.rawData.put(mode, rawData);
			} else {
				frequency = this.frequencies.get(mode);
				rawData = this.rawData.get(mode);
			}

			frequency.addValue(travelTime);
			rawData.addElement(travelTime);

		}
	}

	public TreeMap<String, Double> getAverageTripDurationsByMode() {
		TreeMap<String, Double> averageTripDurations = new TreeMap<String, Double>();
		for (String mode : this.rawData.keySet()) {
			averageTripDurations.put(mode, StatUtils.mean(this.rawData.get(mode).getElements()));
		}
		return averageTripDurations;
	}

	public double getAverageOverallTripDuration() {

		double overallTripDuration = 0.0;
		int overallNumTrips = 0;

		for (String mode : this.rawData.keySet()) {
			overallTripDuration += StatUtils.sum(this.rawData.get(mode).getElements());
			overallNumTrips += this.rawData.get(mode).getNumElements();
		}

		return (overallTripDuration / overallNumTrips);
	}

	@Override
	public void run(Person person) {
		// not used
	}

}
