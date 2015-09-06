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
package playground.johannes.gsv.demand.tasks;

import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PersonImpl;

import com.vividsolutions.jts.geom.Point;

import playground.johannes.coopsim.util.MatsimCoordUtils;
import playground.johannes.gsv.demand.PersonAttributes;
import playground.johannes.gsv.demand.PopulationTask;
import playground.johannes.sna.gis.Zone;
import playground.johannes.sna.gis.ZoneLayer;
import playground.johannes.socialnetworks.utils.XORShiftRandom;

/**
 * @author johannes
 *
 */
public class PersonGender implements PopulationTask {

	private final ZoneLayer<Double> zones;
	
	private final Random random;
	
	public PersonGender(ZoneLayer<Double> zones) {
		this(zones, new XORShiftRandom());
	}
	
	public PersonGender(ZoneLayer<Double> zones, Random random) {
		this.zones = zones;
		this.random = random;
	}
	
	@Override
	public void apply(Population pop) {
		for(Person person : pop.getPersons().values()) {
			Coord c = ((Activity)person.getPlans().get(0).getPlanElements().get(0)).getCoord();
			Point point = MatsimCoordUtils.coordToPoint(c);
			point.setSRID(4326);
			Zone<Double> zone = zones.getZone(point);
			
			double p = zone.getAttribute();
			
			if(random.nextDouble() < p) {
				PersonImpl.setSex(person, PersonAttributes.FEMALE);
			} else {
				PersonImpl.setSex(person, PersonAttributes.MALE);
			}
		}

	}

}
