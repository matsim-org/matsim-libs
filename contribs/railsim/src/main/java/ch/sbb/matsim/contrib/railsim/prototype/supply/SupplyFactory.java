package ch.sbb.matsim.contrib.railsim.prototype.supply;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.utils.objectattributes.attributable.Attributable;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Supply factory
 * <p>
 * Create MATSim instances and add them to a transit schedule and network.
 *
 * @author Merlin Unterfinger
 */
class SupplyFactory {
	private static final Logger log = LogManager.getLogger(SupplyFactory.class);
	private static final double DEFAULT_LINK_FREESPEED = 999.;
	private static final double DEFAULT_LINK_CAPACITY = 999.;
	private static final double DEFAULT_LINK_LANES = 1.;
	private static final String DEFAULT_LINK_MODE = "rail";
	private static final double DEFAULT_VEHICLE_ACCESS_TIME = 1.;
	private static final double DEFAULT_VEHICLE_EGRESS_TIME = 1.;
	private static final String ID_FORMAT_DEPARTURE = "%s";
	private static final String ID_FORMAT_TRANSIT_LINE = "%s_line";
	private static final String ID_FORMAT_TRANSIT_ROUTE = "%s_route";
	private static final String ID_FORMAT_TRANSIT_STOP = "%s";
	private static final String ID_FORMAT_VEHICLE = "%s";
	private static final String ID_FORMAT_VEHICLE_TYPE = "train_%s";
	private static final String ID_FORMAT_LINK = "%s_%s-%s";
	private static final String ID_FORMAT_NODE = "%s";
	private final TransitSchedule schedule;
	private final Vehicles vehicles;
	private final Network network;
	private final TransitScheduleFactory sf;
	private final VehiclesFactory vf;
	private final NetworkFactory nf;


	SupplyFactory(Scenario scenario) {
		schedule = scenario.getTransitSchedule();
		vehicles = scenario.getTransitVehicles();
		network = scenario.getNetwork();
		sf = schedule.getFactory();
		vf = vehicles.getFactory();
		nf = network.getFactory();
	}

	Departure createDeparture(String id, double time) {
		var departureId = Id.create(String.format(ID_FORMAT_DEPARTURE, id), Departure.class);
		log.debug("Creating Departure {}", departureId);
		return sf.createDeparture(departureId, time);
	}

	TransitLine createTransitLine(String id) {
		var lineId = Id.create(String.format(ID_FORMAT_TRANSIT_LINE, id), TransitLine.class);
		var transitLine = schedule.getTransitLines().get(lineId);
		if (transitLine != null) {
			log.warn("TransitLine {} is already existing", lineId);
			return transitLine;
		}
		log.debug("Creating TransitLine {}", lineId);
		transitLine = sf.createTransitLine(lineId);
		schedule.addTransitLine(transitLine);
		return transitLine;
	}

	TransitRoute createTransitRoute(TransitLine transitLine, String id, List<Id<Link>> routeLinks, List<TransitRouteStop> stops) {
		var routeId = Id.create(String.format(ID_FORMAT_TRANSIT_ROUTE, id), TransitRoute.class);
		var networkRoute = RouteUtils.createLinkNetworkRouteImpl(routeLinks.get(0), routeLinks.subList(1, routeLinks.size() - 1), routeLinks.get(routeLinks.size() - 1));
		var transitRoute = transitLine.getRoutes().get(routeId);
		if (transitRoute != null) {
			log.warn("TransitRoute {} is already existing on TransitLine {}", routeId, transitLine.getId());
			return transitRoute;
		}
		log.debug("Creating TransitRoute {} and adding to TransitLine {}", routeId, transitLine.getId());
		transitRoute = sf.createTransitRoute(routeId, networkRoute, stops, DEFAULT_LINK_MODE);
		transitLine.addRoute(transitRoute);
		return transitRoute;
	}

	TransitRouteStop createTransitRouteStop(TransitStopFacility transitStopFacility, double cumulativeTravelTime, double departureTime, boolean awaitDeparture) {
		log.debug("Creating TransitRouteStop at TransitStopFacility {}", transitStopFacility.getId());
		var transitRouteStop = sf.createTransitRouteStop(transitStopFacility, cumulativeTravelTime, departureTime);
		transitRouteStop.setAwaitDepartureTime(awaitDeparture);
		return transitRouteStop;
	}

	TransitRouteStop createTransitRouteStop(TransitStopFacility transitStopFacility, double cumulativeTravelTime) {
		log.debug("Creating TransitRouteStop at TransitStopFacility {} (using builder)", transitStopFacility.getId());
		return sf.createTransitRouteStopBuilder(transitStopFacility).departureOffset(cumulativeTravelTime).build();
	}

