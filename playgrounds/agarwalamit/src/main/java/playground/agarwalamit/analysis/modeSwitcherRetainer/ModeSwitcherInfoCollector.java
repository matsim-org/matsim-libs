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
package playground.agarwalamit.analysis.modeSwitcherRetainer;

import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.collections.Tuple;

/**
 * A class to store all information of mode switchers/retainers
 * @author amit
 */

public class ModeSwitcherInfoCollector {

	private static final Logger LOG = Logger.getLogger(ModeSwitcherInfoCollector.class);

	private final List<Id<Person>> personIds = new ArrayList<>();
	private int numberOfLegs = 0;
	private Tuple<Double, Double> firstAndLastIterationStats = new Tuple<>(new Double(0.), new Double(0.));

	public void addPersonToList(final Id<Person> personId) {
		this.personIds.add(personId);

		// also increase the counter
		numberOfLegs++;
	}

	public void addToFirstIterationStats(final double value){
		firstAndLastIterationStats = new Tuple<>(firstAndLastIterationStats.getFirst() + value,
				firstAndLastIterationStats.getSecond());
	}

	public void addToLastIterationStats(final double value){
		firstAndLastIterationStats = new Tuple<>(firstAndLastIterationStats.getFirst() ,
				firstAndLastIterationStats.getSecond() + value);
	}

	public List<Id<Person>> getPersonIds(){
		return this.personIds;
	}

	public int getNumberOfLegs(){
		return this.numberOfLegs;
	}

	public double getFirstIterationStats(){
		return this.firstAndLastIterationStats.getFirst();
	}

	public double getLastIterationStats(){
		return this.firstAndLastIterationStats.getSecond();
	}
}
