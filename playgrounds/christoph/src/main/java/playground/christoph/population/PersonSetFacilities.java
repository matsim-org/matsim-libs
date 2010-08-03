/* *********************************************************************** *
 * project: org.matsim.*
 * PersonSetFacilities.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.christoph.population;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

public class PersonSetFacilities extends AbstractPersonAlgorithm implements PlanAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private static final Logger log = Logger.getLogger(UpdateCrossboarderPopulation.class);
	
	private static final String HOME = "home";
	private static final String SHOP = "shop";
	private static final String WORK_SECTOR2 = "work_sector2";
	private static final String WORK_SECTOR3 = "work_sector3";
	private static final String LEISURE = "leisure";
	private static final String TTA = "tta";
	
	private final ActivityFacilitiesImpl facilities;
	private QuadTree<Facility> homeQuadTree = null;
	private QuadTree<Facility> shopQuadTree = null;
	private QuadTree<Facility> work2QuadTree = null;
	private QuadTree<Facility> work3QuadTree = null;
	private QuadTree<Facility> leisureQuadTree = null;
	private QuadTree<Facility> ttaQuadTree = null;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonSetFacilities(final ActivityFacilitiesImpl facilities) {
		super();
		log.info("init " + this.getClass().getName() + " module...");
		this.facilities = facilities;
		this.buildQuadTrees();
		log.info("done.");
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private void buildQuadTrees() {
		log.info("building facility quad tree...");
		
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		for (ActivityFacilityImpl f : this.facilities.getFacilities().values()) {
			if (f.getCoord().getX() < minx) { minx = f.getCoord().getX(); }
			if (f.getCoord().getY() < miny) { miny = f.getCoord().getY(); }
			if (f.getCoord().getX() > maxx) { maxx = f.getCoord().getX(); }
			if (f.getCoord().getY() > maxy) { maxy = f.getCoord().getY(); }
		}
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;
		log.info("xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
				
		this.homeQuadTree = new QuadTree<Facility>(minx, miny, maxx, maxy);
		this.shopQuadTree = new QuadTree<Facility>(minx, miny, maxx, maxy);
		this.work2QuadTree = new QuadTree<Facility>(minx, miny, maxx, maxy);
		this.work3QuadTree = new QuadTree<Facility>(minx, miny, maxx, maxy);
		this.leisureQuadTree = new QuadTree<Facility>(minx, miny, maxx, maxy);
		this.ttaQuadTree = new QuadTree<Facility>(minx, miny, maxx, maxy);
		
		for (ActivityFacilityImpl f : this.facilities.getFacilities().values()) {
			if (f.getActivityOptions().get(HOME) != null) {
				this.homeQuadTree.put(f.getCoord().getX(),f.getCoord().getY(),f);
			}
			if (f.getActivityOptions().get(SHOP) != null) {
				this.shopQuadTree.put(f.getCoord().getX(),f.getCoord().getY(),f);
			}
			if (f.getActivityOptions().get(WORK_SECTOR2) != null) {
				this.work2QuadTree.put(f.getCoord().getX(),f.getCoord().getY(),f);
			}
			if (f.getActivityOptions().get(WORK_SECTOR3) != null) {
				this.work3QuadTree.put(f.getCoord().getX(),f.getCoord().getY(),f);
			}
			if (f.getActivityOptions().get(LEISURE) != null) {
				this.leisureQuadTree.put(f.getCoord().getX(),f.getCoord().getY(),f);
			}
			if (f.getActivityOptions().get(TTA) != null) {
				this.ttaQuadTree.put(f.getCoord().getX(),f.getCoord().getY(),f);
			}
			
		}
		log.info("done.");
		
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(Person person) {
		
		for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
			if (planElement instanceof Activity) {
				Activity activity = (Activity) planElement;
				
				Coord coord = activity.getCoord();
				Facility facility = null;
				
				if (activity.getType().equals(HOME)) facility = this.homeQuadTree.get(coord.getX(),coord.getY());
				else if (activity.getType().equals(SHOP)) facility = this.shopQuadTree.get(coord.getX(),coord.getY());
				else if (activity.getType().equals(WORK_SECTOR2)) facility = this.work2QuadTree.get(coord.getX(),coord.getY());
				else if (activity.getType().equals(WORK_SECTOR3)) facility = this.work3QuadTree.get(coord.getX(),coord.getY());
				else if (activity.getType().equals(LEISURE)) facility = this.leisureQuadTree.get(coord.getX(),coord.getY());
				else if (activity.getType().equals(TTA)) facility = this.ttaQuadTree.get(coord.getX(),coord.getY());

				if (facility != null) {
					((ActivityImpl) activity).setFacilityId(facility.getId());
					
					if ( ((CoordImpl)facility.getCoord()).calcDistance(coord) > 500000 ) {
						log.warn("No Facility found within 500 km");
					}
				}
				else log.error("No Facility found!");
			}
		}
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	public void run(Plan plan) {
	}
}
