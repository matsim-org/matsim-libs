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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.socnetgen.sna.gis.Zone;
import org.matsim.contrib.socnetgen.sna.gis.ZoneLayer;
import org.matsim.contrib.socnetgen.socialnetworks.utils.XORShiftRandom;
import playground.johannes.gsv.demand.ActivityTypes;
import playground.johannes.gsv.demand.PopulationTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * @author johannes
 *
 */
public class PersonEqualZoneDistribution implements PopulationTask {

	private final ZoneLayer<Double> zones;
	
	private final Random random;
	
	private final GeometryFactory geoFactory = new GeometryFactory();
	
	public PersonEqualZoneDistribution(ZoneLayer<Double> zones) {
		this(zones, new XORShiftRandom());
	}
	
	public PersonEqualZoneDistribution(ZoneLayer<Double> zones, Random random) {
		this.zones = zones;
		this.random = random;
	}
	
	@Override
	public void apply(Population pop) {
		int N = pop.getPersons().size();
		/*
		 * shuffle persons
		 */
		List<Person> persons = new ArrayList<Person>(pop.getPersons().values());
		Collections.shuffle(persons);
		
		int processed = 0;
		for(Zone<Double> zone : zones.getZones()) {
			Envelope env = zone.getGeometry().getEnvelopeInternal();
			double fraction = zone.getAttribute();
			/*
			 * number of persons to create;
			 */
			int n = (int) Math.ceil(N * fraction);
			
			for(int i = 0; i < n; i++) {
				/*
				 * check if all persons already processed
				 */
				if(processed >= N) {
					break;
				}
				/*
				 * draw random coordinate and check if in zone
				 */
				double x = Double.NaN;
				double y = Double.NaN;
				boolean hit = false;
				while (!hit) {
					x = random.nextDouble() * env.getWidth()	+ env.getMinX();
					y = random.nextDouble() * env.getHeight() + env.getMinY();
					Point p = geoFactory.createPoint(new Coordinate(x, y));

					if (zone.getGeometry().contains(p)) {
						hit = true;
					}
				}
				/*
				 * add home activity to first plan
				 */
				Person person = persons.get(processed);
				Plan p = person.getPlans().get(0);
				p.addActivity(pop.getFactory().createActivityFromCoord(ActivityTypes.HOME, new Coord(x, y)));
				
				processed++;
			}
		}
		
		if(processed < N) {
			// i think this should never happen because we use ceil(N * fraction)
			throw new RuntimeException(String.format("%s persons unprocessed.", N - processed));
		}
	}

}
