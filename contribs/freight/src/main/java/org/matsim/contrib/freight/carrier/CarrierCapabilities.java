package org.matsim.contrib.freight.carrier;

import com.graphhopper.jsprit.core.problem.Skills;
import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.VehicleType;

import java.util.*;

/**
 * This contains the capabilities/resources a carrier has/can deploy.
 *
 * <p>If a carrier has a fixed fleet-size, this should contain all carrierVehicles that a carrier can deploy (@see CarrierVehicle).
 * If the fleet configuration is part of the planning problem and the carrier can dimension its fleet, this should contain
 * the available carrierVehicleTypes (@see CarrierVehicleType) and the available depots. If certain types are only
 * available at certain depots, assign them to depotIds accordingly, otherwise it is assumed that every type can be
 * deployed at every depot.
 *
 * @author sschroeder, mzilske
 */
public class CarrierCapabilities {

	public enum FleetSize {
		INFINITE, FINITE
	}

	public static class Builder {

		public static Builder newInstance() {
			return new Builder();
		}

		private Collection<CarrierVehicleType> vehicleTypes = new ArrayList<>();

		private Collection<CarrierVehicle> vehicles = new ArrayList<>();

		private Set<Id<VehicleType>> typeIds = new HashSet<>();

		private FleetSize fleetSize = FleetSize.FINITE;

		public Builder setFleetSize(FleetSize fleetSize) {
			this.fleetSize = fleetSize;
			return this;
		}

		public Builder addType(CarrierVehicleType type) {
			if (!typeIds.contains(type.getId())) {
				vehicleTypes.add(type);
				typeIds.add(type.getId());
			}
			return this;
		}

		public Builder addVehicle(CarrierVehicle carrierVehicle) {
			vehicles.add(carrierVehicle);
			if (carrierVehicle.getVehicleType() != null) addType(carrierVehicle.getVehicleType());
			return this;
		}

		public CarrierCapabilities build() {
			return new CarrierCapabilities(this);
		}


	}

	/**
	 * Returns a new instance of CarrierCapabilities.
	 *
	 * <p>This method always returns an empty CarrierCapabilities object, i.e. with no capabilities.
	 *
	 * This is now deprecated (JWJ, Aug'19) as it allowed for multiple ways to
	 * instantiate the class. Instead, use {@link Builder} that is available
	 * right here in this class.
	 *
	 * @return an empty capability object
	 */
	@Deprecated
	public static CarrierCapabilities newInstance() {
		return new CarrierCapabilities();
	}

	private CarrierCapabilities() {
	}

	private CarrierCapabilities(Builder builder) {
		this.carrierVehicles = builder.vehicles;
		this.vehicleTypes = builder.vehicleTypes;
		this.fleetSize = builder.fleetSize;
	}

	private Collection<CarrierVehicle> carrierVehicles = new ArrayList<>();

	private Collection<CarrierVehicleType> vehicleTypes = new ArrayList<>();


	/**
	 * Sets the fleetSize.
	 *
	 * <p>FleetSize can be FleetSize.INFINITE and FleetSize.FINITE. If fleetSize is FleetSize.INFINITE then the vehicles in carrierVehicles are representative vehicles.
	 * Each representative vehicle can be employed infinite times.
	 * <p>If fleetSize is FleetSize.FINITE then the vehicles in carrierVehicles are exactly the vehicles
	 * the carrier can employ.
	 *
	 * <p>By default, it is FleetSize.FINITE
	 *
	 * @see FleetSize
	 */
	private FleetSize fleetSize = FleetSize.FINITE;

	/**
	 * Returns a collection of carrierVehicles, a carrier has to its disposal.
	 *
	 * @return collection of carrierVehicles
	 * @see CarrierVehicle
	 */
	public Collection<CarrierVehicle> getCarrierVehicles() {
		return carrierVehicles;
	}


	public FleetSize getFleetSize() {
		return fleetSize;
	}


	public void setFleetSize(FleetSize fleetSize) {
		this.fleetSize = fleetSize;
	}


	/**
	 * Returns a collection of CarrierVehicleTypes.
	 *
	 * @return a collection of vehicleTypes
	 * @see CarrierVehicleType
	 */
	public Collection<CarrierVehicleType> getVehicleTypes() {
		return vehicleTypes;
	}
}
