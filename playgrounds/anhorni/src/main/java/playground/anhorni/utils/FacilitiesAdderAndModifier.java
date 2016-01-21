/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.anhorni.utils;

import java.util.TreeMap;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.ActivityOptionImpl;
import org.matsim.facilities.FacilitiesReaderMatsimV1;

public class FacilitiesAdderAndModifier {
	private final static Logger log = Logger.getLogger(FacilitiesAdderAndModifier.class);


	public void add(Config config, Scenario scenario) {
		new FacilitiesReaderMatsimV1(scenario).readFile(config.getModule("facilities").getValue("inputFacilitiesFile"));
		this.simplifyTypes(scenario);

		log.info("Adapting plans ... of " + scenario.getPopulation().getPersons().size() + " persons");
		this.addfacilities2Plans(scenario);
	}

	private void simplifyTypes(Scenario scenario) {
			for (ActivityFacility facility : scenario.getActivityFacilities().getFacilities().values()) {

			Vector<ActivityOption> options = new Vector<ActivityOption>();

			for (ActivityOption option : facility.getActivityOptions().values()) {
				if (option.getType().startsWith("h")) {
					options.add(this.replaceActOption("h", (ActivityOptionImpl)option, facility));
				}
				else if (option.getType().startsWith("w")) {
					options.add(this.replaceActOption("w", (ActivityOptionImpl)option, facility));
				}
				else if (option.getType().startsWith("e")) {
					options.add(this.replaceActOption("e", (ActivityOptionImpl)option, facility));
				}
				else if (option.getType().startsWith("s")) {
					options.add(this.replaceActOption("s", (ActivityOptionImpl)option, facility));
				}
				else if (option.getType().startsWith("l")) {
					options.add(this.replaceActOption("l", (ActivityOptionImpl)option, facility));
				}
				else {
					options.add(this.replaceActOption("tta", (ActivityOptionImpl)option, facility));
				}
			}
			facility.getActivityOptions().clear();
			for (ActivityOption option : options) {
				facility.getActivityOptions().put(option.getType(), option);
			}
		}
	}

	private ActivityOptionImpl replaceActOption(String type, ActivityOptionImpl option, ActivityFacility facility) {
		ActivityOptionImpl optionNew = new ActivityOptionImpl(type);
		optionNew.setFacility(facility);
		optionNew.setOpeningTimes(option.getOpeningTimes());
		optionNew.setCapacity(option.getCapacity());
		return optionNew;
	}

	private void addfacilities2Plans(Scenario scenario) {
		TreeMap<String, QuadTree<ActivityFacility>> trees = new TreeMap<String, QuadTree<ActivityFacility>>();
		trees.put("h", this.builFacQuadTree("h", scenario.getActivityFacilities().getFacilitiesForActivityType("h")));
		trees.put("w", this.builFacQuadTree("w", scenario.getActivityFacilities().getFacilitiesForActivityType("w")));
		trees.put("e", this.builFacQuadTree("e", scenario.getActivityFacilities().getFacilitiesForActivityType("e")));
		trees.put("s", this.builFacQuadTree("s", scenario.getActivityFacilities().getFacilitiesForActivityType("s")));
		trees.put("l", this.builFacQuadTree("l", scenario.getActivityFacilities().getFacilitiesForActivityType("l")));
		trees.put("tta", this.builFacQuadTree("tta", scenario.getActivityFacilities().getFacilitiesForActivityType("tta")));

		int counter = 0;
		int nextMsg = 1;
		for (Person p : scenario.getPopulation().getPersons().values()) {
			Plan plan = p.getSelectedPlan();
			counter++;
			if (counter % nextMsg == 0) {
				nextMsg *= 2;
				log.info(" person # " + counter);
			}
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					ActivityImpl act = (ActivityImpl)pe;

					if (act.getType().equals("tta")) {
						((ActivityImpl)pe).setFacilityId(
								trees.get("tta").getClosest(act.getCoord().getX(), act.getCoord().getY()).
								getId());
					}
					else {
						log.error("correct for type conversion");
						System.exit(-99);
						
						
//						((ActivityImpl)pe).setFacilityId(
//								trees.get(ActTypeConverter.convert2MinimalType(act.getType())).get(act.getCoord().getX(), act.getCoord().getY())
//								.getId());
					}
				}
			}
		}
	}

	private QuadTree<ActivityFacility> builFacQuadTree(String type, TreeMap<Id<ActivityFacility>, ActivityFacility> facilities_of_type) {
		Gbl.startMeasurement();
		log.info(" building " + type + " facility quad tree");
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;

		for (final ActivityFacility f : facilities_of_type.values()) {
			if (f.getCoord().getX() < minx) { minx = f.getCoord().getX(); }
			if (f.getCoord().getY() < miny) { miny = f.getCoord().getY(); }
			if (f.getCoord().getX() > maxx) { maxx = f.getCoord().getX(); }
			if (f.getCoord().getY() > maxy) { maxy = f.getCoord().getY(); }
		}
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;
		System.out.println("        xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
		QuadTree<ActivityFacility> quadtree = new QuadTree<ActivityFacility>(minx, miny, maxx, maxy);
		for (final ActivityFacility f : facilities_of_type.values()) {
			quadtree.put(f.getCoord().getX(),f.getCoord().getY(),f);
		}
		log.info("Quadtree size: " + quadtree.size());
		return quadtree;
	}
}
