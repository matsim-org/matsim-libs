package ch.sbb.matsim.contrib.railsim.prototype.supply;

import ch.sbb.matsim.contrib.railsim.prototype.RailsimConfigGroup;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;

/**
 * Railsim supply config group
 * <p>
 * Config group to configure the supply generation for railsim.
 *
 * @author Merlin Unterfinger
 */
public class RailsimSupplyConfigGroup extends ReflectiveConfigGroup {

	private static final Logger log = LogManager.getLogger(RailsimConfigGroup.class);
	public static final String GROUP_NAME = "railsimSupply";
	// stop
	private static final String STOP_LINK_LENGTH = "stopLinkLength"; // 751.
	private static final String STOP_TRAIN_CAPACITY = "stopTrainCapacity"; // 3
	private static final String STOP_SPEED_LIMIT = "stopSpeedLimit"; // 90. / 3.6;
	// route
	private static final String ROUTE_TRAIN_CAPACITY = "routeTrainCapacity"; // 1
	private static final String ROUTE_SPEED_LIMIT = "routeSpeedLimit"; // 100. / 3.6;
	private static final String ROUTE_EUCLIDEAN_DISTANCE_FACTOR = "routeEuclideanDistanceFactor"; // 1
	// depot
	private static final String DEPOT_TRAIN_CAPACITY = "depotTrainCapacity"; // 999
	private static final String DEPOT_IN_OUT_CAPACITY = "depotInOutCapacity"; // 1
	private static final String DEPOT_OFFSET = "depotOffset"; // 100.
	private static final String DEPOT_TRAVEL_TIME = "depotTravelTime"; // 10 * 60
	private static final String DEPOT_SPEED_LIMIT = "depotSpeedLimit"; // 5./3.6
	private static final String DEPOT_LINK_LENGTH = "depotLinkLength"; // 751.
	// vehicle
	private static final String VEHICLE_PASSENGER_CAPACITY = "vehiclePassengerCapacity"; // 500
	private static final String VEHICLE_LENGTH = "vehicleLength"; // 200.
	private static final String VEHICLE_MAX_VELOCITY = "vehicleMaxVelocity"; // 150 / 3.6
	private static final String VEHICLE_MAX_DECELERATION = "vehicleMaxDeceleration"; // 0.5
	private static final String VEHICLE_MAX_ACCELERATION = "vehicleMaxAcceleration"; // 0.5
	private static final String VEHICLE_TURNAROUND_TIME = "vehicleTurnaroundTime"; // 5. * 60
	// circuit
	private static final String CIRCUIT_MAX_WAITING_TIME = "circuitMaxWaitingTime"; // 20 * 60
	private static final String CIRCUIT_PLANNING_APPROACH = "circuitPlanningApproach"; // DEFAULT

	public enum CircuitPlanningApproach {DEFAULT, NONE}

	/**
	 * Ctor
	 */
	public RailsimSupplyConfigGroup() {
		super(GROUP_NAME);
	}

	// stop
	private double stopLinkLength = 751.; // meters
	private int stopTrainCapacity = 3; // number of rails in station
	private double stopSpeedLimit = 90. / 3.6; // meters per second
	// route
	private int routeTrainCapacity = 1; // number of rails
	private double routeSpeedLimit = 120. / 3.6; // meters per second
	private double routeEuclideanDistanceFactor = 1.; // factor
	// depot
	private int depotTrainCapacity = 999; // number of trains
	private int depotInOutCapacity = 1; // number of rails
	private double depotOffset = 100.; // meters
	private double depotTravelTime = 10. * 60; // seconds
	private double depotSpeedLimit = 5. / 3.6; // meters per second
	private double depotLinkLength = 751.; // meters
	// vehicle
	private int vehiclePassengerCapacity = 500;
	private double vehicleLength = 200.;
	private double vehicleMaxVelocity = 150 / 3.6; // meters per second
	private double vehicleMaxAcceleration = 0.5;  // meters per seconds^2
	private double vehicleMaxDeceleration = 0.5;  // meters per seconds^2
	private double vehicleTurnaroundTime = 5. * 60; // seconds
	// circuit
	private double circuitMaxWaitingTime = 20. * 60; // meters per second
	private CircuitPlanningApproach circuitPlanningApproach = CircuitPlanningApproach.DEFAULT;  // options

