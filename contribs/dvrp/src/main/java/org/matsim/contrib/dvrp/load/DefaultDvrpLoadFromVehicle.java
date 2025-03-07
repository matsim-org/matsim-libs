package org.matsim.contrib.dvrp.load;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.matsim.utils.objectattributes.attributable.Attributable;
import org.matsim.vehicles.Vehicle;

/**
 * @author Tarek Chouaki (tkchouaki), IRT SystemX
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class DefaultDvrpLoadFromVehicle implements DvrpLoadFromVehicle {
	public static final String CAPACITY_ATTRIBUTE = "dvrp:capacity";
	public static final String CAPACITY_ATTRIBUTE_PREFIX = "dvrp:capacity:";

	private final DvrpLoadType loadType;
	private final CapacityMapping mapping;

	public DefaultDvrpLoadFromVehicle(DvrpLoadType loadType, CapacityMapping mapping) {
		this.loadType = loadType;
		this.mapping = mapping;
	}

	@Override
	public DvrpLoad getLoad(Vehicle vehicle) {
		boolean hasCapacityAttribute = false;
		hasCapacityAttribute |= hasCapacityAttribute(vehicle);
		hasCapacityAttribute |= hasCapacityAttribute(vehicle.getType());

		boolean hasCapacityPrefix = false;
		hasCapacityPrefix |= hasCapacityPrefix(vehicle);
		hasCapacityPrefix |= hasCapacityPrefix(vehicle.getType());

		if (hasCapacityAttribute && hasCapacityPrefix) {
			throw new IllegalStateException(String.format(
					"Cannot process a mix of string-based capacities and slot-based attributes for vehicle %s of type %s",
					vehicle.getId().toString(), vehicle.getType().getId().toString()));
		}

		if (hasCapacityAttribute) {
			// serialization of a load string

			if (hasCapacityAttribute(vehicle)) {
				// vehilce has priority
				return loadType.deserialize((String) vehicle.getAttributes().getAttribute(CAPACITY_ATTRIBUTE));
			} else {
				// then fall back to vehicle type
				return loadType
						.deserialize((String) vehicle.getType().getAttributes().getAttribute(CAPACITY_ATTRIBUTE));
			}
		} else {
			// compose capacities using the given information
			Map<String, Number> values = new HashMap<>();

			if (mapping.seats != null) {
				values.put(mapping.seats, Objects.requireNonNull(vehicle.getType().getCapacity().getSeats(), String
						.format("No seat capacity given for vehicle type %s", vehicle.getType().getId().toString())));
			}

			if (mapping.standingRoom != null) {
				values.put(mapping.standingRoom,
						Objects.requireNonNull(vehicle.getType().getCapacity().getStandingRoom(),
								String.format("No standing room capacity given for vehicle type %s",
										vehicle.getType().getId().toString())));
			}

			if (mapping.volume != null) {
				values.put(mapping.volume,
						Objects.requireNonNull(vehicle.getType().getCapacity().getVolumeInCubicMeters(), String.format(
								"No volume capacity given for vehicle type %s", vehicle.getType().getId().toString())));
			}

			if (mapping.weight != null) {
				values.put(mapping.weight,
						Objects.requireNonNull(vehicle.getType().getCapacity().getWeightInTons(), String.format(
								"No weight capacity given for vehicle type %s", vehicle.getType().getId().toString())));
			}

			if (mapping.other != null) {
				values.put(mapping.other, Objects.requireNonNull(vehicle.getType().getCapacity().getOther(), String
						.format("No other capacity given for vehicle type %s", vehicle.getType().getId().toString())));
			}

			if (hasCapacityPrefix) {
				// override values from vehicle attributes
				for (var entry : vehicle.getType().getAttributes().getAsMap().entrySet()) {
					if (entry.getKey().startsWith(CAPACITY_ATTRIBUTE_PREFIX)) {
						String name = entry.getKey().substring(CAPACITY_ATTRIBUTE_PREFIX.length());
						values.put(name, (Number) entry.getValue());
					}
				}

				// override values from vehicles
				for (var entry : vehicle.getAttributes().getAsMap().entrySet()) {
					if (entry.getKey().startsWith(CAPACITY_ATTRIBUTE_PREFIX)) {
						String name = entry.getKey().substring(CAPACITY_ATTRIBUTE_PREFIX.length());
						values.put(name, (Number) entry.getValue());
					}
				}
			}

			return loadType.fromMap(values);
		}
	}

	private boolean hasCapacityAttribute(Attributable attributable) {
		return attributable.getAttributes().getAsMap().containsKey(CAPACITY_ATTRIBUTE);
	}

	private boolean hasCapacityPrefix(Attributable attributable) {
		for (String attribute : attributable.getAttributes().getAsMap().keySet()) {
			if (attribute.startsWith(CAPACITY_ATTRIBUTE_PREFIX)) {
				return true;
			}
		}

		return false;
	}

	public static record CapacityMapping(
			String seats, String standingRoom, String volume, String weight, String other) {
		static public CapacityMapping build(DvrpLoadParams parameters) {
			return new CapacityMapping(parameters.mapVehicleTypeSeats, parameters.mapVehicleTypeStandingRoom,
					parameters.mapVehicleTypeVolume, parameters.mapVehicleTypeWeight, parameters.mapVehicleTypeOther);
		}
	}
}
