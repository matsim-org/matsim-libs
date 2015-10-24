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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.common.util.ProgressLogger;
import org.matsim.contrib.socnetgen.sna.gis.Zone;
import org.matsim.contrib.socnetgen.sna.gis.ZoneLayer;
import org.matsim.contrib.socnetgen.socialnetworks.utils.XORShiftRandom;

import java.util.Iterator;
import java.util.Random;

/**
 * @author johannes
 *
 */
public class PersonEqualDistribution implements PopulationTask {

	public static final String X_COORD_KEY = "x";
	
	public static final String Y_COORD_KEY = "y";
	
	private Random random;
	
	private ZoneLayer<Integer> zoneLayer;
	
	private GeometryFactory geoFactory = new GeometryFactory();
	
	public PersonEqualDistribution(ZoneLayer<Integer> zones) {
		this(zones, new XORShiftRandom());
	}
	
	public PersonEqualDistribution(ZoneLayer<Integer> zones, Random random) {
		this.zoneLayer = zones;
		this.random = random;
	}
	
	/* (non-Javadoc)
	 * @see playground.johannes.gsv.demand.PopulationTask#apply(org.matsim.api.core.v01.population.Population)
	 */
	@Override
	public void apply(Population pop) {
		ProgressLogger.init(pop.getPersons().size(), 1, 10);
		Iterator<? extends Person> it = pop.getPersons().values().iterator();
		
		for(Zone<Integer> zone : zoneLayer.getZones()) {
			Envelope env = zone.getGeometry().getEnvelopeInternal();
			Integer inhabitants = zone.getAttribute();
			
			for (int i = 0; i < inhabitants; i++) {
				Person person = it.next();
				if(person == null) {
					throw new RuntimeException("Not enough person in population!");
				}
				
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
				
				Plan p = person.getPlans().get(0);
				p.addActivity(pop.getFactory().createActivityFromCoord("home", new Coord(x, y)));
				
				ProgressLogger.step();
			}
			
			
		}
		ProgressLogger.termiante();
	}

}
