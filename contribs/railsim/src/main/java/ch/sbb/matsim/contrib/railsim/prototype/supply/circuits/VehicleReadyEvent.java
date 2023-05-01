package ch.sbb.matsim.contrib.railsim.prototype.supply.circuits;

/**
 * Vehicle ready event
 * <p>
 * An event holding the time when the vehicle is ready again for a departure from a stop link or a depot.
 *
 * @param vehicle the vehicle.
 * @param time    the time when the vehicle is ready again.
 * @author Merlin Unterfinger
 */
record VehicleReadyEvent(Vehicle vehicle, double time) {

}
