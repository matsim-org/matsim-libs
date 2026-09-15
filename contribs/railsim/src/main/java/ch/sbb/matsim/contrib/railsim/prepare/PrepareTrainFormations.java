package ch.sbb.matsim.contrib.railsim.prepare;

import java.util.*;

import ch.sbb.matsim.contrib.railsim.RailsimUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.MatsimVehicleReader;
import org.matsim.vehicles.MatsimVehicleWriter;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

import picocli.CommandLine;

@CommandLine.Command(
	name = "prepare-train-formations",
	description = "Utility class to set train unit ids automatically based on chained departures in the transit schedule."
)
public class PrepareTrainFormations implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(PrepareTrainFormations.class);

	@CommandLine.Option(names = "--input-schedule", required = true, description = "Input transit schedule file")
	private String inputSchedule;

	@CommandLine.Option(names = "--input-vehicles", required = true, description = "Input transit vehicles file")
	private String inputVehicles;

	@CommandLine.Option(names = "--output-schedule", required = true, description = "Output transit schedule file")
	private String outputSchedule;

	@CommandLine.Option(names = "--output-vehicles", required = true, description = "Output transit vehicles file")
	private String outputVehicles;

	@SuppressWarnings("unused")
	public PrepareTrainFormations() {
	}

	public static void main(String[] args) {
		new PrepareTrainFormations().execute(args);
	}

	/**
	 * Check if a formation contains empty or null unit IDs.
	 */
	private static boolean hasEmptyUnitIds(List<String> formation) {
		return formation.stream().anyMatch(unitId -> unitId == null || unitId.trim().isEmpty());
	}

	/**
	 * Create individual vehicles for each unit in the formation.
	 * Each unit gets its own vehicle based on the original vehicle type.
	 */
	private static void createUnitVehicles(Departure departure, List<String> units, Vehicles vehicles) {
		if (departure.getVehicleId() == null) {
			log.warn("Departure {} has no vehicle ID, cannot create unit vehicles", departure.getId());
			return;
		}

		// Get the original vehicle and its type
		Vehicle originalVehicle = vehicles.getVehicles().get(departure.getVehicleId());
		if (originalVehicle == null) {
			log.warn("Original vehicle {} not found for departure {}", departure.getVehicleId(), departure.getId());
			return;
		}

		VehicleType originalType = originalVehicle.getType();
		String baseVehicleId = departure.getVehicleId().toString();

		// Create a vehicle for each unit
		for (String unitId : units) {
			// Validate unit ID
			if (unitId == null || unitId.trim().isEmpty()) {
				log.error("Cannot create vehicle for empty unit ID in departure {}", departure.getId());
				continue;
			}

			Id<Vehicle> unitVehicleId = Id.create(unitId, Vehicle.class);

			// Check if vehicle already exists
			if (vehicles.getVehicles().containsKey(unitVehicleId)) {
				log.debug("Vehicle {} already exists, skipping creation", unitId);
				continue;
			}

			// Create new vehicle for this unit
			Vehicle unitVehicle = vehicles.getFactory().createVehicle(unitVehicleId, originalType);
			vehicles.addVehicle(unitVehicle);

			log.debug("Created vehicle {} for unit {} (based on {})", unitVehicleId, unitId, baseVehicleId);
		}
	}

	/**
	 * Set train formations based on chained departures to handle train splitting and merging.
	 * Automatically assigns unit IDs so that units are tracked correctly when trains split or join.
	 */
	public static void setTrainFormations(TransitSchedule schedule, Vehicles vehicles) {

		// Map to track which units are assigned to which departures
		Map<Id<Departure>, List<String>> departureUnits = new HashMap<>();

		// First pass: collect all departures and their chaining relationships
		Map<Id<Departure>, Departure> allDepartures = new HashMap<>();
		Map<Id<Departure>, List<Id<Departure>>> incomingChains = new HashMap<>();
		Map<Id<Departure>, List<Id<Departure>>> outgoingChains = new HashMap<>();

		for (TransitLine line : schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (Departure departure : route.getDepartures().values()) {
					allDepartures.put(departure.getId(), departure);

					// Track outgoing chains (this departure chains to others)
					List<Id<Departure>> outgoing = new ArrayList<>();
					for (ChainedDeparture chained : departure.getChainedDepartures()) {
						Id<Departure> chainedId = chained.getChainedDepartureId();
						outgoing.add(chainedId);

						// Track incoming chains (others chain to this departure)
						incomingChains.computeIfAbsent(chainedId, k -> new ArrayList<>()).add(departure.getId());
					}
					outgoingChains.put(departure.getId(), outgoing);
				}
			}
		}

		// Second pass: assign units based on chaining patterns
		// Process departures in chronological order to handle dependencies correctly
		List<Departure> allDeparturesList = new ArrayList<>();
		for (TransitLine line : schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				allDeparturesList.addAll(route.getDepartures().values());
			}
		}

		// Sort by departure time to ensure proper processing order
		allDeparturesList.sort(Comparator.comparingDouble(Departure::getDepartureTime));

		for (Departure departure : allDeparturesList) {
			Id<Departure> depId = departure.getId();
			List<String> units = new ArrayList<>();

			// Check if this departure is a merge point (multiple incoming chains)
			List<Id<Departure>> incoming = incomingChains.getOrDefault(depId, new ArrayList<>());

			if (incoming.size() > 1) {
				// MERGING: Multiple trains merge into this one
				// Collect units from all incoming departures
				for (Id<Departure> incomingId : incoming) {
					List<String> incomingUnits = departureUnits.get(incomingId);
					if (incomingUnits != null) {
						units.addAll(incomingUnits);
					}
				}
			} else if (incoming.size() == 1) {
				// CONTINUATION: This is a continuation of a previous train
				Id<Departure> previousId = incoming.get(0);
				List<String> previousUnits = departureUnits.get(previousId);
				if (previousUnits != null) {
					units.addAll(previousUnits);
				}
			} else {
				// NEW TRAIN: No incoming chains, create new units
				List<Id<Departure>> outgoing = outgoingChains.getOrDefault(depId, new ArrayList<>());

				if (outgoing.size() > 1) {
					// SPLITTING: This train will split into multiple trains
					// Create units for each split using train ID + unit number
					String trainId = departure.getVehicleId() != null ? departure.getVehicleId().toString() : depId.toString();

					for (int i = 0; i < outgoing.size(); i++) {
						String unitId = trainId + "_" + (i + 1);
						units.add(unitId);
						log.debug("Created unit {} for splitting departure {}", unitId, depId);
					}
					log.debug("Splitting departure {} into {} trains: units {}", depId, outgoing.size(), units);
				} else {
					// REGULAR: Single train, create one unit
					String trainId = departure.getVehicleId() != null ? departure.getVehicleId().toString() : depId.toString();

					String unitId = trainId + "_1";
					units.add(unitId);
				}
			}

			// Every departure should have units at this point
			if (units.isEmpty()) {
				log.error("No units generated for departure {} - this should not happen", depId);
				continue;
			}

			// Validate units before storing
			if (hasEmptyUnitIds(units)) {
				log.error("Generated empty unit IDs for departure {}: {}", depId, units);
				continue;
			}

			// Store the units for this departure
			departureUnits.put(depId, units);

			RailsimUtils.setFormation(departure, units);
			createUnitVehicles(departure, units, vehicles);
		}

		// Third pass: handle splitting - assign individual units to chained departures
		for (TransitLine line : schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (Departure departure : route.getDepartures().values()) {

					List<Id<Departure>> outgoing = outgoingChains.getOrDefault(departure.getId(), new ArrayList<>());

					if (outgoing.size() > 1) {
						// This departure splits - assign individual units to each chained departure
						List<String> parentUnits = departureUnits.get(departure.getId());

						if (parentUnits != null && parentUnits.size() == outgoing.size()) {
							// Assign each unit to the corresponding chained departure
							for (int i = 0; i < outgoing.size(); i++) {
								Id<Departure> chainedId = outgoing.get(i);
								String unitId = parentUnits.get(i);

								// Find the chained departure and assign the single unit
								Departure chainedDeparture = allDepartures.get(chainedId);
								if (chainedDeparture != null) {
									// Validate unit ID
									if (unitId == null || unitId.trim().isEmpty()) {
										log.error("Empty unit ID for chained departure {} from parent {}", chainedId, departure.getId());
										continue;
									}

									List<String> chainedUnits = new ArrayList<>();
									chainedUnits.add(unitId);
									departureUnits.put(chainedId, chainedUnits);
									RailsimUtils.setFormation(chainedDeparture, chainedUnits);

									// Create individual vehicle for this unit
									createUnitVehicles(chainedDeparture, chainedUnits, vehicles);

									log.debug("Assigned unit {} to chained departure {}", unitId, chainedId);
								}
							}
						}
					}
				}
			}
		}

		log.info("Assigned train formations to {} departures", departureUnits.size());

		for (TransitLine line : schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (Departure departure : route.getDepartures().values()) {
					List<String> formation = RailsimUtils.getFormation(departure);
					if (formation.isEmpty() || hasEmptyUnitIds(formation)) {
						log.warn("Could not assign a formation to route {} departure {}. Formation: {}",
							route.getId(), departure.getId(), formation);
					}
				}
			}
		}
	}

	@Override
	public Integer call() throws Exception {

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new TransitScheduleReader(scenario).readFile(inputSchedule);
		new MatsimVehicleReader(scenario.getTransitVehicles()).readFile(inputVehicles);

		TransitSchedule transitSchedule = scenario.getTransitSchedule();

		setTrainFormations(transitSchedule, scenario.getTransitVehicles());

		new TransitScheduleWriter(transitSchedule).writeFile(outputSchedule);
		new MatsimVehicleWriter(scenario.getTransitVehicles()).writeFile(outputVehicles);

		return 0;
	}
}
