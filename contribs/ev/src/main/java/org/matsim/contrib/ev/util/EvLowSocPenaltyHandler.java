package org.matsim.contrib.ev.util;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.PersonScoreEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.ev.EvUnits;
import org.matsim.contrib.ev.charging.ChargingEndEvent;
import org.matsim.contrib.ev.charging.ChargingEndEventHandler;
import org.matsim.contrib.ev.charging.ChargingStartEvent;
import org.matsim.contrib.ev.charging.ChargingStartEventHandler;
import org.matsim.contrib.ev.discharging.DrivingEnergyConsumptionEvent;
import org.matsim.contrib.ev.discharging.DrivingEnergyConsumptionEventHandler;
import org.matsim.contrib.ev.discharging.MissingEnergyEvent;
import org.matsim.contrib.ev.discharging.MissingEnergyEventHandler;
import org.matsim.contrib.ev.fleet.ElectricFleetUtils;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecification;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Event handler that tracks electric vehicle (EV) behavior during MATSim simulation and applies penalties
 * to agents for reaching critically low or zero state of charge (SOC).
 */
public class EvLowSocPenaltyHandler implements PersonEntersVehicleEventHandler,
        PersonLeavesVehicleEventHandler, ChargingEndEventHandler, AfterMobsimListener,
        DrivingEnergyConsumptionEventHandler, MissingEnergyEventHandler,
        ChargingStartEventHandler, ActivityStartEventHandler {

    @Inject private EventsManager eventsManager;
    @Inject private Vehicles vehicles;
    @Inject private Network network;
    @Inject private Config config;
    @Inject private ChargingInfrastructureSpecification chargingInfrastructure;

    // --- Simulation and penalty configuration ---
    private static final double EMPTY_EV_SCORE_DEDUCTION = -4.5;
    private static final String EMPTY_EV_PENALTY_EVENT_KIND = "MissingEnergy";
    private static final double FAILED_CHARGING_THRESHOLD = 0.05;
    private static final double[] SOC_THRESHOLDS = {0.15, 0.20, 0.25, 0.30, 0.35, 0.40};

    // --- Runtime state tracking ---
    private final Map<Id<Vehicle>, Id<Person>> evDrivers = new HashMap<>();
    private final Map<String, EVStats> lowEnergyStats = new HashMap<>();
    private final Map<String, EVStats> missingEnergyStats = new HashMap<>();
    private final Map<Id<Vehicle>, Integer> lowEnergyCount = new HashMap<>();
    private final Map<Id<Vehicle>, Integer> missingEnergyCount = new HashMap<>();
    private final Set<Id<Vehicle>> penalisedEVs = new HashSet<>();
    private final Set<Id<Charger>> failedChargers = new HashSet<>();

    // --- Charging and SOC tracking ---
    private final Map<Id<Vehicle>, Double> energyAtChargeStart = new HashMap<>();
    private final Map<Id<Vehicle>, Double> socAtChargeStart = new HashMap<>();
    private final List<ChargingStats> chargingStatsList = new ArrayList<>();

    // --- Completion & population mapping ---
    private final Map<Id<Vehicle>, Boolean> freightCompletion = new HashMap<>();
    private final Map<Id<Person>, Id<Vehicle>> personToVehicle = new HashMap<>();
    private final Map<Id<Vehicle>, FinalSOCStats> finalSOCMap = new HashMap<>();



    public EvLowSocPenaltyHandler() {
    }

    /**
     * Handle event when a person enters a vehicle.
     * If the vehicle is electric, initializes tracking for this vehicle/person pair.
     */
    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        Vehicle vehicle = vehicles.getVehicles().get(event.getVehicleId());
        if (isElectric(vehicle)) {
            evDrivers.put(event.getVehicleId(), event.getPersonId());
            lowEnergyCount.putIfAbsent(event.getVehicleId(), 0);
            missingEnergyCount.putIfAbsent(event.getVehicleId(), 0);
            personToVehicle.putIfAbsent(event.getPersonId(), event.getVehicleId());
            freightCompletion.put(event.getVehicleId(), false);
        }
    }

    /**
     * Handle event when a person leaves a vehicle.
     * Remove the association for this vehicle.
     */
    @Override
    public void handleEvent(PersonLeavesVehicleEvent event) {
        evDrivers.remove(event.getVehicleId());
    }

    /**
     * Record energy and SOC at the start of charging for later comparison.
     */
    @Override
    public void handleEvent(ChargingStartEvent event) {
        energyAtChargeStart.put(event.getVehicleId(), EvUnits.J_to_kWh(event.getCharge()));
        double capacity = getEnergyCapacity(event.getVehicleId());
        socAtChargeStart.put(event.getVehicleId(), EvUnits.J_to_kWh(event.getCharge()) / capacity);
    }


    /**
     * Handle charging end event.
     * Checks if a charging event failed (did not increase SOC meaningfully), and records charging statistics.
     */
    @Override
    public void handleEvent(ChargingEndEvent event) {
        double startEnergy = energyAtChargeStart.getOrDefault(event.getVehicleId(), 0.0);
        double capacity = getEnergyCapacity(event.getVehicleId());
        double gainedEnergy = EvUnits.J_to_kWh(event.getCharge()) - startEnergy;
        if (gainedEnergy < FAILED_CHARGING_THRESHOLD * capacity) { // Could be used to penalize failed charging events
            failedChargers.add(event.getChargerId());
        }
        Coord coord = getChargerCoord(event.getChargerId());
        chargingStatsList.add(new ChargingStats(coord, event.getVehicleId(), event.getTime(),
                socAtChargeStart.get(event.getVehicleId()), EvUnits.J_to_kWh(event.getCharge()) / capacity, capacity));
    }

    /**
     * Handle missing energy (empty EV) event.
     * Penalizes once per iteration, logs event, and issues scoring penalty.
     */
    @Override
    public void handleEvent(MissingEnergyEvent event) {
        if (!penalisedEVs.contains(event.getVehicleId()) && evDrivers.containsKey(event.getVehicleId())) {
            penalisedEVs.add(event.getVehicleId());
            int count = missingEnergyCount.merge(event.getVehicleId(), 1, Integer::sum);
            Coord coord = network.getLinks().get(event.getLinkId()).getCoord();
            String eventKey = event.getVehicleId() + "_" + count;
            missingEnergyStats.put(eventKey, new EVStats(coord, event.getVehicleId(), evDrivers.get(event.getVehicleId()), event.getTime()));
            eventsManager.processEvent(new PersonScoreEvent(event.getTime(), evDrivers.get(event.getVehicleId()), EMPTY_EV_SCORE_DEDUCTION, EMPTY_EV_PENALTY_EVENT_KIND));
        }
    }

    /**
     * Main event handler for energy consumption while driving.
     * For each SOC threshold crossed, logs a low-energy event (twice for 15/20/25%, once for 30/35/40%).
     * Records final SOC if this is the completion of a freight tour.
     */
    @Override
    public void handleEvent(DrivingEnergyConsumptionEvent event) {
        double soc = EvUnits.J_to_kWh(event.getEndCharge()) / getEnergyCapacity(event.getVehicleId());
        Id<Vehicle> vehicleId = event.getVehicleId();
        Coord coord = network.getLinks().get(event.getLinkId()).getCoord();

        // Store final SOC at tour completion
        if (freightCompletion.getOrDefault(vehicleId, false)) {
            finalSOCMap.put(vehicleId, new FinalSOCStats(event.getTime(), soc, coord));
        }

        // Log low energy events for all thresholds crossed (with double weight for 15/20/25)
        for (double threshold : SOC_THRESHOLDS) {
            String baseKey = vehicleId + "_" + (int)(threshold * 100);

            if (soc < threshold) {
                if (threshold <= 0.25) {
                    // Create two events for thresholds 15%, 20%, 25% = Double weight
                    for (int i = 1; i <= 2; i++) {
                        String key = baseKey + "_" + i;
                        if (!lowEnergyStats.containsKey(key)) {
                            lowEnergyStats.put(key, new EVStats(coord, vehicleId, evDrivers.get(vehicleId), event.getTime()));
                        }
                    }
                } else {
                    // Single event for 30%, 35%, 40%
                    if (!lowEnergyStats.containsKey(baseKey)) {
                        lowEnergyStats.put(baseKey, new EVStats(coord, vehicleId, evDrivers.get(vehicleId), event.getTime()));
                    }
                }
            }
        }
    }

    @Override
    public void handleEvent(ActivityStartEvent event) {
        if (event.getActType().equals("freight")) {
            this.freightCompletion.put(personToVehicle.get(event.getPersonId()),true);
        }
    }

    /**
     * Called at the end of each mobsim iteration. Resets runtime state.
     * Clears all event tracking, but keeps statistics for the last iteration if needed.
     */
    @Override
    public void notifyAfterMobsim(AfterMobsimEvent event) {
        evDrivers.clear();
        penalisedEVs.clear();
        missingEnergyCount.clear();
        lowEnergyCount.clear();
        freightCompletion.clear();
        personToVehicle.clear();
        socAtChargeStart.clear();
        energyAtChargeStart.clear();

        if (!event.isLastIteration()) {
            chargingStatsList.clear();
            finalSOCMap.clear();
            missingEnergyStats.clear();
            lowEnergyStats.clear();
        }
    }

    // --- Utility methods ---

    /** Helper: Returns true if vehicle is electric according to HBEFA technology string. */
    private boolean isElectric(Vehicle vehicle) {
        return ElectricFleetUtils.EV_ENGINE_HBEFA_TECHNOLOGY.equals(
                VehicleUtils.getHbefaTechnology(vehicle.getType().getEngineInformation()));
    }

    /** Helper: Returns the battery capacity in kWh for a vehicle. */
    private double getEnergyCapacity(Id<Vehicle> vehicleId) {
        return VehicleUtils.getEnergyCapacity(
                vehicles.getVehicles().get(vehicleId).getType().getEngineInformation());
    }

    /** Helper: Returns the coordinate of the charger. */
    private Coord getChargerCoord(Id<Charger> chargerId) {
        Id<Link> linkId = chargingInfrastructure.getChargerSpecifications().get(chargerId).getLinkId();
        return network.getLinks().get(linkId).getCoord();
    }
    // --- Data output methods ---
    /**
     * This can be called in e.g. notifyAfterMobsim, if files for each iteration are requested
     */
    public void getDataForEachIteration(Integer iteration) {
        String base = config.controller().getOutputDirectory() + "/ITERS/it." + iteration + "/";
        writeToTXT(
                new File(base + "lowEnergyEvents.txt"),
                new File(base + "missingEnergyEvents.txt"),
                new File(base + "EnergyEvents_IDs.txt")
        );
    }

    public void getDataFromLastIteration() throws IOException {
        String base = config.controller().getOutputDirectory() + "/";
        writeToTXTCharging(new File(base + "detailedChargingStats.txt"));
        writeToTXT(
                new File(base + "lowEnergyEvents.txt"),
                new File(base + "missingEnergyEvents.txt"),
                new File(base + "EnergyEvents_IDs.txt")
        );
        writeToTXTFinalSOC(new File(base + "finalSOC.txt"));
    }

    private void writeToTXT(File fileLEE, File fileMEE, File fileUnique) {
        try (
                BufferedWriter bwLEE = new BufferedWriter(new FileWriter(fileLEE));
                BufferedWriter bwMEE = new BufferedWriter(new FileWriter(fileMEE))
        ) {
            bwLEE.write("Time\tvehicleID\tpersonID\txCoord\tyCoord\tlowEnergy");
            bwLEE.newLine();
            bwMEE.write("Time\tvehicleID\tpersonID\txCoord\tyCoord\tmissingEnergy");
            bwMEE.newLine();

            Set<String> uniquePersonIDs = new HashSet<>();

            // Write low energy stats
            for (Map.Entry<String, EVStats> entry : lowEnergyStats.entrySet()) {
                EVStats stats = entry.getValue();
                String rawVehicleId = extractBaseVehicleId(entry.getKey())+"_1";

                if (missingEnergyStats.containsKey(rawVehicleId)) {
                    uniquePersonIDs.add(stats.personId.toString());
                    bwLEE.write(stats.time + "\t" + stats.vehicleId + "\t" + stats.personId + "\t" +
                            stats.coord.getX() + "\t" + stats.coord.getY() + "\t1");
                    bwLEE.newLine();
                }
            }

            // Write missing energy stats
            for (Map.Entry<String, EVStats> entry : missingEnergyStats.entrySet()) {
                EVStats stats = entry.getValue();
                uniquePersonIDs.add(stats.personId.toString());
                bwMEE.write(stats.time + "\t" + stats.vehicleId + "\t" + stats.personId + "\t" +
                        stats.coord.getX() + "\t" + stats.coord.getY() + "\t1");
                bwMEE.newLine();
            }

            // Write unique IDs
            try (BufferedWriter bfUnique = new BufferedWriter(new FileWriter(fileUnique))) {
                bfUnique.write("personID");
                bfUnique.newLine();
                for (String id : uniquePersonIDs) {
                    bfUnique.write(id);
                    bfUnique.newLine();
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Error writing energy event logs", e);
        }
    }

    private void writeToTXTCharging(File file) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write("time\tvehicleID\tpersonID\txCoord\tyCoord\tSOCatChargeStart\tSOCatChargeEnd\tbatteryCapacity");
            bw.newLine();
            for (ChargingStats stat : chargingStatsList) {
                // If ChargingStats does not have personId, remove that field here
                String personIdStr = (stat.personId != null) ? stat.personId.toString() : "";
                bw.write(stat.time + "\t" +
                        stat.vehicleId + "\t" +
                        personIdStr + "\t" +
                        stat.coord.getX() + "\t" +
                        stat.coord.getY() + "\t" +
                        stat.initialSOC + "\t" +
                        stat.finalSOC + "\t" +
                        stat.batteryCapacity);
                bw.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Error writing charging stats", e);
        }
    }

    private void writeToTXTFinalSOC(File fileFinalSOC) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileFinalSOC))) {
            bw.write("vehicleID\ttime\tfinalSOC\txCoord\tyCoord");
            bw.newLine();
            for (Map.Entry<Id<Vehicle>, FinalSOCStats> entry : finalSOCMap.entrySet()) {
                Id<Vehicle> vehicleId = entry.getKey();
                FinalSOCStats stat = entry.getValue();
                bw.write(vehicleId + "\t" +
                        stat.time + "\t" +
                        stat.finalSoc + "\t" +
                        stat.coord.getX() + "\t" +
                        stat.coord.getY());
                bw.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Error writing final SOC stats", e);
        }
    }

    private String extractBaseVehicleId(String fullKey) {
        return fullKey.replaceAll("_(15|20|25|30|35|40)(_\\d)?$", "");
    }
}
class FinalSOCStats {

	public Coord coord;
	public Double time;
	public Double finalSoc;

	public FinalSOCStats(double time, double finalSOC , Coord coord){
		this.time = time;
		this.finalSoc = finalSOC;
		this.coord = coord;

	}

}

class EVStats {
	public Coord coord;
	public Id<Vehicle> vehicleId;
	public Double time;
	public Id<Person> personId;
	public EVStats(Coord coord, Id<Vehicle> vehicleId, Id<Person> personId, double time){
		this.coord = coord;
		this.vehicleId = vehicleId;
		this.personId = personId;
		this.time = time;

	}

}

class ChargingStats {
	public Coord coord;
	public Id<Vehicle> vehicleId;
	public Double time;
	Id<Person> personId;
	public Double initialSOC;
	public Double finalSOC;
	public Double batteryCapacity;
	public ChargingStats(Coord coord, Id<Vehicle> vehicleId, double time,double initialSOC,
						 double finalSOC, double batteryCapacity){
		this.coord = coord;
		this.vehicleId = vehicleId;
		this.personId = personId;
		this.time = time;
		this.initialSOC = initialSOC;
		this.finalSOC = finalSOC;
		this.batteryCapacity = batteryCapacity;

	}
}
