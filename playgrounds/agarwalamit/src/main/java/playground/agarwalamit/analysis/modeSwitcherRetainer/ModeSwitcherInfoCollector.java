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

/**
 * A class to store all information of mode switchers/retainers
 * @author amit
 */

 class ModeSwitcherInfoCollector {

	private static final Logger LOG = Logger.getLogger(ModeSwitcherInfoCollector.class);

	private final List<Id<Person>> personIds = new ArrayList<>();
	private double firstIterationStat = 0.;
	private double lastIterationStat = 0.;

	void addPersonToList(final Id<Person> personId) {
		this.personIds.add(personId);
	}

	void addToFirstIterationStats(final double value){
		this.firstIterationStat += value;
	}

	void addToLastIterationStats(final double value){
		this.lastIterationStat += value;
	}

	List<Id<Person>> getPersonIds(){
		return this.personIds;
	}

	int getNumberOfLegs(){
		return this.personIds.size();
	}

	double getFirstIterationStats(){
		return this.firstIterationStat;
	}

	double getLastIterationStats(){
		return this.lastIterationStat;
	}
}