	@Override
	public Map<String, String> getComments() {
		Map<String, String> comments = super.getComments();
		// stop
		comments.put(STOP_LINK_LENGTH, "Length of the stop links in the stations, should be longer than the longest stopping train.");
		comments.put(STOP_TRAIN_CAPACITY, "The number of parallel rails in the station sections.");
		comments.put(STOP_SPEED_LIMIT, "The maximum speed allowed in the station in meters per second.");
		// route
		comments.put(ROUTE_TRAIN_CAPACITY, "The number of parallel rails of the route sections. ");
		comments.put(ROUTE_SPEED_LIMIT, "The maximum speed allowed on the route sections in meters per second.");
		comments.put(ROUTE_EUCLIDEAN_DISTANCE_FACTOR, "Factor to scale the euclidean distance between to stops for the route length definition.");
		// depot
		comments.put(DEPOT_TRAIN_CAPACITY, "The number of trains allowed in the depot, should be high enough to prevent grid locks.");
		comments.put(DEPOT_IN_OUT_CAPACITY, "The number of rails that are connecting a depot to its stop.");
		comments.put(DEPOT_OFFSET, "The vertical offset in meters, to place the depot to the stop.");
		comments.put(DEPOT_TRAVEL_TIME, "The travel time to enter a depot from a stop or to reach the stop from the depot.");
		comments.put(DEPOT_SPEED_LIMIT, "The speed allowed in the depot and on the connecting links.");
		comments.put(DEPOT_LINK_LENGTH, "The length of the depot link, should be larger than the maximum vehicle length.");
		// vehicle
		comments.put(VEHICLE_PASSENGER_CAPACITY, "The number of passengers per vehicle.");
		comments.put(VEHICLE_LENGTH, "The total length of the vehicle in meters.");
		comments.put(VEHICLE_MAX_VELOCITY, "The maximum speed the vehicle is capable of driving.");
		comments.put(VEHICLE_MAX_ACCELERATION, "The maximum braking acceleration of the vehicle.");
		comments.put(VEHICLE_MAX_DECELERATION, "The maximum braking declaration of the vehicle.");
		comments.put(VEHICLE_TURNAROUND_TIME, "The time it takes the vehicle to turnaround in station (changing the driver's cab)");
		// circuit
		comments.put(CIRCUIT_MAX_WAITING_TIME, "The maximum waiting time of a vehicle on a stop link for the next circuit. If the next departure is after this waiting time, the vehicle is sent to the " + "depot.");
		comments.put(CIRCUIT_PLANNING_APPROACH, "The circuits planning approach: " + Arrays.toString(CircuitPlanningApproach.values()) + ". " + CircuitPlanningApproach.DEFAULT + " Plan simple vehicle circuits (default). " + CircuitPlanningApproach.NONE + " Omit vehicle circuits and sent a new vehicle from depot to depot for each route (needs a high depot capacity).");
		return comments;
	}

	@Override
	protected void checkConsistency(Config config) {
		log.info("Checking consistency in railsim preparation config group...");
		for (Field field : this.getClass().getDeclaredFields()) {
			if (field.getType().isPrimitive() && Number.class.isAssignableFrom(field.getType())) {
				try {
					field.setAccessible(true);
					Number value = (Number) field.get(this);
					if (value.doubleValue() < 0) {
						throw new RuntimeException(String.format("Negative value %f found for %s", value.doubleValue(), field.getName()));
					}
				} catch (IllegalAccessException e) {
					log.error("Unable to check field {}", field);
				}
			}
		}
	}

	@StringGetter(STOP_LINK_LENGTH)
	public double getStopLinkLength() {
		return stopLinkLength;
	}

	@StringSetter(STOP_LINK_LENGTH)
	public void setStopLinkLength(double stopLinkLength) {
		this.stopLinkLength = stopLinkLength;
	}

	@StringGetter(STOP_TRAIN_CAPACITY)
	public int getStopTrainCapacity() {
		return stopTrainCapacity;
	}

	@StringSetter(STOP_TRAIN_CAPACITY)
	public void setStopTrainCapacity(int stopTrainCapacity) {
		this.stopTrainCapacity = stopTrainCapacity;
	}

	@StringGetter(STOP_SPEED_LIMIT)
	public double getStopSpeedLimit() {
		return stopSpeedLimit;
	}

	@StringSetter(STOP_SPEED_LIMIT)
	public void setStopSpeedLimit(double stopSpeedLimit) {
		this.stopSpeedLimit = stopSpeedLimit;
	}

	@StringGetter(ROUTE_TRAIN_CAPACITY)
	public int getRouteTrainCapacity() {
		return routeTrainCapacity;
	}

	@StringSetter(ROUTE_TRAIN_CAPACITY)
	public void setRouteTrainCapacity(int routeTrainCapacity) {
		this.routeTrainCapacity = routeTrainCapacity;
	}

	@StringGetter(ROUTE_SPEED_LIMIT)
	public double getRouteSpeedLimit() {
		return routeSpeedLimit;
	}

	@StringSetter(ROUTE_SPEED_LIMIT)
	public void setRouteSpeedLimit(double routeSpeedLimit) {
		this.routeSpeedLimit = routeSpeedLimit;
	}

	@StringGetter(ROUTE_EUCLIDEAN_DISTANCE_FACTOR)
	public double getRouteEuclideanDistanceFactor() {
		return routeEuclideanDistanceFactor;
	}

	@StringSetter(ROUTE_EUCLIDEAN_DISTANCE_FACTOR)
	public void setRouteEuclideanDistanceFactor(double routeEuclideanDistanceFactor) {
		this.routeEuclideanDistanceFactor = routeEuclideanDistanceFactor;
	}

