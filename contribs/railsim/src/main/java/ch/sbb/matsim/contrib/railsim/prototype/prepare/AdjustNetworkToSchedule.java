package ch.sbb.matsim.contrib.railsim.prototype.prepare;

import ch.sbb.matsim.contrib.railsim.prototype.RailsimConfigGroup;
import ch.sbb.matsim.contrib.railsim.prototype.RailsimUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Ihab Kaddoura
 */
public class AdjustNetworkToSchedule {
	private static final Logger log = LogManager.getLogger(AdjustNetworkToSchedule.class);

	private final Scenario scenario;
	private final Set<Id<Link>> stopLinkIds = new HashSet<>();
	private final RailsimConfigGroup railsimConfigGroup;

	private boolean throwExceptionIfMaximumVehicleVelocityIsViolated = false;

	/**
	 * @param scenario
	 */
	public AdjustNetworkToSchedule(Scenario scenario) {
		this.scenario = scenario;
		this.railsimConfigGroup = ConfigUtils.addOrGetModule(scenario.getConfig(), RailsimConfigGroup.class);
	}

	public void run() {

		log.info("Adjust network speed levels to the transit schedule...");

		// first store the information which link is a stop link
		for (TransitStopFacility facility : scenario.getTransitSchedule().getFacilities().values()) {
			stopLinkIds.add(facility.getLinkId());
		}

		// now adjust the travel times for each transit line, transit route, stop to stop segment...
		for (TransitLine line : scenario.getTransitSchedule().getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {

				log.debug("+++++++ Transit route: " + route.getId().toString() + " ++++++++");

				// identify vehicle types
				VehicleType vehicleType = null;
				for (Departure departure : route.getDepartures().values()) {
					Vehicle vehicle = this.scenario.getTransitVehicles().getVehicles().get(departure.getVehicleId());
					if (vehicleType == null) {
						vehicleType = vehicle.getType();
					} else {
						if (vehicleType == vehicle.getType()) {
							// same vehicle type
						} else {
							throw new RuntimeException("Different vehicle types detected within a single transit route: " + route.getId());
						}
					}
				}

				Double previousDeparture = null;
				Id<Link> previousStopLink = null;
				for (TransitRouteStop stop : route.getStops()) {
					if (previousDeparture == null) {
						previousDeparture = stop.getDepartureOffset().seconds();
						previousStopLink = stop.getStopFacility().getLinkId();

					} else {
						double ttSchedule = stop.getArrivalOffset().seconds() - previousDeparture;

						if (ttSchedule <= 0) {
							log.warn("Implausible travel time in schedule: " + ttSchedule + " / arrival: " + stop.getArrivalOffset().seconds() + " / previous departure: " + previousDeparture);
							log.warn("Line: " + line.getId() + " Route: " + route.getId() + " Stop: " + stop.getStopFacility().getId());

							if (ttSchedule < 0) throw new RuntimeException("Invalid travel time. Aborting...");
							// TODO: do not throw a runtime exception in the case of ttSchedule == 0; otherwise a test fails...
						}

						List<Id<Link>> routeLinks = getRouteLinks(route, previousStopLink, stop.getStopFacility().getLinkId());
						adjustLinks(route.getId(), line.getId(), routeLinks, ttSchedule, vehicleType);
						previousStopLink = stop.getStopFacility().getLinkId();

						if (stop.getDepartureOffset().isDefined()) {
							previousDeparture = stop.getDepartureOffset().seconds();
						} else {
							previousDeparture = Double.MAX_VALUE;
						}
					}
				}
			}
		}

		new NetworkWriter(scenario.getNetwork()).write(scenario.getConfig().controler().getOutputDirectory() + "../modified_inputTrainNetwork.xml");
	}

