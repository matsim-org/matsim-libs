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

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.geometry.CoordImpl;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import playground.johannes.coopsim.mental.choice.ChoiceSet;
import playground.johannes.coopsim.util.MatsimCoordUtils;
import playground.johannes.sna.gis.Zone;
import playground.johannes.sna.gis.ZoneLayer;
import playground.johannes.sna.util.ProgressLogger;
import playground.johannes.socialnetworks.gis.OrthodromicDistanceCalculator;
import playground.johannes.socialnetworks.utils.XORShiftRandom;

/**
 * @author johannes
 *
 */
public class CreatePrimaryActivity implements PopulationTask {

	private final ZoneLayer<Integer> attractivities;
	
	private final Map<Zone<Integer>, ChoiceSet<Zone<Integer>>> choiceSets;
	
	private final Random random = new XORShiftRandom();
	
	private final GeometryFactory geoFactory = new GeometryFactory();
	
	public CreatePrimaryActivity(ZoneLayer<Integer> attractivites) {
		this.attractivities = attractivites;
		
		choiceSets = new HashMap<Zone<Integer>, ChoiceSet<Zone<Integer>>>();
		
	}
	
	/* (non-Javadoc)
	 * @see playground.johannes.gsv.demand.PopulationTask#apply(org.matsim.api.core.v01.population.Population)
	 */
	@Override
	public void apply(Population pop) {
		ProgressLogger.init(pop.getPersons().size(), 5, 10);
		for(Person p : pop.getPersons().values()) {
			Plan plan = p.getPlans().get(0);
			
			Activity homeAct = (Activity) plan.getPlanElements().get(0);
			Point centroid = MatsimCoordUtils.coordToPoint(homeAct.getCoord());
			centroid.setSRID(4326);
			Zone<Integer> homeZone = attractivities.getZone(centroid);
			
			ChoiceSet<Zone<Integer>> choiceSet = choiceSets.get(homeZone);
			if(choiceSet == null) {
				choiceSet = new ChoiceSet<Zone<Integer>>(random);
				for(Zone<Integer> zone : attractivities.getZones()) {
					double d = OrthodromicDistanceCalculator.getInstance().distance(homeZone.getGeometry().getCentroid(), zone.getGeometry().getCentroid());
					double w = Math.pow(d, -1.4) * zone.getAttribute();
					choiceSet.addChoice(zone, w);
				}
				choiceSets.put(homeZone, choiceSet);
				
			}
			
			Zone<Integer> target = choiceSet.randomWeightedChoice();
			Envelope env = target.getGeometry().getEnvelopeInternal();
	
			double x = Double.NaN;
			double y = Double.NaN;
			boolean hit = false;
			while (!hit) {
				x = random.nextDouble() * env.getWidth()	+ env.getMinX();
				y = random.nextDouble() * env.getHeight() + env.getMinY();
				Point point = geoFactory.createPoint(new Coordinate(x, y));

				if (target.getGeometry().contains(point)) {
					hit = true;
				}
			}
			
			plan.addLeg(pop.getFactory().createLeg("undefined"));
			
			plan.addActivity(pop.getFactory().createActivityFromCoord("work", new CoordImpl(x, y)));
			
			ProgressLogger.step();
		}
		ProgressLogger.termiante();
	}

}
