/* *********************************************************************** *
 * project: org.matsim.*
 * PopulationWithJointTripsReader.java
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
package playground.thibautd.jointtripsoptimizer.population;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.routes.GenericRoute;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.core.utils.misc.RouteUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.knowledges.KnowledgeImpl;
import org.matsim.knowledges.Knowledges;
import org.matsim.knowledges.KnowledgesImpl;
import org.matsim.population.Desires;
import org.xml.sax.Attributes;

/**
 * VERY QUICK implementation of an xml reader for importing all necesary data
 * to reconstruct a population with joint plans.
 * <BR>
 * It is mainly a copy of org.matsim.core.population.PopulationReaderMatsimV4,
 * without check of the types of the individual plans.
 * <BR>
 * At export, the individual plan's types are set so as to identify clearly linked
 * plans. This reader allows to load this data.
 * <BR>
 * This is a quick way of importing this data: in the future, more elegant ways to
 * store and load joint activity information should be found.
 *
 * @author thibautd
 */
public class PopulationWithJointTripsReader extends MatsimXmlParser implements PopulationReader {

	private final static String PLANS = "plans";
	private final static String PERSON = "person";
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

	private final Scenario scenario;
	private final Population plans;
	private final Network network;
	private final ActivityFacilities facilities;
	private final Knowledges knowledges;

	private PersonImpl currperson = null;
	private Desires currdesires = null;
	private KnowledgeImpl currknowledge = null;
	private String curracttype = null;
	private ActivityOption curractivity = null;
	private PlanImpl currplan = null;
	private ActivityImpl curract = null;
	private LegImpl currleg = null;
	private Route currRoute = null;
	private String routeDescription = null;

	private ActivityImpl prevAct = null;

	private final static Logger log = Logger.getLogger(PopulationWithJointTripsReader.class);

	private int warnPlanTypeCount = 0;

	public PopulationWithJointTripsReader(final Scenario scenario) {
		this.scenario = scenario;
		this.plans = scenario.getPopulation();
		this.network = scenario.getNetwork();
		if (scenario instanceof ScenarioImpl) {
			this.facilities = ((ScenarioImpl) scenario).getActivityFacilities();
			Knowledges k = ((ScenarioImpl) scenario).getKnowledges();
			if (k == null) {
				this.knowledges = new KnowledgesImpl(); // we need knowledges to read in a file
			} else {
				this.knowledges = k;
			}
		} else {
			this.facilities = null;
			this.knowledges = new KnowledgesImpl();
		}
	}

	@Override
	public void startTag(final String name, final Attributes atts, final Stack<String> context) {
		if (PLANS.equals(name)) {
			startPlans(atts);
		} else if (PERSON.equals(name)) {
			startPerson(atts);
		} else if (TRAVELCARD.equals(name)) {
			startTravelcard(atts);
		} else if (DESIRES.equals(name)) {
			startDesires(atts);
		} else if (ACTDUR.equals(name)) {
			startActDur(atts);
		} else if (KNOWLEDGE.equals(name)) {
			startKnowledge(atts);
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
			Gbl.errorMsg(this + "[tag=" + name + " not known or not supported]");
		}
	}

