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

package org.matsim.pt.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.MinimalTransferTimes;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.xml.sax.SAXException;

/**
 * An abstract class offering a number of static methods to validate several aspects of transit schedules.
 *
 * @author mrieser
 */
public abstract class TransitScheduleValidator {

	private TransitScheduleValidator() {
		// this class should not be instantiated
	}

	/**
	 * Checks that the links specified for a network route really builds a complete route that can be driven along.
	 *
	 * @param schedule
	 * @param network
	 * @return
	 */
	public static ValidationResult validateNetworkRoutes(final TransitSchedule schedule, final Network network) {
		ValidationResult result = new ValidationResult();
		if (network == null || network.getLinks().size() == 0) {
			result.addWarning("Cannot validate network routes: No network given!");
			return result;
		}

		for (TransitLine line : schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				NetworkRoute netRoute = route.getRoute();
				if (netRoute == null) {
					result.addIssue(new ValidationResult.ValidationIssue(ValidationResult.Severity.ERROR, "Transit line " + line.getId() + ", route " + route.getId() + " has no network route.", ValidationResult.Type.OTHER, Collections.singleton(route.getId())));
				} else {
					Link prevLink = network.getLinks().get(netRoute.getStartLinkId());
					for (Id<Link> linkId : netRoute.getLinkIds()) {
						Link link = network.getLinks().get(linkId);
						if (link == null) {
							result.addIssue(new ValidationResult.ValidationIssue(ValidationResult.Severity.ERROR, "Transit line " + line.getId() + ", route " + route.getId() +
									" contains a link that is not part of the network: " + linkId, ValidationResult.Type.OTHER, Collections.singleton(route.getId())));
						} else if (prevLink != null && !prevLink.getToNode().equals(link.getFromNode())) {
							result.addIssue(new ValidationResult.ValidationIssue(ValidationResult.Severity.ERROR, "Transit line " + line.getId() + ", route " + route.getId() +
									" has inconsistent network route, e.g. between link " + prevLink.getId() + " and " + linkId, ValidationResult.Type.OTHER, Collections.singleton(route.getId())));
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
		if (network == null || network.getLinks().size() == 0) {
			result.addWarning("Cannot validate stops on network route: No network given!");
			return result;
		}

		for (TransitLine line : schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				NetworkRoute netRoute = route.getRoute();
				if (netRoute == null) {
					result.addIssue(new ValidationResult.ValidationIssue(ValidationResult.Severity.ERROR, "Transit line " + line.getId() + ", route " + route.getId() + " has no network route.", ValidationResult.Type.OTHER, Collections.singleton(route.getId())));
				} else {
					List<Id<Link>> linkIds = new ArrayList<>();
					linkIds.add(netRoute.getStartLinkId());
					linkIds.addAll(netRoute.getLinkIds());
					linkIds.add(netRoute.getEndLinkId());
					Iterator<Id<Link>> linkIdIterator = linkIds.iterator();
					Id<Link> nextLinkId = linkIdIterator.next();
					boolean error = false;
					for (TransitRouteStop stop : route.getStops()) {
						Id<Link> linkRefId = stop.getStopFacility().getLinkId();

						while (!linkRefId.equals(nextLinkId)) {
							if (linkIdIterator.hasNext()) {
								nextLinkId = linkIdIterator.next();
							} else {
								result.addIssue(new ValidationResult.ValidationIssue(ValidationResult.Severity.ERROR, "Transit line " + line.getId() + ", route " + route.getId() + ": Stop " + stop.getStopFacility().getId() + " cannot be reached along network route.", ValidationResult.Type.ROUTE_HAS_UNREACHABLE_STOP, Collections.singletonList(stop.getStopFacility().getId())));
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
					Id<Link> linkId = stop.getStopFacility().getLinkId();
					if (linkId == null) {
						result.addIssue(new ValidationResult.ValidationIssue(ValidationResult.Severity.ERROR,"Transit Stop Facility " + stop.getStopFacility().getId() + " has no linkId, but is used by transit line " + line.getId() + ", route " + route.getId(), ValidationResult.Type.HAS_NO_LINK_REF, Collections.singleton(stop.getStopFacility().getId())));
					}
				}
			}
		}
		return result;
	}

	public static ValidationResult validateAllStopsExist(final TransitSchedule schedule) {
		ValidationResult result = new ValidationResult();
		for (TransitLine line : schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (TransitRouteStop stop : route.getStops()) {
					if (stop.getStopFacility() == null) {
						result.addIssue(new ValidationResult.ValidationIssue(ValidationResult.Severity.ERROR, "Transit line " + line.getId() + ", route " + route.getId() + " contains a stop (dep-offset=" + stop.getDepartureOffset() + ") without stop-facility. Most likely, a wrong id was specified in the file.", ValidationResult.Type.HAS_MISSING_STOP_FACILITY, Collections.singletonList(route.getId())));
					} else if (schedule.getFacilities().get(stop.getStopFacility().getId()) == null) {
						result.addIssue(new ValidationResult.ValidationIssue(ValidationResult.Severity.ERROR, "Transit line " + line.getId() + ", route " + route.getId() + " contains a stop (stop-facility " + stop.getStopFacility().getId() + ") that is not contained in the list of all stop facilities.", ValidationResult.Type.HAS_MISSING_STOP_FACILITY, Collections.singletonList(route.getId())));
					}
				}
			}
		}
		return result;
	}

	public static ValidationResult validateOffsets(final TransitSchedule schedule) {
		ValidationResult result = new ValidationResult();

		for (TransitLine line : schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				ArrayList<TransitRouteStop> stops = new ArrayList<TransitRouteStop>(route.getStops());
				int stopCount = stops.size();

				if (stopCount > 0) {
					TransitRouteStop stop = stops.get(0);
					if (stop.getDepartureOffset().isUndefined()) {
						result.addError("Transit line " + line.getId() + ", route " + route.getId() + ": The first stop does not contain any departure offset.");
					}

					for (int i = 1; i < stopCount - 1; i++) {
						stop = stops.get(i);
						if (stop.getDepartureOffset().isUndefined()) {
							result.addError("Transit line " + line.getId() + ", route " + route.getId() + ": Stop " + i + " does not contain any departure offset.");
						}
					}

					stop = stops.get(stopCount - 1);
					if (stop.getArrivalOffset().isUndefined()) {
						result.addError("Transit line " + line.getId() + ", route " + route.getId() + ": The last stop does not contain any arrival offset.");
					}
				} else {
					result.addWarning("Transit line " + line.getId() + ", route " + route.getId() + ": The route has not stops assigned, looks suspicious.");
				}

			}
		}

		return result;
	}

	public static ValidationResult validateTransfers(final TransitSchedule schedule) {
		ValidationResult result = new ValidationResult();

		MinimalTransferTimes transferTimes = schedule.getMinimalTransferTimes();
		MinimalTransferTimes.MinimalTransferTimesIterator iter = transferTimes.iterator();
		Set<Id> missingFromStops = new HashSet<>();
		Set<Id> missingToStops = new HashSet<>();

		while (iter.hasNext()) {
			iter.next();
			Id<TransitStopFacility> fromStopId = iter.getFromStopId();
			Id<TransitStopFacility> toStopId = iter.getToStopId();
			double transferTime = iter.getSeconds();

			if (fromStopId == null && toStopId == null) {
				result.addError("Minimal Transfer Times: both fromStop and toStop are null.");
			} else if (fromStopId == null) {
				result.addError("Minimal Transfer Times: fromStop = null, toStop " + toStopId + ".");
			} else if (toStopId == null) {
				result.addError("Minimal Transfer Times: fromStop " + fromStopId + ", toStop = null.");
			}
			if (transferTime <= 0) {
				result.addWarning("Minimal Transfer Times: fromStop " + fromStopId + " toStop " + toStopId + " with transferTime = " + transferTime);
			}
			if (schedule.getFacilities().get(fromStopId) == null && missingFromStops.add(fromStopId)) {
				result.addError("Minimal Transfer Times: fromStop " + fromStopId + " does not exist in schedule.");
			}
			if (schedule.getFacilities().get(toStopId) == null && missingToStops.add(toStopId)) {
				result.addError("Minimal Transfer Times: toStop " + toStopId + " does not exist in schedule.");
			}
		}

		return result;
	}

	public static ValidationResult validateAll(final TransitSchedule schedule, final Network network) {
		ValidationResult v = validateUsedStopsHaveLinkId(schedule);
		v.add(validateNetworkRoutes(schedule, network));
		try {
			v.add(validateStopsOnNetworkRoute(schedule, network));
		} catch (NullPointerException e) {
			v.addError("Exception during 'validateStopsOnNetworkRoute'. Most likely something is wrong in the file, but it cannot be specified in more detail." + Arrays.toString(e.getStackTrace()));
		}
		v.add(validateAllStopsExist(schedule));
		v.add(validateOffsets(schedule));
		v.add(validateTransfers(schedule));
		return v;
	}

	public static void printResult(final ValidationResult result) {
		if (result.isValid()) {
			System.out.println("Schedule appears valid!");
		} else {
			System.out.println("Schedule is NOT valid!");
		}
		if (result.getErrors().size() > 0) {
			System.out.println("Validation errors:");
			for (String e : result.getErrors()) {
				System.out.println(e);
			}
		}
		if (result.getWarnings().size() > 0) {
			System.out.println("Validation warnings:");
			for (String w : result.getWarnings()) {
				System.out.println(w);
			}
		}
	}

	/**
	 * @param args [0] path to transitSchedule.xml, [1] path to network.xml (optional)
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {
		if (args.length > 2 || args.length < 1) {
			System.err.println("Usage: TransitScheduleValidator transitSchedule.xml [network.xml]");
			return;
		}

		MutableScenario s = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		s.getConfig().transit().setUseTransit(true);
		TransitSchedule ts = s.getTransitSchedule();
		Network net = s.getNetwork();

		if (args.length > 1) {
			new MatsimNetworkReader(s.getNetwork()).readFile(args[1]);
		}
		new TransitScheduleReader(s).readFile(args[0]);

		ValidationResult v = validateAll(ts, net);
		printResult(v);
	}

	public static class ValidationResult {

		public enum Severity {
			WARNING, ERROR
		}

		public enum Type {
			HAS_MISSING_STOP_FACILITY, HAS_NO_LINK_REF, ROUTE_HAS_UNREACHABLE_STOP, OTHER
		}

		public static class ValidationIssue<T> {
			private final Severity severity;
			private final String message;
			private final Type errorCode;
			private final Collection<Id<T>> entities;

			public ValidationIssue(Severity severity, String message, Type errorCode, Collection<Id<T>> entities) {
				this.severity = severity;
				this.message = message;
				this.errorCode = errorCode;
				this.entities = entities;
			}

			public Severity getSeverity() {
				return severity;
			}

			public String getMessage() {
				return message;
			}

			public Type getErrorCode() {
				return errorCode;
			}

			public Collection<Id<T>> getEntities() {
				return entities;
			}

		}

		private boolean isValid = true;
		private final List<ValidationIssue> issues = new ArrayList<>();

		public boolean isValid() {
			return this.isValid;
		}

		public List<String> getWarnings() {
			List<String> result = new ArrayList<>();
			for (ValidationIssue issue : this.issues) {
				if (issue.severity == Severity.WARNING) {
					result.add(issue.getMessage());
				}
			}
			return Collections.unmodifiableList(result);
		}

		public List<String> getErrors() {
			List<String> result = new ArrayList<>();
			for (ValidationIssue issue : this.issues) {
				if (issue.severity == Severity.ERROR) {
					result.add(issue.getMessage());
				}
			}
			return Collections.unmodifiableList(result);
		}

		public List<ValidationIssue> getIssues() {
			return Collections.unmodifiableList(this.issues);
		}

		public void addWarning(final String warning) {
			this.issues.add(new ValidationIssue(Severity.WARNING, warning, Type.OTHER, Collections.<Id<?>>emptyList()));
		}

		public void addError(final String error) {
			this.issues.add(new ValidationIssue(Severity.ERROR, error, Type.OTHER, Collections.<Id<?>>emptyList()));
			this.isValid = false;
		}

		public void addIssue(final ValidationIssue issue) {
			this.issues.add(issue);
			if (issue.severity == Severity.ERROR) {
				this.isValid = false;
			}
		}

		public void add(final ValidationResult otherResult) {
			this.issues.addAll(otherResult.getIssues());
			this.isValid = this.isValid && otherResult.isValid;
		}
	}
}
