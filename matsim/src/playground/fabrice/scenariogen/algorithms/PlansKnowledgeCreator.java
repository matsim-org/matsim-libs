/* *********************************************************************** *
 * project: org.matsim.*
 * PlansKnowledgeCreator.java
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

package playground.fabrice.scenariogen.algorithms;

import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Double;
import java.util.TreeMap;

import org.matsim.basic.v01.BasicAct;
import org.matsim.basic.v01.BasicPlan.ActIterator;
import org.matsim.facilities.Activity;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.plans.Knowledge;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.plans.algorithms.PlansAlgorithm;
import org.matsim.utils.collections.QuadTree;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.geometry.shared.Coord;
import org.matsim.world.Location;

public class PlansKnowledgeCreator extends PlansAlgorithm {

	TreeMap<String,QuadTree<Facility>> quadtrees = new TreeMap<String,QuadTree<Facility>>();

	@Override
	public void run(Plans plans) {

		buildQuatrees();

		int step=1;
		int numagent=0;
		int numact=0;

		for( Person person : plans.getPersons().values() ){
			Knowledge know = person.getKnowledge();
			Plan plan = person.getPlans().get(0);
			ActIterator it = plan.getIteratorAct();
			while( it.hasNext() ){
				BasicAct act = it.next();
				String act_type = act.getType();
				Coord center = (Coord)((Link)act.getLink()).getCenter();
				Facility facility = quadtrees.get( act.getType() ).get( center.getX(), center.getY());
				know.addActivity(new Activity(act_type, facility));
//				ActivityFacilities actfac = know.createActivityFacility( act_type );
//				actfac.addFacility(facility);
				numact++;
			}
			numagent++;
			if( numagent >= 2*step ){
				System.out.println("Num. agents processed: "+numagent+"\t"+numact+" activities in knowledge");
				step = 2*step;
			}
		}
	}

	void buildQuatrees(){
		System.out.println("  computing the bounding box");
		Facilities facilities = (Facilities)Gbl.getWorld().getLayer(Facilities.LAYER_TYPE);

		GetLayerBoundingBox glbb = new GetLayerBoundingBox( facilities );
		Gbl.getWorld().addAlgorithm(glbb);
		Gbl.getWorld().runAlgorithms();
		Rectangle2D.Double bbox = (Double) glbb.getBoundingBox();
		System.out.println( glbb.getBoundingBox());
		System.out.println("  done.");

		System.out.println(" building the quadtrees");


		// We create one quadtree for each activity type

		for (Location location : facilities.getLocations().values() ){
			Facility facility = (Facility) location;
			CoordI coord = facility.getCenter();
			for( String act_type : facility.getActivities().keySet() ){
				QuadTree<Facility> quad = quadtrees.get( act_type );
				if( quad == null ){
					quad = new QuadTree<Facility>( bbox.x, bbox.y, bbox.x+bbox.width, bbox.y+bbox.height);
					quadtrees.put( act_type, quad );
				}
				quad.put( coord.getX(), coord.getY(), facility );
			}
		}
		System.out.println("  done.");

		for( String act_type : quadtrees.keySet() )
			System.out.println("Quad of act "+ act_type + "\tsize:" + quadtrees.get(act_type).size() );
	}

	void testingQuadtree( QuadTree<Facility> quad, Rectangle2D.Double bbox ){
		System.out.println("  testing quadtree retrieval for 10000 random locations");
		long start = System.currentTimeMillis();
		for( int i=0;i<10000;i++){
			double x = bbox.x+Gbl.random.nextDouble()*bbox.width;
			double y = bbox.y+Gbl.random.nextDouble()*bbox.height;
			Facility facility = quad.get(x, y);
			facility.getId();
		}
		start = (System.currentTimeMillis() - start) / 1000;
		System.out.println(" done in (sec):" + start);
	}
}
