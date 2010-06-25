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

package playground.mzilske.neo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.routes.GenericRoute;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.core.utils.misc.RouteUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.population.Desires;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * A reader for plans files of MATSim according to <code>plans_v4.dtd</code>.
 *
 * @author mrieser
 * @author balmermi
 */
public class FlushingNeoPopulationReader extends MatsimXmlParser implements PopulationReader {

	int nPersons = 0;
	
	private final static String PLANS = "plans";
	private final static String PERSON = "person";
	private final static String TRAVELCARD = "travelcard";
	private final static String DESIRES = "desires";
	private final static String ACTDUR = "actDur";
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

	private Person currperson = null;
	private Desires currdesires = null;
	private String curracttype = null;
	private ActivityOption curractivity = null;
	private Plan currplan = null;
	private Activity curract = null;
	private Leg currleg = null;
	private Route currRoute = null;
	private String routeDescription = null;

	private Activity prevAct = null;

	private final static Logger log = Logger.getLogger(FlushingNeoPopulationReader.class);

	public FlushingNeoPopulationReader(final Scenario scenario) {
		this.scenario = scenario;
		this.plans = scenario.getPopulation();
		this.network = scenario.getNetwork();
		if (scenario instanceof ScenarioImpl) {
			this.facilities = ((ScenarioImpl) scenario).getActivityFacilities();
		} else {
			this.facilities = null;
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
		} else if (ACTDUR.equals(name)) {
			startActDur(atts);
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
				((NeoScenario) scenario).success();
				((NeoScenario) scenario).finish();
				nPersons++;
				if (nPersons % 1000 == 0) {
					System.out.println(nPersons);
				}
			}
			this.currperson = null;
		} else if (DESIRES.equals(name)) {
			this.currdesires = null;
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
	 * Parses the specified plans file. This method calls {@link #parse(String)}, but handles all
	 * possible exceptions on its own.
	 *
	 * @param filename The name of the file to parse.
	 */
	public void readFile(final String filename) {
		try {
			parse(filename);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void startPlans(final Attributes atts) {
		this.plans.setName(atts.getValue("name"));
		if (atts.getValue("reference_layer") != null) {
			log.warn("plans.reference_layer is no longer supported.");
		}
	}

	private void startPerson(final Attributes atts) {
		((NeoScenario) scenario).beginTx();
		this.currperson = this.scenario.getPopulation().getFactory().createPerson(this.scenario.createId(atts.getValue("id")));
	}

	private void startTravelcard(final Attributes atts) {
		
	}

	private void startActDur(final Attributes atts) {
		this.currdesires.putActivityDuration(atts.getValue(ATTR_TYPE),atts.getValue("dur"));
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

		if (type != null) { log.info("Attribute type in <location> is deprecated!"); }
		if (id == null) { Gbl.errorMsg("NEW: location must have an id!"); }
		if ((x != null) || (y != null)) { log.info("NEW: coords in <location> will be ignored!"); }
		if (freq != null) { log.info("NEW: Attribute freq in <location> is not supported at the moment!"); }

		ActivityFacility currfacility = this.facilities.getFacilities().get(this.scenario.createId(id));
		if (currfacility == null) { Gbl.errorMsg("facility id=" + id + " does not exist!"); }
		this.curractivity = currfacility.getActivityOptions().get(this.curracttype);
		if (this.curractivity == null) { Gbl.errorMsg("facility id=" + id + ": Activity of type=" + this.curracttype + " does not exist!"); }
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
		this.currplan = this.scenario.getPopulation().getFactory().createPlan();
		this.currperson.addPlan(this.currplan);
		this.currplan.setSelected(selected);
		
		String scoreString = atts.getValue("score");
		if (scoreString != null) {
			double score = Double.parseDouble(scoreString);
			this.currplan.setScore(score);
		}

	}

	private void startAct(final Attributes atts) {
		Coord coord = null;
		if (atts.getValue("link") != null) {
			Id linkId = this.scenario.createId(atts.getValue("link"));
			this.curract = this.scenario.getPopulation().getFactory().createActivityFromLinkId(atts.getValue(ATTR_TYPE), linkId);
			this.currplan.addActivity(this.curract);
		} else if ((atts.getValue("x") != null) && (atts.getValue("y") != null)) {
			coord = new CoordImpl(atts.getValue("x"), atts.getValue("y"));
			this.curract = this.scenario.getPopulation().getFactory().createActivityFromCoord(atts.getValue(ATTR_TYPE), coord);
			this.currplan.addActivity(curract);
		} else {
			throw new IllegalArgumentException("In this version of MATSim either the coords or the link must be specified for an Act.");
		}
		this.curract.setStartTime(Time.parseTime(atts.getValue("start_time")));
		this.curract.setEndTime(Time.parseTime(atts.getValue("end_time")));
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
		this.currleg = this.scenario.getPopulation().getFactory().createLeg(TransportMode.valueOf(mode));
		this.currplan.addLeg(this.currleg);
		this.currleg.setDepartureTime(Time.parseTime(atts.getValue("dep_time")));
		this.currleg.setTravelTime(Time.parseTime(atts.getValue("trav_time")));
	}

	private void startRoute(final Attributes atts) {
		this.currRoute = ((NetworkFactoryImpl) this.network.getFactory()).createRoute(this.currleg.getMode(), null, null);
		this.currleg.setRoute(this.currRoute);
		if (atts.getValue("dist") != null) {
			this.currRoute.setDistance(Double.parseDouble(atts.getValue("dist")));
		}
		if (atts.getValue("trav_time") != null) {
			this.currRoute.setTravelTime(Time.parseTime(atts.getValue("trav_time")));
		}
	}

}