	@StringGetter(DEPOT_TRAIN_CAPACITY)
	public int getDepotTrainCapacity() {
		return depotTrainCapacity;
	}

	@StringSetter(DEPOT_TRAIN_CAPACITY)
	public void setDepotTrainCapacity(int depotTrainCapacity) {
		this.depotTrainCapacity = depotTrainCapacity;
	}

	@StringGetter(DEPOT_IN_OUT_CAPACITY)
	public int getDepotInOutCapacity() {
		return depotInOutCapacity;
	}

	@StringSetter(DEPOT_IN_OUT_CAPACITY)
	public void setDepotInOutCapacity(int depotInOutCapacity) {
		this.depotInOutCapacity = depotInOutCapacity;
	}

	@StringGetter(DEPOT_OFFSET)
	public double getDepotOffset() {
		return depotOffset;
	}

	@StringSetter(DEPOT_OFFSET)
	public void setDepotOffset(double depotOffset) {
		this.depotOffset = depotOffset;
	}

	@StringGetter(DEPOT_TRAVEL_TIME)
	public double getDepotTravelTime() {
		return depotTravelTime;
	}

	@StringSetter(DEPOT_TRAVEL_TIME)
	public void setDepotTravelTime(double depotTravelTime) {
		this.depotTravelTime = depotTravelTime;
	}

	@StringGetter(DEPOT_SPEED_LIMIT)
	public double getDepotSpeedLimit() {
		return depotSpeedLimit;
	}

	@StringSetter(DEPOT_SPEED_LIMIT)
	public void setDepotSpeedLimit(double depotSpeedLimit) {
		this.depotSpeedLimit = depotSpeedLimit;
	}

	@StringGetter(DEPOT_LINK_LENGTH)
	public double getDepotLinkLength() {
		return depotLinkLength;
	}

	@StringSetter(DEPOT_LINK_LENGTH)
	public void setDepotLinkLength(double depotLinkLength) {
		this.depotLinkLength = depotLinkLength;
	}

	@StringGetter(VEHICLE_PASSENGER_CAPACITY)
	public int getVehiclePassengerCapacity() {
		return vehiclePassengerCapacity;
	}

	@StringSetter(VEHICLE_PASSENGER_CAPACITY)
	public void setVehiclePassengerCapacity(int vehiclePassengerCapacity) {
		this.vehiclePassengerCapacity = vehiclePassengerCapacity;
	}

	@StringGetter(VEHICLE_LENGTH)
	public double getVehicleLength() {
		return vehicleLength;
	}

	@StringSetter(VEHICLE_LENGTH)
	public void setVehicleLength(double vehicleLength) {
		this.vehicleLength = vehicleLength;
	}

	@StringGetter(VEHICLE_MAX_VELOCITY)
	public double getVehicleMaxVelocity() {
		return vehicleMaxVelocity;
	}

	@StringSetter(VEHICLE_MAX_VELOCITY)
	public void setVehicleMaxVelocity(double vehicleMaxVelocity) {
		this.vehicleMaxVelocity = vehicleMaxVelocity;
	}

	@StringGetter(VEHICLE_MAX_ACCELERATION)
	public double getVehicleMaxAcceleration() {
		return vehicleMaxAcceleration;
	}

	@StringSetter(VEHICLE_MAX_ACCELERATION)
	public void setVehicleMaxAcceleration(double vehicleMaxAcceleration) {
		this.vehicleMaxAcceleration = vehicleMaxAcceleration;
	}

	@StringGetter(VEHICLE_MAX_DECELERATION)
	public double getVehicleMaxDeceleration() {
		return vehicleMaxDeceleration;
	}

	@StringSetter(VEHICLE_MAX_DECELERATION)
	public void setVehicleMaxDeceleration(double vehicleMaxDeceleration) {
		this.vehicleMaxDeceleration = vehicleMaxDeceleration;
	}

	@StringGetter(VEHICLE_TURNAROUND_TIME)
	public double getVehicleTurnaroundTime() {
		return vehicleTurnaroundTime;
	}

	@StringSetter(VEHICLE_TURNAROUND_TIME)
	public void setVehicleTurnaroundTime(double vehicleTurnaroundTime) {
		this.vehicleTurnaroundTime = vehicleTurnaroundTime;
	}

	@StringGetter(CIRCUIT_MAX_WAITING_TIME)
	public double getCircuitMaxWaitingTime() {
		return circuitMaxWaitingTime;
	}

	@StringSetter(CIRCUIT_MAX_WAITING_TIME)
	public void setCircuitMaxWaitingTime(double circuitMaxWaitingTime) {
		this.circuitMaxWaitingTime = circuitMaxWaitingTime;
	}

	@StringGetter(CIRCUIT_PLANNING_APPROACH)
	public CircuitPlanningApproach getCircuitPlanningApproach() {
		return circuitPlanningApproach;
	}

	@StringSetter(CIRCUIT_PLANNING_APPROACH)
	public void setCircuitPlanningApproach(CircuitPlanningApproach circuitPlanningApproach) {
		this.circuitPlanningApproach = circuitPlanningApproach;
	}
}
