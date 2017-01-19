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

package playground.agarwalamit.analysis.toll;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.population.Person;

import playground.agarwalamit.utils.MapUtils;

/**
 * @author amit
 */

public class TollInfoHandler implements PersonMoneyEventHandler {

	private final SortedMap<Double, Map<Id<Person>,Double> >  timeBin2Person2Toll = new TreeMap<>();
	private final double timeBinSize;

	public TollInfoHandler (final double simulationEndTime, final int numberOfTimeBins) {
		this.timeBinSize = simulationEndTime/numberOfTimeBins;
	}

	@Override
	public void reset(int iteration) {
		timeBin2Person2Toll.clear();
	}

	@Override
	public void handleEvent(PersonMoneyEvent event) {

		double endOfTimeInterval = Math.max(1, Math.ceil( event.getTime()/this.timeBinSize) ) * this.timeBinSize;

		if( timeBin2Person2Toll.containsKey(endOfTimeInterval) ) {

			Map<Id<Person>,Double> person2Toll = timeBin2Person2Toll.get(endOfTimeInterval);

			if( person2Toll.containsKey(event.getPersonId()) ) {
				person2Toll.put(event.getPersonId(), event.getAmount() + person2Toll.get(event.getPersonId()) );
			} else {
				person2Toll.put(event.getPersonId(), event.getAmount());
			}

		} else {
			Map<Id<Person>,Double> person2Toll = new HashMap<>();
			person2Toll.put(event.getPersonId(), event.getAmount());
			timeBin2Person2Toll.put(endOfTimeInterval, person2Toll);
		}
	}

	/**
	 * @return time bin to person id to toll value
	 */
	public SortedMap<Double,Map<Id<Person>,Double>> getTimeBin2Person2Toll() {
		return timeBin2Person2Toll;
	}

	/**
	 * @return timeBin to toll values for whole population
	 */
	public SortedMap<Double,Double> getTimeBin2Toll(){
		SortedMap<Double, Double> timebin2Toll = new TreeMap<>();

		timebin2Toll.putAll( this.timeBin2Person2Toll
				.entrySet()
				.stream()
				.collect( Collectors.toMap( e -> e.getKey(), e -> MapUtils.doubleValueSum(e.getValue()) ) ) );
		return timebin2Toll;
	}
}