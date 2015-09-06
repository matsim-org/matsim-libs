/* *********************************************************************** *
 * project: org.matsim.*
 * PlansReaderMatsimV1.java
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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Time;
import org.xml.sax.Attributes;

/**
 * A reader for plans files of MATSim according to <code>plans_v1.dtd</code>.
 *
 * @author mrieser
 * @author balmermi
 */
/*package*/ class PopulationReaderMatsimV1 extends MatsimXmlParser implements
		PopulationReader {

	private final static String PLANS = "plans";
	private final static String PERSON = "person";
	private final static String PLAN = "plan";
	private final static String ACT = "act";
	private final static String LEG = "leg";
	private final static String ROUTE = "route";

	private final static String ATTR_X100 = "x100";
	private final static String ATTR_Y100 = "y100";

	private final Population plans;
	private final Network network;

	private Person currperson = null;

	private PlanImpl currplan = null;

	private LegImpl currleg = null;

	private NetworkRoute currroute = null;
	private String routeNodes = null;

	private ActivityImpl prevAct = null;

	public PopulationReaderMatsimV1(final Scenario scenario) {
		this.plans = scenario.getPopulation();
		this.network = scenario.getNetwork();
	}

	@Override
	public void startTag(final String name, final Attributes atts,
			final Stack<String> context) {
		if (PLANS.equals(name)) {
			startPlans(atts);
		}
		else if (PERSON.equals(name)) {
			startPerson(atts);
		}
		else if (PLAN.equals(name)) {
			startPlan(atts);
		}
		else if (ACT.equals(name)) {
			startAct(atts);
		}
		else if (LEG.equals(name)) {
			startLeg(atts);
		}
		else if (ROUTE.equals(name)) {
			startRoute(atts);
		}
		else {
			throw new RuntimeException(this + "[tag=" + name + " not known or not supported]");
		}
	}

	@Override
	public void endTag(final String name, final String content,
			final Stack<String> context) {
		if (PERSON.equals(name)) {
			this.plans.addPerson(this.currperson);
			this.currperson = null;
		}
		else if (PLAN.equals(name)) {
			if (this.currplan.getPlanElements() instanceof ArrayList) {
				((ArrayList<?>) this.currplan.getPlanElements()).trimToSize();
			}
			this.currplan = null;
		}
		else if (LEG.equals(name)) {
			this.currleg = null;
		}
		else if (ROUTE.equals(name)) {
			this.routeNodes = content;
		}
	}

	/**
	 * Parses the specified plans file. This method calls {@link #parse(String)}.
	 *
	 * @param filename
	 *          The name of the file to parse.
	 */
	@Override
	public void readFile(final String filename) throws UncheckedIOException {
		parse(filename);
	}

	private void startPlans(final Attributes atts) {
		this.plans.setName(atts.getValue("name"));
	}

	private void startPerson(final Attributes atts) {
		this.currperson = PersonImpl.createPerson(Id.create(atts.getValue("id"), Person.class));
		PersonImpl.setSex(this.currperson, atts.getValue("sex"));
		PersonImpl.setAge(this.currperson, Integer.parseInt(atts.getValue("age")));
		PersonImpl.setLicence(this.currperson, atts.getValue("license"));
		PersonImpl.setCarAvail(this.currperson, atts.getValue("car_avail"));
		String employed = atts.getValue("employed");
		if (employed == null) {
			PersonImpl.setEmployed(this.currperson, null);
		} else {
			PersonImpl.setEmployed(this.currperson, "yes".equals(employed));
		}
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
			throw new NumberFormatException(
					"Attribute 'selected' of Element 'Plan' is neither 'yes' nor 'no'.");
		}
		this.currplan = PersonImpl.createAndAddPlan(this.currperson, selected);
		this.routeNodes = null;

		String scoreString = atts.getValue("score");
		if (scoreString != null) {
			double score = Double.parseDouble(scoreString);
			this.currplan.setScore(score);
		}

	}

	private void startAct(final Attributes atts) {
		Id<Link> linkId = null;
		Coord coord = null;
		ActivityImpl act = null;
		if (atts.getValue("link") != null) {
			linkId = Id.create(atts.getValue("link"), Link.class);
			act = this.currplan.createAndAddActivity(atts.getValue("type"), linkId);
			if (atts.getValue(ATTR_X100) != null && atts.getValue(ATTR_Y100) != null) {
				coord = new CoordImpl(atts.getValue(ATTR_X100), atts.getValue(ATTR_Y100));
				act.setCoord(coord);
			}
		} else if (atts.getValue(ATTR_X100) != null && atts.getValue(ATTR_Y100) != null) {
			coord = new CoordImpl(atts.getValue(ATTR_X100), atts.getValue(ATTR_Y100));
			act = this.currplan.createAndAddActivity(atts.getValue("type"), coord);
		} else {
			throw new IllegalArgumentException("Either the coords or the link must be specified for an Act.");
		}
		act.setStartTime(Time.parseTime(atts.getValue("start_time")));
		act.setMaximumDuration(Time.parseTime(atts.getValue("dur")));
		act.setEndTime(Time.parseTime(atts.getValue("end_time")));

		if (this.routeNodes != null) {
			this.currroute.setLinkIds(this.prevAct.getLinkId(), NetworkUtils.getLinkIds(RouteUtils.getLinksFromNodes(NetworkUtils.getNodes(this.network, this.routeNodes))), act.getLinkId());
			this.routeNodes = null;
			this.currroute = null;
		}
		this.prevAct = act;
	}

	private void startLeg(final Attributes atts) {
		this.currleg = this.currplan.createAndAddLeg(atts.getValue("mode").toLowerCase(Locale.ROOT).intern());
		this.currleg.setDepartureTime(Time.parseTime(atts.getValue("dep_time")));
		this.currleg.setTravelTime(Time.parseTime(atts.getValue("trav_time")));
		this.currleg.setArrivalTime(Time.parseTime(atts.getValue("arr_time")));
	}

	private void startRoute(final Attributes atts) {
		this.currroute = (NetworkRoute) ((PopulationFactoryImpl) this.plans.getFactory()).createRoute(TransportMode.car, this.prevAct.getLinkId(), this.prevAct.getLinkId());
		this.currleg.setRoute(this.currroute);
		if (atts.getValue("dist") != null) {
			this.currroute.setDistance(Double.parseDouble(atts.getValue("dist")));
		}
		if (atts.getValue("trav_time") != null) {
			this.currroute.setTravelTime(Time.parseTime(atts.getValue("trav_time")));
		}
	}

}