	@Override
	public void endTag(final String name, final String content, final Stack<String> context) {
		if (PERSON.equals(name)) {
			if (this.plans instanceof PopulationImpl) {
				((PopulationImpl) this.plans).addPerson(this.currperson);
			} else {
				this.plans.addPerson(this.currperson);
			}
			this.currperson = null;
		} else if (DESIRES.equals(name)) {
			this.currdesires = null;
		} else if (KNOWLEDGE.equals(name)) {

				this.currknowledge = null;
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
	 * Parses the specified plans file. This method calls {@link #parse(String)}.
	 *
	 * @param filename The name of the file to parse.
	 */
	@Override
	public void readFile(final String filename) throws UncheckedIOException {
		parse(filename);
	}

	private void startPlans(final Attributes atts) {
		this.plans.setName(atts.getValue("name"));
		if (atts.getValue("reference_layer") != null) {
			log.warn("plans.reference_layer is no longer supported.");
		}
	}

	private void startPerson(final Attributes atts) {
		String ageString = atts.getValue("age");
		int age = Integer.MIN_VALUE;
		if (ageString != null)
			age = Integer.parseInt(ageString);
		this.currperson = new PersonImpl(this.scenario.createId(atts.getValue("id")));
		this.currperson.setSex(atts.getValue("sex"));
		this.currperson.setAge(age);
		this.currperson.setLicence(atts.getValue("license"));
		this.currperson.setCarAvail(atts.getValue("car_avail"));
		String employed = atts.getValue("employed");
		if (employed == null) {
			this.currperson.setEmployed(null);
		} else {
			this.currperson.setEmployed("yes".equals(employed));
		}
	}

	private void startTravelcard(final Attributes atts) {
		this.currperson.addTravelcard(atts.getValue(ATTR_TYPE));
	}

	private void startDesires(final Attributes atts) {
		this.currdesires = this.currperson.createDesires(atts.getValue("desc"));
	}

	private void startActDur(final Attributes atts) {
		this.currdesires.putActivityDuration(atts.getValue(ATTR_TYPE),atts.getValue("dur"));
	}

	private void startKnowledge(final Attributes atts) {
		this.currknowledge = this.knowledges.getFactory().createKnowledge(this.currperson.getId(), atts.getValue("desc"));
		this.knowledges.getKnowledgesByPersonId().put(this.currperson.getId(), this.currknowledge);
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
		if ("yes".equals(iP)) { isPrimary = true; }

		if (type != null) { log.info("Attribute type in <location> is deprecated!"); }
		if (id == null) { Gbl.errorMsg("NEW: location must have an id!"); }
		if ((x != null) || (y != null)) { log.info("NEW: coords in <location> will be ignored!"); }
		if (freq != null) { log.info("NEW: Attribute freq in <location> is not supported at the moment!"); }

		ActivityFacility currfacility = this.facilities.getFacilities().get(this.scenario.createId(id));
		if (currfacility == null) { Gbl.errorMsg("facility id=" + id + " does not exist!"); }
		this.curractivity = currfacility.getActivityOptions().get(this.curracttype);
		if (this.curractivity == null) { Gbl.errorMsg("facility id=" + id + ": Activity of type=" + this.curracttype + " does not exist!"); }
		this.currknowledge.addActivityOption(this.curractivity,isPrimary);
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
		this.routeDescription = null;
		this.currplan = this.currperson.createAndAddPlan(selected);

		String scoreString = atts.getValue("score");
		if (scoreString != null) {
			double score = Double.parseDouble(scoreString);
			this.currplan.setScore(score);
		}

		// BEGINNING OF MODIFIED PART
		String type = atts.getValue(ATTR_TYPE);
		if (type == null) {
			this.currplan.setType("undefined");
		}
		else {
			this.currplan.setType(type);
		}
		// END OF MODIFIED PART
	}

	private void startAct(final Attributes atts) {
		Coord coord = null;
		if (atts.getValue("link") != null) {
			Id linkId = this.scenario.createId(atts.getValue("link"));
			this.curract = this.currplan.createAndAddActivity(atts.getValue(ATTR_TYPE), linkId);
			if ((atts.getValue("x") != null) && (atts.getValue("y") != null)) {
				coord = new CoordImpl(atts.getValue("x"), atts.getValue("y"));
				this.curract.setCoord(coord);
			}
		} else if ((atts.getValue("x") != null) && (atts.getValue("y") != null)) {
			coord = new CoordImpl(atts.getValue("x"), atts.getValue("y"));
			this.curract = this.currplan.createAndAddActivity(atts.getValue(ATTR_TYPE), coord);
		} else {
			throw new IllegalArgumentException("In this version of MATSim either the coords or the link must be specified for an Act.");
		}
		this.curract.setStartTime(Time.parseTime(atts.getValue("start_time")));
		this.curract.setMaximumDuration(Time.parseTime(atts.getValue("dur")));
		this.curract.setEndTime(Time.parseTime(atts.getValue("end_time")));
		String fId = atts.getValue("facility");
		if (fId != null) {
			this.curract.setFacilityId(this.scenario.createId(fId));
		}
		if (this.routeDescription != null) {
			Id startLinkId = null;
			if (this.prevAct.getLinkId() != null) {
				startLinkId = this.prevAct.getLinkId();
			}
			Id endLinkId = null;
			if (this.curract.getLinkId() != null) {
				endLinkId = this.curract.getLinkId();
			}
			if (this.currRoute instanceof GenericRoute) {
				((GenericRoute) this.currRoute).setRouteDescription(startLinkId, this.routeDescription.trim(), endLinkId);
			} else if (this.currRoute instanceof NetworkRoute) {
				((NetworkRoute) this.currRoute).setLinkIds(startLinkId, NetworkUtils.getLinkIds(RouteUtils.getLinksFromNodes(NetworkUtils.getNodes(this.network, this.routeDescription))), endLinkId);
			} else {
				throw new RuntimeException("unknown route type: " + this.currRoute.getClass().getName());
			}
			this.routeDescription = null;
			this.currRoute = null;
		}
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
		this.currRoute = ((PopulationFactoryImpl) this.plans.getFactory()).createRoute(this.currleg.getMode(), null, null);
		this.currleg.setRoute(this.currRoute);
		if (atts.getValue("dist") != null) {
			this.currRoute.setDistance(Double.parseDouble(atts.getValue("dist")));
		}
		if (atts.getValue("trav_time") != null) {
			this.currRoute.setTravelTime(Time.parseTime(atts.getValue("trav_time")));
		}
	}


}

