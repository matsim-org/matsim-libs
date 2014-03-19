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

/**
 * 
 */
package playground.johannes.gsv.demand;

import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.utils.collections.Tuple;

import playground.johannes.coopsim.mental.choice.ChoiceSet;
import playground.johannes.coopsim.util.MatsimCoordUtils;
import playground.johannes.sna.gis.Zone;
import playground.johannes.sna.gis.ZoneLayer;
import playground.johannes.socialnetworks.utils.XORShiftRandom;

/**
 * @author johannes
 *
 */
public class PersonAge implements PopulationTask {

	private final ZoneLayer<ChoiceSet<Tuple<Integer, Integer>>> zones;
	
	private final Random random;
	
	public PersonAge(ZoneLayer<ChoiceSet<Tuple<Integer, Integer>>> zones) {
		this(zones, new XORShiftRandom());
	}
	
	public PersonAge(ZoneLayer<ChoiceSet<Tuple<Integer, Integer>>> zones, Random random) {
		this.zones = zones;
		this.random = random;
	}
	
	@Override
	public void apply(Population pop) {
		for(Person person : pop.getPersons().values()) {
			Coord c = ((Activity)person.getPlans().get(0).getPlanElements().get(0)).getCoord();
			Zone<ChoiceSet<Tuple<Integer, Integer>>> zone = zones.getZone(MatsimCoordUtils.coordToPoint(c));
			ChoiceSet<Tuple<Integer, Integer>> choiceSet = zone.getAttribute();
			
			Tuple<Integer, Integer> category = choiceSet.randomWeightedChoice();
			int lower = category.getFirst();
			int upper = category.getSecond();
			int diff = upper - lower;
			
			int offset = random.nextInt(diff);
			
			int age = lower + offset;
			
			((PersonImpl)person).setAge(age);
		}

	}
	
	

}
