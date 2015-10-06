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

package playground.agarwalamit.analysis.Toll;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.population.Person;

import playground.agarwalamit.munich.utils.ExtendedPersonFilter;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;

/**
 * @author amit
 */

public class TollInfoHandler implements PersonMoneyEventHandler {

	private SortedMap<UserGroup, SortedMap<Double,Double> > userGrp2TimeBin2Toll = new TreeMap<>();
	private SortedMap<UserGroup, SortedMap<Double, Map<Id<Person>,Double> > > userGrp2TimeBin2Person2Toll = new TreeMap<>();

	private ExtendedPersonFilter pf = new ExtendedPersonFilter();
	private final int noOfTimeBins;
	private final double timeBinSize;

	public TollInfoHandler (double simulationEndTime, int numberOfTimeBins) {
		this.noOfTimeBins = numberOfTimeBins;
		this.timeBinSize = simulationEndTime/this.noOfTimeBins;

		initializeMaps();
	}

	@Override
	public void reset(int iteration) {
		userGrp2TimeBin2Toll.clear();
		userGrp2TimeBin2Person2Toll.clear();

		initializeMaps();
	}

	private void initializeMaps(){
		for (UserGroup ug : UserGroup.values()){
			this.userGrp2TimeBin2Toll.put(ug, new TreeMap<Double, Double>() );
			this.userGrp2TimeBin2Person2Toll.put(ug, new TreeMap<Double, Map<Id<Person>,Double>>() );
		}
	}

	@Override
	public void handleEvent(PersonMoneyEvent event) {
		UserGroup ug = pf.getUserGroupFromPersonId(event.getPersonId());

		Double time = event.getTime(); 
		if(time ==0.0) time = this.timeBinSize;
		double endOfTimeInterval =  Math.ceil(time/timeBinSize)*timeBinSize;

		if( endOfTimeInterval <= 0.0 ) endOfTimeInterval = timeBinSize;

		Map<Double,Double> timeBin2Toll = this.userGrp2TimeBin2Toll.get(ug);
		Map<Double,Map<Id<Person>,Double>> timeBin2PersonId2Toll = this.userGrp2TimeBin2Person2Toll.get(ug);

		if( timeBin2Toll.containsKey(endOfTimeInterval) ) {

			timeBin2Toll.put(endOfTimeInterval, event.getAmount() + timeBin2Toll.get(endOfTimeInterval));

			Map<Id<Person>,Double> person2Toll = timeBin2PersonId2Toll.get(endOfTimeInterval);

			if( person2Toll.containsKey(event.getPersonId()) ) {
				person2Toll.put(event.getPersonId(), event.getAmount() + person2Toll.get(event.getPersonId()) );
			} else {
				person2Toll.put(event.getPersonId(), event.getAmount());
			}

		} else {
			timeBin2Toll.put(endOfTimeInterval, event.getAmount());

			Map<Id<Person>,Double> person2Toll = new HashMap<Id<Person>, Double>();
			person2Toll.put(event.getPersonId(), event.getAmount());
			timeBin2PersonId2Toll.put(endOfTimeInterval, person2Toll);
		}
	}

	/**
	 * @return user group to time bin to to toll value
	 */
	public SortedMap<UserGroup,SortedMap<Double,Double>> getUserGroup2TimeBin2Toll() {
		return userGrp2TimeBin2Toll;
	}

	/**
	 * @return user group to time bin to person id to toll value
	 */
	public SortedMap<UserGroup,SortedMap<Double,Map<Id<Person>,Double>>> getUserGrp2TimeBin2Person2Toll() {
		return userGrp2TimeBin2Person2Toll;
	}

	/**
	 * @return timeBin to toll values for whole population
	 */
	public SortedMap<Double,Double> getTimeBin2Toll(){
		SortedMap<Double, Double> person2Toll = new TreeMap<Double, Double>();

		for (UserGroup ug : UserGroup.values()){

			for (double d :this.userGrp2TimeBin2Toll.get(ug).keySet()){

				if(person2Toll.containsKey(d)) {
					person2Toll.put(d, this.userGrp2TimeBin2Toll.get(ug).get(d) + person2Toll.get(d) );
				} else {
					person2Toll.put(d, this.userGrp2TimeBin2Toll.get(ug).get(d));
				}
			}
		}
		return person2Toll;
	}
}


