/* *********************************************************************** *
 * project: org.matsim.*
 * PlansReaderMatsimV0.java
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

package org.matsim.core.population.io;

import java.util.Locale;
import java.util.Stack;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.misc.Time;
import org.xml.sax.Attributes;

/**
 * A reader for plans files of MATSim according to <code>plans_v0.dtd</code>.
 *
 * @author mrieser
 * @author balmermi
 */
/*package*/ class PopulationReaderMatsimV0 extends MatsimXmlParser implements MatsimReader {

	private final static String PLANS = "plans";
	private final static String DEMAND = "demand";
	private final static String SEGMENT = "segment";
	private final static String MODEL = "model";
	private final static String PARAM = "param";
	private final static String PERSON = "person";
	private final static String PLAN = "plan";
	private final static String ACT = "act";
	private final static String LEG = "leg";
	private final static String ROUTE = "route";

	private final static String ATTR_X100 = "x100";
	private final static String ATTR_Y100 = "y100";

	private final CoordinateTransformation coordinateTransformation;

	private final Population plans;
	private final Network network;
	private Person currperson = null;
	private Plan currplan = null;
	private Leg currleg = null;
	private NetworkRoute currroute = null;

	private Activity prevAct = null;
	private String routeNodes = null;

	private static final Logger log = LogManager.getLogger(PopulationReaderMatsimV0.class);

	protected PopulationReaderMatsimV0(
			final Scenario scenario) {
		this( new IdentityTransformation() , scenario );
	}

	protected PopulationReaderMatsimV0(
			final CoordinateTransformation coordinateTransformation,
			final Scenario scenario) {
		super(ValidationType.DTD_ONLY);
		this.coordinateTransformation = coordinateTransformation;
		this.plans = scenario.getPopulation();
		this.network = scenario.getNetwork();
	}

	@Override
	public void startTag(final String name, final Attributes atts, final Stack<String> context) {
		if (PERSON.equals(name)) {
			startPerson(atts);
		} else if (PLAN.equals(name)) {
			startPlan(atts);
		} else if (ACT.equals(name)) {
			startAct(atts);
		} else if (LEG.equals(name)) {
			startLeg(atts);
		} else if (ROUTE.equals(name)) {
			startRoute();
		} else if (DEMAND.equals(name)) {
			log.info("The tag <demand> is not supported");
		} else if (!SEGMENT.equals(name) && !MODEL.equals(name) && !PARAM.equals(name) && !PLANS.equals(name)) {
			throw new RuntimeException(this + "[tag=" + name + " not known or not supported]");
		}
	}

	@Override
	public void endTag(final String name, final String content, final Stack<String> context) {
		if (PERSON.equals(name)) {
			this.plans.addPerson(this.currperson);
			this.currperson = null;
		} else if (PLAN.equals(name)) {
			this.currplan = null;
		} else if (LEG.equals(name)) {
			this.currleg = null;
		} else if (ROUTE.equals(name)) {
			this.routeNodes = content;
		}
	}

	private void startPerson(final Attributes atts) {
		this.currperson = this.plans.getFactory().createPerson(Id.create(atts.getValue("id"), Person.class));
	}

	private void startPlan(final Attributes atts) {
		String sel = atts.getValue("selected");
		boolean selected;
		if (sel.equals("yes")) {
			selected = true;
		}
		else if (sel.equals("no")) {
			selected = false;
		}
		else {
			throw new NumberFormatException("Attribute 'selected' of Element 'Plan' is neither 'yes' nor 'no'.");
		}
		this.currplan = PersonUtils.createAndAddPlan(this.currperson, selected);
		this.routeNodes = null;

		String scoreString = atts.getValue("score");
		if (scoreString != null) {
			double score = Double.parseDouble(scoreString);
			this.currplan.setScore(score);
		}

	}

	private void startAct(final Attributes atts) {
		if (atts.getValue("zone") != null) {
			log.info("The attribute 'zone' of <act> will be ignored");
		}

		Activity act;
		if (atts.getValue("link") != null) {
			Id<Link> linkId = Id.create(atts.getValue("link"), Link.class);
			final Id<Link> linkId1 = linkId;
			act = PopulationUtils.createAndAddActivityFromLinkId(this.currplan, atts.getValue("type"), linkId1);
			if (atts.getValue(ATTR_X100) != null && atts.getValue(ATTR_Y100) != null) {
				final Coord coord = parseCoord( atts );
				act.setCoord(coord);
			}
		} else if (atts.getValue(ATTR_X100) != null && atts.getValue(ATTR_Y100) != null) {
			final Coord coord = parseCoord( atts );
			act = PopulationUtils.createAndAddActivityFromCoord(this.currplan, atts.getValue("type"), coord);
		} else {
			throw new IllegalArgumentException("Either the coords or the link must be specified for an Act.");
		}
		Time.parseOptionalTime(atts.getValue("start_time"))
				.ifDefinedOrElse(act::setStartTime, act::setStartTimeUndefined);
		Time.parseOptionalTime(atts.getValue("dur"))
				.ifDefinedOrElse(act::setMaximumDuration, act::setMaximumDurationUndefined);
		Time.parseOptionalTime(atts.getValue("end_time"))
				.ifDefinedOrElse(act::setEndTime, act::setEndTimeUndefined);

		if (this.routeNodes != null) {
			this.currroute.setLinkIds(this.prevAct.getLinkId(), NetworkUtils.getLinkIds(RouteUtils.getLinksFromNodes(NetworkUtils.getNodes(this.network, this.routeNodes))), act.getLinkId());
			this.routeNodes = null;
			this.currroute = null;
		}
		this.prevAct = act;
	}

	private Coord parseCoord(Attributes atts) {
		return coordinateTransformation.transform(
				new Coord(
						Double.parseDouble(atts.getValue( ATTR_X100 )),
						Double.parseDouble(atts.getValue( ATTR_Y100 )) ) );
	}

	private void startLeg(final Attributes atts) {
		this.currleg = PopulationUtils.createAndAddLeg( this.currplan, atts.getValue("mode").toLowerCase(Locale.ROOT).intern() );
		Time.parseOptionalTime(atts.getValue("dep_time"))
				.ifDefinedOrElse(currleg::setDepartureTime, currleg::setDepartureTimeUndefined);
		Time.parseOptionalTime(atts.getValue("trav_time"))
				.ifDefinedOrElse(currleg::setTravelTime, currleg::setTravelTimeUndefined);
//		LegImpl r = this.currleg;
//		r.setTravelTime( Time.parseTime(atts.getValue("arr_time")) - r.getDepartureTime() );
		// arrival time is in dtd, but no longer evaluated in code (according to not being in API).  kai, jun'16
	}

	private void startRoute() {
		this.currroute = this.plans.getFactory().getRouteFactories().createRoute(NetworkRoute.class, this.prevAct.getLinkId(), this.prevAct.getLinkId());
		this.currleg.setRoute(this.currroute);
	}

}
