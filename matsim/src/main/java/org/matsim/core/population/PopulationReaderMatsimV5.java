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

package org.matsim.core.population;

import java.util.ArrayList;
import java.util.Stack;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.RouteFactoryImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacility;
import org.matsim.vehicles.Vehicle;
import org.xml.sax.Attributes;

/**
 * A reader for plans files of MATSim according to <code>population_v5.dtd</code>.
 *
 * @author mrieser
 * @author balmermi
 */
public class PopulationReaderMatsimV5 extends MatsimXmlParser implements PopulationReader {

	private final static String POPULATION = "population";
	private final static String PERSON = "person";
	private final static String PLAN = "plan";
	private final static String ACT = "act";
	private final static String LEG = "leg";
	private final static String ROUTE = "route";

	private final static String ATTR_POPULATION_DESC = "desc";
	private final static String ATTR_PERSON_ID = "id";
	private final static String ATTR_PERSON_SEX = "sex";
	private final static String ATTR_PERSON_AGE = "age";
	private final static String ATTR_PERSON_LICENSE = "license";
	private final static String ATTR_PERSON_CARAVAIL = "car_avail";
	private final static String ATTR_PERSON_EMPLOYED = "employed";
	private final static String ATTR_PLAN_SCORE = "score";
	private final static String ATTR_PLAN_TYPE = "type";
	private final static String ATTR_PLAN_SELECTED = "selected";
	private final static String ATTR_ACT_TYPE = "type";
	private final static String ATTR_ACT_X = "x";
	private final static String ATTR_ACT_Y = "y";
	private final static String ATTR_ACT_LINK = "link";
	private final static String ATTR_ACT_FACILITY = "facility";
	private final static String ATTR_ACT_STARTTIME = "start_time";
	private final static String ATTR_ACT_ENDTIME = "end_time";
	private final static String ATTR_ACT_MAXDUR = "max_dur";
	private final static String ATTR_LEG_MODE = "mode";
	private final static String ATTR_LEG_DEPTIME = "dep_time";
	private final static String ATTR_LEG_TRAVTIME = "trav_time";
	private final static String ATTR_LEG_ARRTIME = "arr_time";
	private static final String ATTR_ROUTE_STARTLINK = "start_link";
	private static final String ATTR_ROUTE_ENDLINK = "end_link";

	private final static String VALUE_YES = "yes";
	private final static String VALUE_NO = "no";
	private final static String VALUE_UNDEF = "undef";

	private final CoordinateTransformation coordinateTransformation;

	private final Scenario scenario;
	private final Population plans;

	private Person currperson = null;
	private PlanImpl currplan = null;
	private ActivityImpl curract = null;
	private LegImpl currleg = null;
	private Route currRoute = null;
	private String routeDescription = null;

	private ActivityImpl prevAct = null;

	public PopulationReaderMatsimV5(final Scenario scenario) {
		this( new IdentityTransformation() , scenario );
	}

	public PopulationReaderMatsimV5(
			final CoordinateTransformation coordinateTransformation,
			final Scenario scenario) {
		this.coordinateTransformation = coordinateTransformation;
		this.scenario = scenario;
		this.plans = scenario.getPopulation();
	}

	@Override
	public void startTag(final String name, final Attributes atts, final Stack<String> context) {
		if (POPULATION.equals(name)) {
			startPopulation(atts);
		} else if (PERSON.equals(name)) {
			startPerson(atts);
		} else if (PLAN.equals(name)) {
			startPlan(atts);
		} else if (ACT.equals(name)) {
			startAct(atts);
		} else if (LEG.equals(name)) {
			startLeg(atts);
		} else if (ROUTE.equals(name)) {
			startRoute(atts);
		} else {
			throw new RuntimeException(this + "[tag=" + name + " not known or not supported]");
		}
	}

	@Override
	public void endTag(final String name, final String content, final Stack<String> context) {
		if (PERSON.equals(name)) {
			this.plans.addPerson(this.currperson);
			this.currperson = null;
		} else if (PLAN.equals(name)) {
			if (this.currplan.getPlanElements() instanceof ArrayList<?>) {
				((ArrayList<?>) this.currplan.getPlanElements()).trimToSize();
			}
			this.currplan = null;
		} else if (ACT.equals(name)) {
			this.prevAct = this.curract;
			this.curract = null;
		} else if (ROUTE.equals(name)) {
			endRoute(content);
		}
	}

	/**
	 * Parses the specified plans file. This method calls {@link #parse(String)}.
	 *
	 * @param filename The name of the file to parse.
	 */
	@Override
	public void readFile(final String filename) throws UncheckedIOException {
		parse(filename);
	}

	private void startPopulation(final Attributes atts) {
		this.plans.setName(atts.getValue(ATTR_POPULATION_DESC));
	}

