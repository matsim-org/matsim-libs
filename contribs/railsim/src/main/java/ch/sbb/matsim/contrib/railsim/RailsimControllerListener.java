package ch.sbb.matsim.contrib.railsim;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import ch.sbb.matsim.contrib.railsim.qsimengine.TrainManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.utils.objectattributes.attributable.AttributesUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

import com.google.inject.Inject;

/**
 * General railsim listener
 */
public class RailsimControllerListener implements StartupListener {

	private static final Logger log = LogManager.getLogger(RailsimControllerListener.class);

	private final Scenario scenario;

	@Inject
	public RailsimControllerListener(Scenario scenario) {
		this.scenario = scenario;
	}

	@Override
	public void notifyStartup(StartupEvent event) {

		createVehiclesForUnits(scenario.getTransitSchedule(), scenario.getTransitVehicles());
	}

	/**
	 * Create composite vehicle ids and types if Railsim formations are specified.
	 * For each departure with a formation, creates a combined vehicle and vehicle type.
	 */
	private void createVehiclesForUnits(TransitSchedule schedule, Vehicles vehicles) {

		int formationsProcessed = 0;

		for (TransitLine line : schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (Departure departure : route.getDepartures().values()) {

					List<String> formation = RailsimUtils.getFormation(departure);

					if (!formation.isEmpty()) {
						// This departure has a formation - create combined vehicle
						createCombinedVehicle(departure, formation, vehicles);
						formationsProcessed++;
					}
				}
			}
		}

		log.info("Processed {} departures with formations", formationsProcessed);
	}

	/**
	 * Create a combined vehicle and vehicle type for a departure with formation.
	 */
	private void createCombinedVehicle(Departure departure, List<String> formation, Vehicles vehicles) {

		// Create combined vehicle ID by combining unit IDs
		String combinedVehicleId = String.join("_", formation);
		Id<Vehicle> combinedId = Id.create(combinedVehicleId, Vehicle.class);

		// Check if combined vehicle already exists
		if (vehicles.getVehicles().containsKey(combinedId)) {
			log.debug("Combined vehicle {} already exists, skipping creation", combinedVehicleId);
			// Still update the departure to use the existing combined vehicle
			departure.setVehicleId(combinedId);
			return;
		}

		// Create combined vehicle type
		VehicleType combinedType = createCombinedVehicleType(formation, vehicles);
		if (combinedType == null) {
			log.warn("Could not create combined vehicle type for departure {}", departure.getId());
			return;
		}

		// Create combined vehicle
		Vehicle combinedVehicle = vehicles.getFactory().createVehicle(combinedId, combinedType);
		vehicles.addVehicle(combinedVehicle);

		// Update departure to use combined vehicle
		departure.setVehicleId(combinedId);

		log.debug("Created combined vehicle {} with {} units for departure {}",
			combinedVehicleId, formation.size(), departure.getId());
	}

	/**
	 * Create a combined vehicle type based on the formation units.
	 * Combines capacities and takes minimum speed.
	 */
	private VehicleType createCombinedVehicleType(List<String> formation, Vehicles vehicles) {

		if (formation.isEmpty()) {
			return null;
		}

		VehicleType[] unitTypes = new VehicleType[formation.size()];

		for (int i = 0; i < unitTypes.length; i++) {

			Vehicle vehicle = vehicles.getVehicles().get(Id.createVehicleId(formation.get(i)));
			if (vehicle == null) {
				throw new IllegalArgumentException("No vehicle type found for unit with vehicle id " + formation.get(i));
			}

			unitTypes[i] = vehicle.getType();
		}

		String combinedTypeId = Arrays.stream(unitTypes).map(t -> t.getId().toString()).collect(Collectors.joining("_"));

		// Create combined vehicle type ID
		Id<VehicleType> combinedTypeIdObj = Id.create(combinedTypeId, VehicleType.class);

		// Check if combined type already exists
		if (vehicles.getVehicleTypes().containsKey(combinedTypeIdObj)) {
			return vehicles.getVehicleTypes().get(combinedTypeIdObj);
		}

		// Create combined vehicle type
		VehicleType combinedType = vehicles.getFactory().createVehicleType(combinedTypeIdObj);

		// Combine properties from all unit types
		combineVehicleTypeProperties(combinedType, unitTypes);

		// Add to vehicles
		vehicles.addVehicleType(combinedType);

		log.debug("Created combined vehicle type {} based on {} unit types", combinedTypeId, formation.size());

		return combinedType;
	}

	/**
	 * Combine properties from multiple vehicle types into one.
	 * Adds capacities, takes minimum speed, combines other properties.
	 */
	private void combineVehicleTypeProperties(VehicleType combinedType, VehicleType[] unitTypes) {

		if (unitTypes.length == 0) {
			return;
		}

		// Use first type as base
		VehicleType baseType = unitTypes[0];

		// Copy basic properties from first type
		combinedType.setLength(baseType.getLength());
		combinedType.setWidth(baseType.getWidth());
		combinedType.setMaximumVelocity(baseType.getMaximumVelocity());
		combinedType.setPcuEquivalents(baseType.getPcuEquivalents());
		combinedType.setNetworkMode(baseType.getNetworkMode());

		// Copy capacity from first type
		combinedType.getCapacity().setSeats(baseType.getCapacity().getSeats());
		combinedType.getCapacity().setStandingRoom(baseType.getCapacity().getStandingRoom());

		// Combine with other types
		for (int i = 1; i < unitTypes.length; i++) {
			VehicleType unitType = unitTypes[i];

			// Add capacities
			combinedType.getCapacity().setSeats(
				combinedType.getCapacity().getSeats() + unitType.getCapacity().getSeats()
			);
			combinedType.getCapacity().setStandingRoom(
				combinedType.getCapacity().getStandingRoom() + unitType.getCapacity().getStandingRoom()
			);

			// Take minimum speed
			combinedType.setMaximumVelocity(
				Math.min(combinedType.getMaximumVelocity(), unitType.getMaximumVelocity())
			);

			// Add PcuEquivalents
			combinedType.setPcuEquivalents(
				combinedType.getPcuEquivalents() + unitType.getPcuEquivalents()
			);

			// Add length (trains are connected)
			combinedType.setLength(combinedType.getLength() + unitType.getLength());
		}

		// Copy all attributes from first type
		AttributesUtils.copyTo(baseType.getAttributes(), combinedType.getAttributes());

		log.debug("Combined vehicle type: seats={}, standing={}, maxVel={}, length={}",
			combinedType.getCapacity().getSeats(),
			combinedType.getCapacity().getStandingRoom(),
			combinedType.getMaximumVelocity(),
			combinedType.getLength());
	}

}
