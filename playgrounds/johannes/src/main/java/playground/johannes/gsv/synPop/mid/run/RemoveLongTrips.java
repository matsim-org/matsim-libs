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

package playground.johannes.gsv.synPop.mid.run;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.facilities.ActivityFacility;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.synpop.data.Element;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.ProxyPlan;
import playground.johannes.gsv.synPop.data.DataPool;
import playground.johannes.gsv.synPop.data.FacilityData;
import playground.johannes.gsv.synPop.data.FacilityDataLoader;
import playground.johannes.gsv.synPop.io.XMLParser;
import playground.johannes.gsv.synPop.io.XMLWriter;
import playground.johannes.sna.util.ProgressLogger;
import playground.johannes.socialnetworks.utils.XORShiftRandom;

/**
 * @author johannes
 *
 */
public class RemoveLongTrips {

	private static final Logger logger = Logger.getLogger(RemoveLongTrips.class);
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		XMLParser parser = new XMLParser();
		parser.setValidating(false);
		parser.parse(args[0]);
		
		Set<ProxyPerson> persons = parser.getPersons();
		
		double proba = Double.parseDouble(args[4]);
		double threshold = Double.parseDouble(args[3]);
		
		Random random = new XORShiftRandom(4711);
		
		DataPool dataPool = new DataPool();
		dataPool.register(new FacilityDataLoader(args[2], random), FacilityDataLoader.KEY);
		FacilityData fData = (FacilityData) dataPool.get(FacilityDataLoader.KEY);
		
		Set<ProxyPerson> remove = new HashSet<>(persons.size());
		
		logger.info("Processing persons...");
		
		ProgressLogger.init(persons.size(), 2, 10);
		for(ProxyPerson person : persons) {
			for(ProxyPlan plan : person.getPlans()) {
				for(int i = 0; i < plan.getLegs().size(); i++) {
					Element origin = plan.getActivities().get(i);
					Element destination = plan.getActivities().get(i + 1);
					
					Id<ActivityFacility> id1 = Id.create(origin.getAttribute(CommonKeys.ACTIVITY_FACILITY), ActivityFacility.class);
					Id<ActivityFacility> id2 = Id.create(destination.getAttribute(CommonKeys.ACTIVITY_FACILITY), ActivityFacility.class);
					
					ActivityFacility orgFac = fData.getAll().getFacilities().get(id1); 
					ActivityFacility destFac = fData.getAll().getFacilities().get(id2);

					Coord c1 = orgFac.getCoord();
					Coord c2 = destFac.getCoord();

					double dx = c1.getX() - c2.getX();
					double dy = c1.getY() - c2.getY();
					double d = Math.sqrt(dx*dx + dy*dy); 
					
					if(d > threshold) {
						if(random.nextDouble() < proba) {
							remove.add(person);
							break;
						}
					}
				}
			}
			ProgressLogger.step();
		}
		ProgressLogger.termiante();
		
		logger.info(String.format("Removing %s persons.", remove.size()));
		
		for(ProxyPerson person : remove) {
			persons.remove(person);
		}
		
		logger.info("Writing persons...");
		XMLWriter writer = new XMLWriter();
		writer.write(args[1], persons);
		logger.info("Done.");
	}

}