	private void startPerson(final Attributes atts) {
		String ageString = atts.getValue(ATTR_PERSON_AGE);
//		int age = Integer.MIN_VALUE;
		Integer age = null ;
		if (ageString != null)
			age = Integer.parseInt(ageString);
		this.currperson = PopulationUtils.createPerson(Id.create(atts.getValue(ATTR_PERSON_ID), Person.class));
		PersonUtils.setSex(this.currperson, atts.getValue(ATTR_PERSON_SEX));
		PersonUtils.setAge(this.currperson, age);
		PersonUtils.setLicence(this.currperson, atts.getValue(ATTR_PERSON_LICENSE));
		PersonUtils.setCarAvail(this.currperson, atts.getValue(ATTR_PERSON_CARAVAIL));
		String employed = atts.getValue(ATTR_PERSON_EMPLOYED);
		if (employed == null) {
			PersonUtils.setEmployed(this.currperson, null);
		} else {
			PersonUtils.setEmployed(this.currperson, VALUE_YES.equals(employed));
		}
	}

	private void startPlan(final Attributes atts) {
		String sel = atts.getValue(ATTR_PLAN_SELECTED);
		boolean selected;
		if (VALUE_YES.equals(sel)) {
			selected = true;
		}
		else if (VALUE_NO.equals(sel)) {
			selected = false;
		}
		else {
			throw new IllegalArgumentException(
					"Attribute 'selected' of Element 'Plan' is neither 'yes' nor 'no'.");
		}
		this.routeDescription = null;
		this.currplan = PersonUtils.createAndAddPlan(this.currperson, selected);

		String scoreString = atts.getValue(ATTR_PLAN_SCORE);
		if (scoreString != null) {
			double score = Double.parseDouble(scoreString);
			this.currplan.setScore(score);
		}

		String type = atts.getValue(ATTR_PLAN_TYPE);
		if (type != null) {
			this.currplan.setType(type);
		}
	}

	private void startAct(final Attributes atts) {
		if (atts.getValue(ATTR_ACT_LINK) != null) {
			Id<Link> linkId = Id.create(atts.getValue(ATTR_ACT_LINK), Link.class);
			this.curract = this.currplan.createAndAddActivity(atts.getValue(ATTR_ACT_TYPE), linkId);
			if ((atts.getValue(ATTR_ACT_X) != null) && (atts.getValue(ATTR_ACT_Y) != null)) {
				final Coord coord = parseCoord( atts );
				this.curract.setCoord(coord);
			}
		} else if ((atts.getValue(ATTR_ACT_X) != null) && (atts.getValue(ATTR_ACT_Y) != null)) {
			final Coord coord = parseCoord( atts );
			this.curract = this.currplan.createAndAddActivity(atts.getValue(ATTR_ACT_TYPE), coord);
		} else {
			throw new IllegalArgumentException("In this version of MATSim either the coords or the link must be specified for an Act.");
		}
		this.curract.setStartTime(Time.parseTime(atts.getValue(ATTR_ACT_STARTTIME)));
		this.curract.setMaximumDuration(Time.parseTime(atts.getValue(ATTR_ACT_MAXDUR)));
		this.curract.setEndTime(Time.parseTime(atts.getValue(ATTR_ACT_ENDTIME)));
		String fId = atts.getValue(ATTR_ACT_FACILITY);
		if (fId != null) {
			this.curract.setFacilityId(Id.create(fId, ActivityFacility.class));
		}
		if (this.routeDescription != null) {
			finishLastRoute();
		}
	}

	private Coord parseCoord(Attributes atts) {
		return coordinateTransformation.transform(
				new Coord(
						Double.parseDouble(atts.getValue( ATTR_ACT_X )),
						Double.parseDouble(atts.getValue( ATTR_ACT_Y )) ) );
	}

	private void finishLastRoute() {
		Id<Link> startLinkId = null;
		if (this.currRoute.getStartLinkId() != null) {
			startLinkId = this.currRoute.getStartLinkId();
		} else if (this.prevAct.getLinkId() != null) {
			startLinkId = this.prevAct.getLinkId();
		}
		Id<Link> endLinkId = null;
		if (this.currRoute.getEndLinkId() != null) {
			endLinkId = this.currRoute.getEndLinkId();
		} else if (this.curract != null && this.curract.getLinkId() != null) {
			endLinkId = this.curract.getLinkId();
		}

		this.currRoute.setStartLinkId(startLinkId);
		this.currRoute.setEndLinkId(endLinkId);
		this.currRoute.setRouteDescription(this.routeDescription.trim());
		if (Double.isNaN(this.currRoute.getDistance())) {
			if (this.currRoute instanceof NetworkRoute) {
				if (!this.scenario.getNetwork().getLinks().isEmpty()) {
					this.currRoute.setDistance(RouteUtils.calcDistanceExcludingStartEndLink((NetworkRoute) this.currRoute, this.scenario.getNetwork()));
				}
			} else {
				Coord fromCoord = getCoord(this.prevAct);
				Coord toCoord = getCoord(this.curract);
				if (fromCoord != null && toCoord != null) {
					double dist = CoordUtils.calcEuclideanDistance(fromCoord, toCoord);
					if ( this.scenario.getConfig().plansCalcRoute().
							getModeRoutingParams().containsKey(  this.currleg.getMode()  ) ) {
						double estimatedNetworkDistance = dist * this.scenario.getConfig().plansCalcRoute().
								getModeRoutingParams().get( this.currleg.getMode() ).getBeelineDistanceFactor() ;
						this.currRoute.setDistance(estimatedNetworkDistance);
					}
				}
			}
		}
		if (this.currRoute.getTravelTime() == Time.UNDEFINED_TIME) {
			this.currRoute.setTravelTime(this.currleg.getTravelTime());
		}

		this.routeDescription = null;
		this.currRoute = null;

	}