	private void adjustLinks(Id<TransitRoute> routeId, Id<TransitLine> lineId, List<Id<Link>> routeLinks, double ttSchedule, VehicleType vehicleType) {
		log.info("++++++++++++++++++");
		log.info("Line: " + lineId);
		log.info("Route: " + routeId);
		log.info("Vehicle type: " + vehicleType.getId());
		log.info("Route links: " + routeLinks.toString());

		final double ttNetwork = computeRouteTravelTime(routeLinks, lineId, routeId, vehicleType.getId());

		if (isSame(ttNetwork, ttSchedule)) {
			// no delays and no early arrivals, nothing to do.

		} else {

			// Start with the easiest implementation: set the speed equally for each link
			// In a later step and depending on the available data, we may come up with
			// a more elaborated approach.
			// If we already have several smaller links, we may account for the acceleration and deceleration
			// and apply a function, e.g. lower speeds behind/before stops and so on...
			double requiredSpeed = this.computeRouteTravelDistance(routeLinks) / ttSchedule;

			if (requiredSpeed > vehicleType.getMaximumVelocity()) {
				log.warn("The required speed is larger than the maximum velocity of the vehicle.");
				log.warn("ttNetwork: " + ttNetwork + " ttSchedule: " + ttSchedule);
				log.warn("Required speed is above vehicle maximum velocity.");
				log.warn("required speed: " + requiredSpeed);
				log.warn("vehicle maximum velocity: " + vehicleType.getMaximumVelocity());

				if (this.throwExceptionIfMaximumVehicleVelocityIsViolated) {
					throw new RuntimeException("Aborting...");
				} else {
					log.warn("Continuing anyway...");
				}
			}

			Network network = this.scenario.getNetwork();

			for (Id<Link> linkId : routeLinks) {
				Link link = network.getLinks().get(linkId);

				if (railsimConfigGroup.getTrainSpeedApproach() == RailsimConfigGroup.TrainSpeedApproach.fromLinkAttributesForEachLine) {
					if (link.getAttributes().getAttribute(lineId.toString()) == null) {
						link.getAttributes().putAttribute(lineId.toString(), requiredSpeed);
					} else {
						double previousValue = (double) link.getAttributes().getAttribute(lineId.toString());
						if (previousValue == requiredSpeed) {
							// ok
						} else {
							log.warn("link: " + linkId);
							log.warn("previous value: " + previousValue);
							log.warn("required speed: " + requiredSpeed);
							throw new RuntimeException("Link attribute already set for line " + lineId + ". Maybe try " + RailsimConfigGroup.TrainSpeedApproach.fromLinkAttributesForEachLineAndRoute + " instead! Aborting...");
						}
					}
				} else if (railsimConfigGroup.getTrainSpeedApproach() == RailsimConfigGroup.TrainSpeedApproach.fromLinkAttributesForEachLineAndRoute) {
					if (link.getAttributes().getAttribute(lineId.toString() + "+++" + routeId.toString()) == null) {
						link.getAttributes().putAttribute(lineId.toString() + "+++" + routeId.toString(), requiredSpeed);
					} else {
						double previousValue = (double) link.getAttributes().getAttribute(lineId.toString() + "+++" + routeId.toString());
						if (previousValue == requiredSpeed) {
							// ok
						} else {
							log.warn("previous value: " + previousValue);
							log.warn("required speed: " + requiredSpeed);
							throw new RuntimeException("Link attribute already set for line " + lineId + " and route " + routeId + ". Aborting...");
						}
					}
				} else if (railsimConfigGroup.getTrainSpeedApproach() == RailsimConfigGroup.TrainSpeedApproach.fromLinkAttributesForEachVehicleType) {
					if (link.getAttributes().getAttribute(vehicleType.getId().toString()) == null) {
						link.getAttributes().putAttribute(vehicleType.getId().toString(), requiredSpeed);
					} else {
						double previousValue = (double) link.getAttributes().getAttribute(vehicleType.getId().toString());
						if (previousValue == requiredSpeed) {
							// ok
						} else {
							log.warn("previous value: " + previousValue);
							log.warn("required speed: " + requiredSpeed);
							throw new RuntimeException("Link attribute already set for vehicleType " + vehicleType.getId() + ". Maybe try " + RailsimConfigGroup.TrainSpeedApproach.fromLinkAttributesForEachLine + " instead! Aborting...");
						}
					}
				} else {
					throw new RuntimeException("AdjustNetworkToSchedule does not work for trainSpeedApproach " + railsimConfigGroup.getTrainSpeedApproach().toString() + " Aborting...");
				}
			}

			// re-compute the network travel time...
			double ttNetworkAfterNetworkAdjustment = computeRouteTravelTime(routeLinks, lineId, routeId, vehicleType.getId());
			if (isSame(ttNetworkAfterNetworkAdjustment, ttSchedule)) {
				// network adjustment successful

			} else {
				log.warn("ttNetwork (before): " + ttNetwork + " / ttNetwork (after): " + ttNetworkAfterNetworkAdjustment + " / ttSchedule: " + ttSchedule);
				throw new RuntimeException("Network travel time is still different from schedule travel time. Aborting...");
			}
		}
	}

