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
import java.util.*;

import javax.xml.parsers.ParserConfigurationException;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.GeometryUtils;
import org.matsim.pt.transitSchedule.api.*;
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

	public static ValidationResult validateDepartures(TransitSchedule schedule) {
		ValidationResult result = new ValidationResult();
		for (TransitLine line : schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				if (route.getDepartures().isEmpty())
					result.addError("No departures defined for line %s, route %s".formatted(line.getId(), route.getId()));

			}
		}

		return result;
	}

	/**
	 * Validate if coordinates of stops and given travel times are plausible.
	 */
	public static ValidationResult validateStopCoordinates(final TransitSchedule schedule) {

		ValidationResult result = new ValidationResult();

		// List of stops to the collected suspicious stops
		Map<TransitStopFacility, DoubleList> suspiciousStops = new TreeMap<>(Comparator.comparing(TransitStopFacility::getName));

		for (TransitLine line : schedule.getTransitLines().values()) {

			for (TransitRoute route : line.getRoutes().values()) {

				List<TransitRouteStop> routeStops = route.getStops();

				// For too short routes, we can not detect outliers
				if (routeStops.size() <= 4)
					continue;

				double lastDepartureOffset = routeStops.getFirst().getDepartureOffset().or(routeStops.getFirst().getArrivalOffset()).seconds();

				DoubleList speeds = new DoubleArrayList();
				DoubleList dists = new DoubleArrayList();

				for (int i = 1; i < routeStops.size(); i++) {
					TransitRouteStop routeStop = routeStops.get(i);

					if (routeStop.getStopFacility().getCoord() == null)
						break;

					double departureOffset = routeStop.getArrivalOffset().or(routeStop.getDepartureOffset()).orElse(0);
					double travelTime = departureOffset - lastDepartureOffset;
					double length = CoordUtils.calcEuclideanDistance(routeStop.getStopFacility().getCoord(),
						routeStops.get(i - 1).getStopFacility().getCoord());

					dists.add(length);

					// Short distances are not checked, because here high speeds are not so problematic and arise from few seconds difference
					if (length <= 20) {
						speeds.add(-1);
						continue;
					}

					if (travelTime == 0) {
						speeds.add(Double.POSITIVE_INFINITY);
						continue;
					}

					double speed = length / travelTime;
					speeds.add(speed);
					lastDepartureOffset = departureOffset;
				}

				// If all speeds are valid, the stops and speeds can be checked
				if (speeds.size() == routeStops.size() - 1) {

					// First check for suspicious stops
					// These are stops with very high speed, and also high distance between stops
					for (int i = 0; i < speeds.size() - 1; i++) {
						TransitRouteStop stop = routeStops.get(i + 1);
						double toStop = speeds.getDouble(i);
						double fromStop = speeds.getDouble(i + 1);

						double both = (toStop + fromStop) / 2;

						double dist = (dists.getDouble(i) + dists.getDouble(i + 1)) / 2;

						// Only if the distance is large, we assume a mapping error might have occurred
						if (dist < 5_000)
							continue;

						// Remove the considered speeds from the calculation
						DoubleList copy = new DoubleArrayList(speeds);
						copy.removeDouble(i);
						copy.removeDouble(i);
						copy.removeIf(s -> s == -1 || s == Double.POSITIVE_INFINITY);

						double mean = copy.doubleStream().average().orElse(-1);

						// If no mean is known, use a high value to avoid false positives
						if (mean == -1) {
							mean = 70;
						}

						// Some hard coded rules to detect suspicious stops, these are speed m/s, so quite high values
						if (((toStop > 3 * mean && both > 50) || toStop > 120) && (((fromStop > 3 * mean && both > 50) || fromStop > 120))) {
							DoubleList suspiciousSpeeds = suspiciousStops.computeIfAbsent(stop.getStopFacility(), (k) -> new DoubleArrayList());
							suspiciousSpeeds.add(toStop);
							suspiciousSpeeds.add(fromStop);
						}
					}

					// Then check for implausible travel times
					for (int i = 0; i < speeds.size(); i++) {
						double speed = speeds.getDouble(i);
						TransitStopFacility from = routeStops.get(i).getStopFacility();
						TransitStopFacility to = routeStops.get(i + 1).getStopFacility();
						if (speed > 230) {
							result.addWarning("Suspicious high speed from stop %s (%s) to %s (%s) on line %s, route %s, index: %d: %.2f m/s, %.2fm"
								.formatted(from.getName(), from.getId(), to.getName(), to.getId(), line.getId(), route.getId(), i, speed, dists.getDouble(i)));
						}
					}
				}
			}
		}

		for (Map.Entry<TransitStopFacility, DoubleList> e : suspiciousStops.entrySet()) {
			TransitStopFacility stop = e.getKey();
			double speed = e.getValue().doubleStream().average().orElse(-1);
			result.addWarning("Suspicious location for stop %s (%s) at stop area %s: %s, avg. speed: %.2f m/s".formatted(stop.getName(), stop.getId(), stop.getStopAreaId(), stop.getCoord(), speed));
		}

		return result;
	}

	/**
	 * Validate that all chained departures reference existing departures in the schedule.
	 */
	public static ValidationResult validateChainedDepartures(final TransitSchedule schedule) {
		ValidationResult result = new ValidationResult();
		
		// Track all available departures in the schedule
		Map<Id<TransitLine>, TransitLine> allLines = schedule.getTransitLines();
		
		for (TransitLine line : allLines.values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (Departure departure : route.getDepartures().values()) {
					if (!departure.getChainedDepartures().isEmpty()) {
						for (ChainedDeparture chainedDep : departure.getChainedDepartures()) {
							Id<TransitLine> transitLineId = chainedDep.getChainedTransitLineId();
							Id<TransitRoute> transitRouteId = chainedDep.getChainedRouteId();
							Id<Departure> departureId = chainedDep.getChainedDepartureId();
							
							// Check if the referenced transit line exists
							TransitLine targetLine = allLines.get(transitLineId);
							if (targetLine == null) {
								result.addIssue(new ValidationResult.ValidationIssue(ValidationResult.Severity.ERROR, 
									"Transit line " + line.getId() + ", route " + route.getId() + 
									", departure " + departure.getId() + " has a chained departure reference to line " + 
									transitLineId + " which does not exist in the schedule.", 
									ValidationResult.Type.OTHER, Collections.singleton(departure.getId())));
								continue;
							}
							
							// Check if the referenced transit route exists
							TransitRoute targetRoute = targetLine.getRoutes().get(transitRouteId);
							if (targetRoute == null) {
								result.addIssue(new ValidationResult.ValidationIssue(ValidationResult.Severity.ERROR, 
									"Transit line " + line.getId() + ", route " + route.getId() + 
									", departure " + departure.getId() + " has a chained departure reference to route " + 
									transitRouteId + " in line " + transitLineId +
									" which does not exist in the schedule.", 
									ValidationResult.Type.OTHER, Collections.singleton(departure.getId())));
								continue;
							}
							
							// Check if the referenced departure exists
							if (!targetRoute.getDepartures().containsKey(departureId)) {
								result.addIssue(new ValidationResult.ValidationIssue(ValidationResult.Severity.ERROR, 
									"Transit line " + line.getId() + ", route " + route.getId() + 
									", departure " + departure.getId() + " has a chained departure reference to departure " + 
									departureId + " in route " + targetRoute.getId() + ", line " + transitLineId +
									" which does not exist in the schedule.", 
									ValidationResult.Type.OTHER, Collections.singleton(departure.getId())));
								continue;
							}

							// Check if the last stop of the route matches the first stop of the chained route
							if (!route.getStops().isEmpty() && !targetRoute.getStops().isEmpty()) {
								TransitRouteStop lastStop = route.getStops().get(route.getStops().size() - 1);
								TransitRouteStop firstStop = targetRoute.getStops().get(0);
								
								if (!lastStop.getStopFacility().getId().equals(firstStop.getStopFacility().getId())) {
									result.addIssue(new ValidationResult.ValidationIssue(ValidationResult.Severity.ERROR,
										"Transit line " + line.getId() + ", route " + route.getId() +
										", departure " + departure.getId() + " has a chained departure where the last stop (" +
										lastStop.getStopFacility().getId() + ") does not match the first stop (" +
										firstStop.getStopFacility().getId() + ") of the chained route " + targetRoute.getId() +
										" in line " + transitLineId,
										ValidationResult.Type.OTHER, Collections.singleton(departure.getId())));
								}
							}
						}
					}
				}
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
		v.add(validateStopCoordinates(schedule));
		v.add(validateDepartures(schedule));
		v.add(validateChainedDepartures(schedule));
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