	private Coord getCoord(Activity activity) {
		if (activity == null) {
			return null;
		}
		Coord fromCoord;
		if (activity.getCoord() != null) {
			fromCoord = activity.getCoord();
		} else {
			if (!this.scenario.getNetwork().getLinks().isEmpty()) {
				fromCoord = this.scenario.getNetwork().getLinks().get(activity.getLinkId()).getCoord();
			} else {
				fromCoord = null;
			}
		}
		return fromCoord;
	}

	private void startLeg(final Attributes atts) {
		if (this.routeDescription != null) {
			finishLastRoute();
		}

		String mode = atts.getValue(ATTR_LEG_MODE);
		if (VALUE_UNDEF.equals(mode)) {
			mode = "undefined";
		}
		this.currleg = this.currplan.createAndAddLeg(mode.intern());
		this.currleg.setDepartureTime(Time.parseTime(atts.getValue(ATTR_LEG_DEPTIME)));
		this.currleg.setTravelTime(Time.parseTime(atts.getValue(ATTR_LEG_TRAVTIME)));
		this.currleg.setArrivalTime(Time.parseTime(atts.getValue(ATTR_LEG_ARRTIME)));
	}

	private void startRoute(final Attributes atts) {
		String startLinkId = atts.getValue(ATTR_ROUTE_STARTLINK);
		String endLinkId = atts.getValue(ATTR_ROUTE_ENDLINK);
		String routeType = atts.getValue("type");
		
		if (routeType == null) {
			String legMode = this.currleg.getMode();
			if ("pt".equals(legMode)) {
				routeType = "experimentalPt1";
			} else if ("car".equals(legMode)) {
				routeType = "links";
			} else {
				routeType = "generic";
			}
		}
		
		RouteFactoryImpl factory = ((PopulationFactoryImpl) this.scenario.getPopulation().getFactory()).getRouteFactory();
		Class<? extends Route> routeClass = factory.getRouteClassForType(routeType);
		
		this.currRoute = ((PopulationFactoryImpl) this.scenario.getPopulation().getFactory()).createRoute(
				routeClass, 
				startLinkId == null ? null : Id.create(startLinkId, Link.class), 
						endLinkId == null ? null : Id.create(endLinkId, Link.class));
		this.currleg.setRoute(this.currRoute);

		if (atts.getValue("trav_time") != null) {
			this.currRoute.setTravelTime(Time.parseTime(atts.getValue("trav_time")));
		}
		if (atts.getValue("distance") != null) {
			this.currRoute.setDistance(Double.parseDouble(atts.getValue("distance")));
		}
		if (atts.getValue("vehicleRefId") != null && this.currRoute instanceof NetworkRoute ) {
			((NetworkRoute)this.currRoute).setVehicleId(Id.create(atts.getValue("vehicleRefId"), Vehicle.class));
		}
	}

	private void endRoute(final String content) {
		this.routeDescription = content;

		Id<Link> startLinkId = this.currRoute.getStartLinkId();
		Id<Link> endLinkId = this.currRoute.getEndLinkId();
		this.currRoute.setStartLinkId(startLinkId);
		this.currRoute.setEndLinkId(endLinkId);
		this.currRoute.setRouteDescription(this.routeDescription.trim());
		
		if (Double.isNaN(this.currRoute.getDistance())) {
			if (this.currRoute instanceof NetworkRoute) {
				if (!this.scenario.getNetwork().getLinks().isEmpty()) {
					this.currRoute.setDistance(RouteUtils.calcDistanceExcludingStartEndLink((NetworkRoute) this.currRoute, this.scenario.getNetwork()));
				}
			} else {
				Coord fromCoord = getCoord(this.prevAct);
				Coord toCoord = getCoord(this.curract);
				if (fromCoord != null && toCoord != null) {
					double dist = CoordUtils.calcEuclideanDistance(fromCoord, toCoord);
					if ( this.scenario.getConfig().plansCalcRoute().
							getModeRoutingParams().containsKey(  this.currleg.getMode()  ) ) {
						double estimatedNetworkDistance = dist * this.scenario.getConfig().plansCalcRoute().
								getModeRoutingParams().get( this.currleg.getMode() ).getBeelineDistanceFactor() ;
						this.currRoute.setDistance(estimatedNetworkDistance);
					}
				}
			}
		}
		if (this.currRoute.getTravelTime() == Time.UNDEFINED_TIME) {
			this.currRoute.setTravelTime(this.currleg.getTravelTime());
		}

		if (this.currRoute.getEndLinkId() != null) {
			// this route is complete
			this.currRoute = null;
			this.routeDescription = null;
		}
	}

}