	/**
	 * @param ttNetwork
	 * @param ttSchedule
	 * @return
	 */
	private boolean isSame(double ttNetwork, double ttSchedule) {
		return Math.abs(ttNetwork - ttSchedule) <= 1.;
	}

	/**
	 * @param route
	 * @param fromStopLinkId
	 * @param toStopLinkId
	 * @return a snippet of the transit route which contains all links between the provided from and to link (excluding the from stop and including the to stop)
	 */
	private List<Id<Link>> getRouteLinks(TransitRoute route, Id<Link> fromStopLinkId, Id<Link> toStopLinkId) {
		List<Id<Link>> routeSnippet = new ArrayList<>();

		List<Id<Link>> transitRouteInclFirstAndLastLink = new ArrayList<>();
		transitRouteInclFirstAndLastLink.add(route.getRoute().getStartLinkId());
		transitRouteInclFirstAndLastLink.addAll(route.getRoute().getLinkIds());
		transitRouteInclFirstAndLastLink.add(route.getRoute().getEndLinkId());

		boolean putInList = false;
		for (Id<Link> linkId : transitRouteInclFirstAndLastLink) {

			if (putInList) {
				routeSnippet.add(linkId);
			}

			if (linkId.toString().equals(fromStopLinkId.toString())) {
				putInList = true;
			}

			if (linkId.toString().equals(toStopLinkId.toString())) {
				break;
			}
		}

		if (routeSnippet.size() < 2) {
			log.warn("fromLink: " + fromStopLinkId + " --> toLink: " + toStopLinkId);
			log.warn("Route: " + route.getRoute().toString());
			throw new RuntimeException("Route snippet should at least have two links. Aborting... " + routeSnippet.toString());
		}

		return routeSnippet;
	}

	private double computeRouteTravelTime(List<Id<Link>> routeLinks, Id<TransitLine> lineId, Id<TransitRoute> routeId, Id<VehicleType> vehicleType) {
		double ttNetwork = 0.;
		for (Id<Link> linkId : routeLinks) {
			Link link = this.scenario.getNetwork().getLinks().get(linkId);
			double linkFreespeed = Double.NEGATIVE_INFINITY;
			if (railsimConfigGroup.getTrainSpeedApproach() == RailsimConfigGroup.TrainSpeedApproach.fromLinkAttributesForEachLine) {
				linkFreespeed = RailsimUtils.getLinkFreespeedForTransitLine(lineId, link);
			} else if (railsimConfigGroup.getTrainSpeedApproach() == RailsimConfigGroup.TrainSpeedApproach.fromLinkAttributesForEachVehicleType) {
				linkFreespeed = RailsimUtils.getLinkFreespeedForVehicleType(vehicleType, link);
			} else if (railsimConfigGroup.getTrainSpeedApproach() == RailsimConfigGroup.TrainSpeedApproach.fromLinkAttributesForEachLineAndRoute) {
				linkFreespeed = RailsimUtils.getLinkFreespeedForTransitLineAndTransitRoute(lineId, routeId, link);
			} else {
				linkFreespeed = link.getFreespeed();
			}
			double ttLink = link.getLength() / linkFreespeed;
			ttNetwork += ttLink;
		}
		return ttNetwork;
	}

	private double computeRouteTravelDistance(List<Id<Link>> routeLinks) {
		double distance = 0.;
		for (Id<Link> linkId : routeLinks) {
			Link link = this.scenario.getNetwork().getLinks().get(linkId);
			double distanceLink = link.getLength();
			distance += distanceLink;
		}
		return distance;
	}

}