	TransitStopFacility createTransitStopFacility(String id, Link link) {
		var stopId = Id.create(String.format(ID_FORMAT_TRANSIT_STOP, id), TransitStopFacility.class);
		var transitStopFacility = schedule.getFacilities().get(stopId);
		if (transitStopFacility != null) {
			log.warn("TransitStopFacility {} is already existing", stopId);
			return transitStopFacility;
		}
		log.debug("Creating TransitStopFacility {}", stopId);
		transitStopFacility = sf.createTransitStopFacility(stopId, link.getToNode().getCoord(), false);
		transitStopFacility.setLinkId(link.getId());
		schedule.addStopFacility(transitStopFacility);
		return transitStopFacility;
	}

	Node createNode(String id, Coord coord) {
		var nodeId = Id.create(String.format(ID_FORMAT_NODE, id), Node.class);
		var node = network.getNodes().get(nodeId);
		if (node != null) {
			log.warn("Node {} is already existing", nodeId);
			return node;
		}
		log.debug("Creating Node {}", nodeId);
		node = nf.createNode(nodeId, coord);
		network.addNode(node);
		return node;
	}

	Link createLink(LinkType linkType, Node fromNode, Node toNode, double length, Map<String, Object> attributes) {
		var linkId = Id.create(String.format(ID_FORMAT_LINK, linkType.getAbbreviation(), fromNode.getId().toString(), toNode.getId().toString()), Link.class);
		var link = network.getLinks().get(linkId);
		if (link != null) {
			log.warn("Link {} is already existing", linkId);
			return link;
		}
		log.debug("Creating Link {}", linkId);
		link = nf.createLink(linkId, fromNode, toNode);
		link.setAllowedModes(new HashSet<>(List.of(DEFAULT_LINK_MODE)));
		link.setLength(length);
		link.setFreespeed(DEFAULT_LINK_FREESPEED);
		link.setCapacity(DEFAULT_LINK_CAPACITY);
		link.setNumberOfLanes(DEFAULT_LINK_LANES);
		putAttributes(link, attributes);
		network.addLink(link);
		return link;
	}

	/**
	 * Creates a new vehicle type or returns if already existing.
	 *
	 * @param id          vehicle type id.
	 * @param length      the length of the complete vehicle in meters.
	 * @param maxVelocity maximum velocity the vehicle type is allowed to drive (m/s).
	 * @param capacity    the passenger capacity of the vehicle type.
	 * @param attributes  a map holding further attributes (acceleration, deceleration)
	 * @return the corresponding vehicle type.
	 */
	VehicleType getOrCreateVehicleType(String id, double length, double maxVelocity, int capacity, Map<String, Object> attributes) {
		Id<VehicleType> vehicleTypeId = Id.create(String.format(ID_FORMAT_VEHICLE_TYPE, id), VehicleType.class);
		VehicleType vehicleType = vehicles.getVehicleTypes().get(vehicleTypeId);
		if (vehicleType != null) {
			log.debug("VehicleType {} is already existing", vehicleTypeId);
			return vehicleType;
		}
		log.debug("Creating VehicleType {}", vehicleTypeId);
		vehicleType = vf.createVehicleType(vehicleTypeId);
		vehicleType.getCapacity().setSeats(capacity);
		vehicleType.setMaximumVelocity(maxVelocity);
		VehicleUtils.setDoorOperationMode(vehicleType, VehicleType.DoorOperationMode.parallel);
		VehicleUtils.setAccessTime(vehicleType, DEFAULT_VEHICLE_ACCESS_TIME);
		VehicleUtils.setEgressTime(vehicleType, DEFAULT_VEHICLE_EGRESS_TIME);
		vehicleType.setLength(length);
		putAttributes(vehicleType, attributes);
		vehicles.addVehicleType(vehicleType);
		return vehicleType;
	}

	/**
	 * Creates a new vehicle for a given vehicle type
	 * <p>
	 * The vehicle id is set to the vehicle type name and the count of the total amount of existing vehicles of this type.
	 * <p>
	 * Note: The vehicle type has to be created beforehand using getOrCreateVehicleType.
	 *
	 * @param vehicleType The type of the vehicle to create.
	 * @return The created vehicle.
	 */
	Vehicle getOrCreateVehicle(VehicleType vehicleType, String id) {
		Id<Vehicle> vehicleId = Id.create(String.format(ID_FORMAT_VEHICLE, id), Vehicle.class);
		Vehicle vehicle = vehicles.getVehicles().get(vehicleId);
		if (vehicle != null) {
			log.debug("Vehicle {} is already existing", vehicleId);
			return vehicle;
		}
		log.debug("Creating Vehicle {}", vehicleId);
		vehicle = vf.createVehicle(vehicleId, vehicleType);
		vehicles.addVehicle(vehicle);
		return vehicle;
	}

	private static void putAttributes(Attributable object, Map<String, Object> attributes) {
		for (var entry : attributes.entrySet()) {
			object.getAttributes().putAttribute(entry.getKey(), entry.getValue());
		}
	}
}
