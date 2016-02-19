/* *********************************************************************** *
 * project: org.matsim.*
 * PlansReaderMatsimV4.java
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

package org.matsim.core.population;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.xml.sax.Attributes;

/**
 * A reader for plans files of MATSim according to <code>plans_v4.dtd</code>.
 * 
 * @author mrieser
 * @author balmermi
 */
/* package */class PopulationReaderMatsimV4 extends MatsimXmlParser implements PopulationReader {

	/* package */final static String PLANS = "plans";
	/* package */final static String PERSON = "person";
	private final static String TRAVELCARD = "travelcard";
	private final static String DESIRES = "desires";
	private final static String ACTDUR = "actDur";
	private final static String KNOWLEDGE = "knowledge";
	private final static String ACTIVITYSPACE = "activityspace";
	private final static String ACTIVITY = "activity";
	private final static String LOCATION = "location";
	private final static String CAPACITY = "capacity";
	private final static String OPENTIME = "opentime";
	private final static String PLAN = "plan";
	private final static String ACT = "act";
	private final static String LEG = "leg";
	private final static String ROUTE = "route";

	private final static String ATTR_TYPE = "type";

	private final CoordinateTransformation coordinateTransformation;

	/* package*/ final Scenario scenario;
	/* package*/ final Population plans;
	private final Network network;
	private final ActivityFacilities facilities;

	/*package*/ Person currperson = null;
	private String curracttype = null;
	private ActivityOption curractivity = null;
	private PlanImpl currplan = null;
	private ActivityImpl curract = null;
	private LegImpl currleg = null;
	private Route currRoute = null;
	private String routeDescription = null;

	private ActivityImpl prevAct = null;

	private final static Logger log = Logger.getLogger(PopulationReaderMatsimV4.class);

	public PopulationReaderMatsimV4(final Scenario scenario) {
		this(new IdentityTransformation(), scenario);
	}

	public PopulationReaderMatsimV4(
			final CoordinateTransformation coordinateTransformation,
			final Scenario scenario) {
		this.coordinateTransformation = coordinateTransformation;
		this.scenario = scenario;
		this.plans = scenario.getPopulation();
		this.network = scenario.getNetwork();
		this.facilities = scenario.getActivityFacilities();
	}

	@Override
	public void startTag(final String name, final Attributes atts,
						 final Stack<String> context) {
		if (PLANS.equals(name)) {
			startPlans(atts);
		} else if (PERSON.equals(name)) {
			startPerson(atts);
		} else if (TRAVELCARD.equals(name)) {
			startTravelcard(atts);
		} else if (DESIRES.equals(name)) {
			log.error("Desires are no longer supported and will be ignored.");
		} else if (ACTDUR.equals(name)) {
			log.error("Desires are no longer supported and will be ignored.");
		} else if (KNOWLEDGE.equals(name)) {
			// Knowledges are no more
		} else if (ACTIVITYSPACE.equals(name)) {
			log.warn("<activityspace> will be ignored.");
		} else if (ACTIVITY.equals(name)) {
			startActivityFacility(atts);
		} else if (LOCATION.equals(name)) {
			startLocation(atts);
		} else if (CAPACITY.equals(name)) {
			log.warn("<capacity> will be ignored!");
		} else if (OPENTIME.equals(name)) {
			log.warn("<opentime> will be ignored!");
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
	public void endTag(final String name, final String content,
					   final Stack<String> context) {
		if (PERSON.equals(name)) {
			this.plans.addPerson(this.currperson);
			this.currperson = null;
		} else if (ACTIVITY.equals(name)) {
			this.curracttype = null;
		} else if (LOCATION.equals(name)) {
			this.curractivity = null;
		} else if (PLAN.equals(name)) {
			if (this.currplan.getPlanElements() instanceof ArrayList<?>) {
				((ArrayList<?>) this.currplan.getPlanElements()).trimToSize();
			}
			this.currplan = null;
		} else if (ACT.equals(name)) {
			this.prevAct = this.curract;
			this.curract = null;
		} else if (ROUTE.equals(name)) {
			this.routeDescription = content;
		}
	}

	/**
	 * Parses the specified plans file. This method calls {@link #parse(String)}
	 * .
	 *
	 * @param filename The name of the file to parse.
	 */
	@Override
	public void readFile(final String filename) throws UncheckedIOException {
		parse(filename);
	}

	/* package */void startPlans(final Attributes atts) {
		this.plans.setName(atts.getValue("name"));
		if (atts.getValue("reference_layer") != null) {
			log.warn("plans.reference_layer is no longer supported.");
		}
	}

	private void startPerson(final Attributes atts) {
		String ageString = atts.getValue("age");
//		int age = Integer.MIN_VALUE;
		Integer age = null;
		if (ageString != null) age = Integer.parseInt(ageString);
		this.currperson = PopulationUtils.createPerson(Id.create(atts.getValue("id"), Person.class));
		PersonUtils.setSex(this.currperson, atts.getValue("sex"));
		PersonUtils.setAge(this.currperson, age);
		PersonUtils.setLicence(this.currperson, atts.getValue("license"));
		PersonUtils.setCarAvail(this.currperson, atts.getValue("car_avail"));
		String employed = atts.getValue("employed");
		if (employed == null) {
			PersonUtils.setEmployed(this.currperson, null);
		} else {
			PersonUtils.setEmployed(this.currperson, "yes".equals(employed));
		}
	}

	private void startTravelcard(final Attributes atts) {
		PersonUtils.addTravelcard(this.currperson, atts.getValue(ATTR_TYPE));
	}

	private void startActivityFacility(final Attributes atts) {
		this.curracttype = atts.getValue(ATTR_TYPE);
	}

	private void startLocation(final Attributes atts) {
		String type = atts.getValue(ATTR_TYPE);
		String id = atts.getValue("id");
		String x = atts.getValue("x");
		String y = atts.getValue("y");
		String freq = atts.getValue("freq");
		String iP = atts.getValue("isPrimary");
		boolean isPrimary = false;
		if ("yes".equals(iP)) {
			isPrimary = true;
		}

		if (type != null) {
			log.info("Attribute type in <location> is deprecated!");
		}
		if (id == null) {
			throw new RuntimeException("NEW: location must have an id!");
		}
		if ((x != null) || (y != null)) {
			log.info("NEW: coords in <location> will be ignored!");
		}
		if (freq != null) {
			log.info("NEW: Attribute freq in <location> is not supported at the moment!");
		}

		ActivityFacility currfacility = this.facilities.getFacilities().get(Id.create(id, ActivityFacility.class));
		if (currfacility == null) {
			throw new RuntimeException("facility id=" + id + " does not exist!");
		}
		this.curractivity = currfacility.getActivityOptions().get(this.curracttype);
		if (this.curractivity == null) {
			throw new RuntimeException("facility id=" + id + ": Activity of type=" + this.curracttype + " does not exist!");
		}
	}

	private void startPlan(final Attributes atts) {
		String sel = atts.getValue("selected");
		boolean selected;
		if (sel.equals("yes")) {
			selected = true;
		} else if (sel.equals("no")) {
			selected = false;
		} else {
			throw new NumberFormatException("Attribute 'selected' of Element 'Plan' is neither 'yes' nor 'no'.");
		}
		this.routeDescription = null;
		this.currplan = PersonUtils.createAndAddPlan(this.currperson, selected);

		String scoreString = atts.getValue("score");
		if (scoreString != null) {
			double score = Double.parseDouble(scoreString);
			this.currplan.setScore(score);
		}

		String type = atts.getValue(ATTR_TYPE);
		this.currplan.setType(type);
	}

	private void startAct(final Attributes atts) {
		if (atts.getValue("link") != null) {
			Id<Link> linkId = Id.create(atts.getValue("link"), Link.class);
			this.curract = this.currplan.createAndAddActivity(
					atts.getValue(ATTR_TYPE), linkId);
			if ((atts.getValue("x") != null) && (atts.getValue("y") != null)) {
				final Coord coord = parseCoord( atts );
				this.curract.setCoord(coord);
			}
		} else if ((atts.getValue("x") != null) && (atts.getValue("y") != null)) {
			final Coord coord = parseCoord( atts );
			this.curract = this.currplan.createAndAddActivity(
					atts.getValue(ATTR_TYPE), coord);
		} else {
			throw new IllegalArgumentException(
					"In this version of MATSim either the coords or the link must be specified for an Act.");
		}
		this.curract.setStartTime(Time.parseTime(atts.getValue("start_time")));
		this.curract.setMaximumDuration(Time.parseTime(atts.getValue("dur")));
		this.curract.setEndTime(Time.parseTime(atts.getValue("end_time")));
		String fId = atts.getValue("facility");
		if (fId != null) {
			this.curract.setFacilityId(Id.create(fId, ActivityFacility.class));
		}
		if (this.routeDescription != null) {
			Id<Link> startLinkId = null;
			if (this.prevAct.getLinkId() != null) {
				startLinkId = this.prevAct.getLinkId();
			}
			Id<Link> endLinkId = null;
			if (this.curract.getLinkId() != null) {
				endLinkId = this.curract.getLinkId();
			}
			this.currRoute.setStartLinkId(startLinkId);
			this.currRoute.setEndLinkId(endLinkId);
			if (this.currRoute instanceof NetworkRoute) {
				((NetworkRoute) this.currRoute).setLinkIds(startLinkId,
						NetworkUtils.getLinkIds(RouteUtils
								.getLinksFromNodes(NetworkUtils.getNodes(
										this.network, this.routeDescription))),
						endLinkId);
			} else {
				this.currRoute.setRouteDescription(this.routeDescription.trim());
			}
			this.routeDescription = null;
			this.currRoute = null;
		}
	}

	private Coord parseCoord(Attributes atts) {
		return coordinateTransformation.transform(
				new Coord(
						Double.parseDouble(atts.getValue("x")),
						Double.parseDouble(atts.getValue("y")) ) );
	}

	private void startLeg(final Attributes atts) {
		String mode = atts.getValue("mode").toLowerCase(Locale.ROOT);
		if (mode.equals("undef")) {
			mode = "undefined";
		}
		this.currleg = this.currplan.createAndAddLeg(mode.intern());
		this.currleg.setDepartureTime(Time.parseTime(atts.getValue("dep_time")));
		this.currleg.setTravelTime(Time.parseTime(atts.getValue("trav_time")));
		this.currleg.setArrivalTime(Time.parseTime(atts.getValue("arr_time")));
	}

	private void startRoute(final Attributes atts) {
		Class<? extends Route> routeType = GenericRouteImpl.class;
		if ("pt".equals(this.currleg.getMode())) {
			routeType = ExperimentalTransitRoute.class;
		}
		if ("car".equals(this.currleg.getMode())) {
			routeType = NetworkRoute.class;
		}
		this.currRoute = ((PopulationFactoryImpl) this.plans.getFactory()).createRoute(routeType, null, null);
		this.currleg.setRoute(this.currRoute);
		if (atts.getValue("dist") != null) {
			this.currRoute.setDistance(Double.parseDouble(atts.getValue("dist")));
		}
		if (atts.getValue("trav_time") != null) {
			this.currRoute.setTravelTime(Time.parseTime(atts.getValue("trav_time")));
		}
	}

}
