/* *********************************************************************** *
 * project: org.matsim.*
 * PersonSetSecLoc.java
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

package playground.thibautd.initialdemandgeneration.old.modules;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.facilities.OpeningTime;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.thibautd.initialdemandgeneration.old.CAtts;

/**
 * Taken from balmermi, and adapted fto be compatible with the current API.
 *
 * Given plans with primary activities locations set properly, it does the
 * following:
 * <ul>
 * <li> It identifies the activities with facility set, which preceed and succeed
 * leisure and shop activities,
 * <li> for shop and leisure activities, it searches facilities around the origin
 * and the destination activities, by increasing incrementaly the search radius
 * <li> as soon as at least one facility is found, a facility is drawn at random,
 * weighted by the facility capacity.
 * </ul>
 *
 * @author thibautd
 */
public class PersonAssignShopLeisureLocations extends AbstractPersonAlgorithm implements PlanAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(PersonAssignShopLeisureLocations.class);

	private static final String L = "l";
	private static final String S = "s";

	/**
	 * when coords of start and end are identical, the initial raidus to search
	 * activities in.
	 */
	private final static double DEFAULT_RADIUS = 1000;
	private final ActivityFacilities facilities;

	private QuadTree<ActivityOption> shopActQuadTree = null;
	private QuadTree<ActivityOption> leisActQuadTree = null;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonAssignShopLeisureLocations(
			final ActivityFacilities facilities,
			final OpeningTime.DayType day) {
		super();
		log.info("    init " + this.getClass().getName() + " module...");
		this.facilities = facilities;
		this.buildShopActQuadTree( day );
		this.buildLeisActQuadTree( day );
		log.info("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// build methods
	//////////////////////////////////////////////////////////////////////

	private void buildShopActQuadTree( final OpeningTime.DayType day ) {
		log.info("      building shop activity quad tree...");
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		ArrayList<ActivityOption> acts = new ArrayList<ActivityOption>();
		for (ActivityFacility f : this.facilities.getFacilities().values()) {
			for (ActivityOption a : f.getActivityOptions().values()) {
				if ( CAtts.ACTS_SHOP.contains( a.getType() ) ||
						isCompatible( (ActivityOptionImpl) a , day ) ) {
					acts.add(a);
					if (f.getCoord().getX() < minx) { minx = f.getCoord().getX(); }
					if (f.getCoord().getY() < miny) { miny = f.getCoord().getY(); }
					if (f.getCoord().getX() > maxx) { maxx = f.getCoord().getX(); }
					if (f.getCoord().getY() > maxy) { maxy = f.getCoord().getY(); }
				}
			}
		}
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;
		log.info("        xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
		this.shopActQuadTree = new QuadTree<ActivityOption>(minx, miny, maxx, maxy);
		for (ActivityOption a : acts) {
			this.shopActQuadTree.put(
					((ActivityOptionImpl) a).getFacility().getCoord().getX(),
					((ActivityOptionImpl) a).getFacility().getCoord().getY(),
					a);
		}
		log.info("      done.");
	}

	private void buildLeisActQuadTree( final OpeningTime.DayType day ) {
		log.info("      building leisure activity quad tree...");
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		ArrayList<ActivityOption> acts = new ArrayList<ActivityOption>();
		for (ActivityFacility f : this.facilities.getFacilities().values()) {
			for (ActivityOption a : f.getActivityOptions().values()) {
				if ( CAtts.ACTS_LEISURE.contains( a.getType() ) ||
						isCompatible( (ActivityOptionImpl) a , day ) ) {
					acts.add(a);
					if (f.getCoord().getX() < minx) { minx = f.getCoord().getX(); }
					if (f.getCoord().getY() < miny) { miny = f.getCoord().getY(); }
					if (f.getCoord().getX() > maxx) { maxx = f.getCoord().getX(); }
					if (f.getCoord().getY() > maxy) { maxy = f.getCoord().getY(); }
				}
			}
		}
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;
		log.info("        xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
		this.leisActQuadTree = new QuadTree<ActivityOption>(minx, miny, maxx, maxy);
		for (ActivityOption a : acts) {
			this.leisActQuadTree.put(
					((ActivityOptionImpl) a).getFacility().getCoord().getX(),
					((ActivityOptionImpl) a).getFacility().getCoord().getY(),
					a);
		}
		log.info("      done.");
	}

	private boolean isCompatible(
			final ActivityOptionImpl option,
			final OpeningTime.DayType day) {
		// avoid warning due to unused params
		if ( option == option && day == day ) {}
		
		throw new RuntimeException("DayType is no longer supported.");
//		for ( OpeningTime.DayType openingDay : option.getOpeningTimes() ) {
//			if ( openingDay.equals( day ) ) return true;
//			switch ( openingDay ) {
//				case wk: return true;
//				case wkday: return day.equals( OpeningTime.DayType.mon ) ||
//							day.equals( OpeningTime.DayType.tue ) ||
//							day.equals( OpeningTime.DayType.wed ) ||
//							day.equals( OpeningTime.DayType.thu ) ||
//							day.equals( OpeningTime.DayType.fri );
//				case wkend: return day.equals( OpeningTime.DayType.sat ) ||
//							day.equals( OpeningTime.DayType.sun );
//				default:
//					break; 
//			}
//		}
//		return false;
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private final ActivityOption getActivity(
			final Collection<ActivityOption> activities,
			final String act_type) {
		ArrayList<String> act_types = new ArrayList<String>();
		if (act_type.startsWith(S)) {
			act_types.addAll( CAtts.ACTS_SHOP );
		}
		else if (act_type.startsWith(L)) {
			act_types.addAll( CAtts.ACTS_LEISURE );
		}
		else {
			throw new RuntimeException("act_type="+act_type+" not allowed!");
		}
		
		ArrayList<ActivityOption> acts = new ArrayList<ActivityOption>();
		ArrayList<Integer> sum_cap_acts = new ArrayList<Integer>();
		int sum_cap = 0;
		for (ActivityOption a : activities) {
			sum_cap += a.getCapacity();
			sum_cap_acts.add(sum_cap);
			acts.add( a );
		}
		int r = MatsimRandom.getRandom().nextInt(sum_cap);
		for (int i=0; i<sum_cap_acts.size(); i++) {
			if (r < sum_cap_acts.get(i)) {
				return acts.get(i);
			}
		}
		throw new RuntimeException("It should never reach this line!");
	}

	//////////////////////////////////////////////////////////////////////

	private final QuadTree<ActivityOption> getActivities(final String act_type) {
		if (act_type.startsWith(S)) {
			return this.shopActQuadTree;
		}
		else if (act_type.startsWith(L)) {
			return this.leisActQuadTree;
		}
		else {
			throw new RuntimeException("act_type=" + act_type + " not allowed!");
		}
	}

	//////////////////////////////////////////////////////////////////////

	private final ActivityOption getActivity(
			final Coord coord,
			final double radius,
			final String act_type) {
		Collection<ActivityOption> acts =
			this.getActivities( act_type ).get(
					coord.getX(),
					coord.getY(),
					radius);
		if (acts.isEmpty()) {
			if (radius > 200000.0) {
				throw new RuntimeException("radius>200'000 meters and still no facility found "+
				"for acttype="+act_type+
				" and coord="+coord);
			}
			return this.getActivity(coord , 2.0 * radius , act_type);
		}
		return this.getActivity( acts , act_type );
	}

	private final ActivityOption getActivity(
			final Coord coord1,
			final Coord coord2,
			final double radius,
			final String act_type) {
		Collection<ActivityOption> acts =
			this.getActivities( act_type ).get(
					coord1.getX(),
					coord1.getY(),
					radius);
		acts.addAll(
				this.getActivities( act_type ).get(
					coord2.getX(),
					coord2.getY(),
					radius));
		if (acts.isEmpty()) {
			if (radius > 200000.0) {
				throw new RuntimeException("radius>200'000 meters and still no facility found "+
				"for acttype="+act_type+
				" and coords={ "+coord1+" ; "+coord2+" }");
			}
			return this.getActivity( coord1 , coord2 , 2.0 * radius , act_type);
		}
		return this.getActivity( acts , act_type );
	}

	//////////////////////////////////////////////////////////////////////

	private final void assignRemainingLocations(
			final ActivityImpl act,
			final ActivityFacility start,
			final ActivityFacility end) {
		Coord c_start = start.getCoord();
		Coord c_end   = end.getCoord();

		double dx = c_end.getX() - c_start.getX();
		double dy = c_end.getX() - c_start.getX();
		if ((dx == 0.0) && (dy == 0.0)) {
			// c_start and c_end equal
			// Zone z = (Zone)start.getUpMapping().values().iterator().next();
			double r = DEFAULT_RADIUS;
			ActivityOptionImpl activity = (ActivityOptionImpl)
				this.getActivity( c_start , r , act.getType());
			act.setType( activity.getType() );
			act.setFacilityId( activity.getFacility().getId() );
			act.setCoord(this.facilities.getFacilities().get(act.getFacilityId()).getCoord());
		}
		else {
			// c_start and c_end different
			double r = Math.sqrt( dx*dx + dy*dy ) / 3.0;
			dx = dx / 6.0;
			dy = dy / 6.0;
			Coord c1 = new CoordImpl(
					c_start.getX() + dx,
					c_start.getY() + dy);
			Coord c2 = new CoordImpl(
					c_end.getX() - dx,
					c_end.getY() + dy);
			ActivityOptionImpl activity = (ActivityOptionImpl)
				this.getActivity( c1 , c2 , r , act.getType() );
			act.setType( activity.getType() );
			act.setFacilityId( activity.getFacility().getId() );
			act.setCoord(
					this.facilities.getFacilities().get( act.getFacilityId() ).getCoord());
		}
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////
	@Override
	public void run(final Person person) {
		if (person.getPlans().size() != 1) {
			throw new RuntimeException("pid="+person.getId()+": There must be exactly one plan.");
		}
		Plan plan = person.getSelectedPlan();
		this.run(plan);
	}

	@Override
	public void run(final Plan plan) {
		for (int i=0; i < plan.getPlanElements().size(); i += 2) {
			ActivityImpl act = (ActivityImpl)plan.getPlanElements().get(i);
			if (act.getFacilityId() == null) {
				// get the prev act with a facility
				ActivityFacility start = null;
				for (int b=i-2; b>=0; b=b-2) {
					ActivityImpl b_act = (ActivityImpl)plan.getPlanElements().get(b);
					if (b_act.getFacilityId() != null) {
						start = this.facilities.getFacilities().get(b_act.getFacilityId());
						break;
					}
				}
				// get the next act with a facility
				ActivityFacility end = null;
				for (int a=i+2; a<plan.getPlanElements().size(); a=a+2) {
					ActivityImpl a_act = (ActivityImpl)plan.getPlanElements().get(a);
					if (a_act.getFacilityId() != null) {
						end = this.facilities.getFacilities().get(a_act.getFacilityId());
						break;
					}
				}
				if ( (start == null) || (end == null) ) {
					throw new RuntimeException("That should not happen!");
				}
				this.assignRemainingLocations( act , start , end );
			}
		}
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////
}

