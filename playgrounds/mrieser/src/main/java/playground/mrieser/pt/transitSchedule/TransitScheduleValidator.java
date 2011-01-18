/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.mrieser.pt.transitSchedule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.xml.sax.SAXException;

/**
 * @author mrieser
 */
public abstract class TransitScheduleValidator {

	/**
	 * Checks that the links specified for a network route really builds a complete route that can be driven along.
	 *
	 * @param schedule
	 * @param network
	 * @return
	 */
	public static ValidationResult validateNetworkRoutes(final TransitSchedule schedule, final Network network) {
		ValidationResult result = new ValidationResult();

		for (TransitLine line : schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				NetworkRoute netRoute = route.getRoute();
				if (netRoute == null) {
					result.addError("Transit line " + line.getId() + ", route " + route.getId() + " has no network route.");
				} else {
					Link prevLink = network.getLinks().get(netRoute.getStartLinkId());
					for (Id linkId : netRoute.getLinkIds()) {
						Link link = network.getLinks().get(linkId);
						if (!prevLink.getToNode().equals(link.getFromNode())) {
							result.addError("Transit line " + line.getId() + ", route " + route.getId() +
									" has inconsistent network route, e.g. between link " + prevLink.getId() + " and " + linkId);
						}
						prevLink = link;
					}
				}
			}
		}
		return result;
	}

	/**
	 * Checks that all the listed stops in a route appear in that order when driving along the network route
	 *
	 * @param schedule
	 * @param network
	 * @return
	 */
	public static ValidationResult validateStopsOnNetworkRoute(final TransitSchedule schedule, final Network network) {
		ValidationResult result = new ValidationResult();

		for (TransitLine line : schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				NetworkRoute netRoute = route.getRoute();
				if (netRoute == null) {
					result.addError("Transit line " + line.getId() + ", route " + route.getId() + " has no network route.");
				} else {
					List<Id> linkIds = new ArrayList<Id>();
					linkIds.add(netRoute.getStartLinkId());
					linkIds.addAll(netRoute.getLinkIds());
					linkIds.add(netRoute.getEndLinkId());
					Iterator<Id> linkIdIterator = linkIds.iterator();
					Id nextLinkId = linkIdIterator.next();
					boolean error = false;
					for (TransitRouteStop stop : route.getStops()) {
						Id linkRefId = stop.getStopFacility().getLinkId();

						while (!linkRefId.equals(nextLinkId)) {
							if (linkIdIterator.hasNext()) {
								nextLinkId = linkIdIterator.next();
							} else {
								result.addError("Transit line " + line.getId() + ", route " + route.getId() + ": Stop " + stop.getStopFacility().getId() + " cannot be reached along network route.");
								error = true;
								break;
							}
						}
						if (error) {
							break;
						}

					}
				}
			}
		}
		return result;
	}

	public static ValidationResult validateUsedStopsHaveLinkId(final TransitSchedule schedule) {
		ValidationResult result = new ValidationResult();
		for (TransitLine line : schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (TransitRouteStop stop : route.getStops()) {
					Id linkId = stop.getStopFacility().getLinkId();
					if (linkId == null) {
						result.addError("Transit Stop Facility " + stop.getStopFacility().getId() + " has no linkId, but is used by transit line " + line.getId() + ", route " + route.getId());
					}
				}
			}
		}
		return result;
	}

	public static ValidationResult validateAll(final TransitSchedule schedule, final Network network) {
		ValidationResult v = validateUsedStopsHaveLinkId(schedule);
		ValidationResult v2 = validateNetworkRoutes(schedule, network);
		v.add(v2);
		try {
			ValidationResult v3 = validateStopsOnNetworkRoute(schedule, network);
			v.add(v3);
		} catch (NullPointerException e) {
			v.addError("Exception during 'validateStopsOnNetworkRoute'. Most likely something is wrong in the file, but it cannot be specified in more detail." + Arrays.toString(e.getStackTrace())); // TODO [MR] improve
		}
		return v;
	}

	/**
	 * @param args [0] path to transitSchedule.xml, [1] path to network.xml
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {
		ScenarioImpl s = new ScenarioImpl();
		s.getConfig().scenario().setUseTransit(true);
		TransitSchedule ts = s.getTransitSchedule();
		Network net = s.getNetwork();

//		args = new String[] {"/data/projects/bvg2010/Daten/transitSchedule.oevnet.xml", "/data/projects/bvg2010/Daten/transit-network.xml"};
//		args = new String[] {"/Users/cello/Desktop/MATSim/pt-test/matsim-0.2.0-rc1-release/pt-rdam/transitSchedule.xml", "/Users/cello/Desktop/MATSim/pt-test/matsim-0.2.0-rc1-release/pt-rdam/rdamnetwork.xml"};
//		args = new String[] {"/data/senozon/visumData/matsim/transitSchedule.xml", "/data/senozon/visumData/matsim/network.cleaned.xml"};
		args = new String[] {"/Users/cello/Desktop/bvgnet8/transitSchedule.xml", "/Users/cello/Desktop/bvgnet8/network.xml"};

		new MatsimNetworkReader(s).parse(args[1]);
		new TransitScheduleReader(s).readFile(args[0]);

		ValidationResult v = validateAll(ts, net);
		if (v.isValid()) {
			System.out.println("Schedule appears valid!");
		} else {
			System.out.println("Schedule is NOT valid!");
		}
		if (v.getErrors().size() > 0) {
			System.out.println("Validation errors:");
			for (String e : v.getErrors()) {
				System.out.println(e);
			}
		}
		if (v.getWarnings().size() > 0) {
			System.out.println("Validation warnings:");
			for (String w : v.getWarnings()) {
				System.out.println(w);
			}
		}
	}

	public static class ValidationResult {
		private boolean isValid = true;
		private List<String> warnings = new ArrayList<String>();
		private List<String> errors = new ArrayList<String>();

		public boolean isValid() {
			return this.isValid;
		}

		public List<String> getWarnings() {
			return Collections.unmodifiableList(this.warnings);
		}

		public List<String> getErrors() {
			return Collections.unmodifiableList(this.errors);
		}

		/*package*/ void addWarning(final String warning) {
			this.warnings.add(warning);
		}

		/*package*/ void addError(final String error) {
			this.errors.add(error);
			this.isValid = false;
		}

		/*package*/ void add(final ValidationResult otherResult) {
			this.warnings.addAll(otherResult.warnings);
			this.errors.addAll(otherResult.errors);
			this.isValid = this.isValid && otherResult.isValid;
		}
	}
}
