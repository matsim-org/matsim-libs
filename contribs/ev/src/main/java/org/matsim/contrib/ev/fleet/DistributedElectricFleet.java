package org.matsim.contrib.ev.fleet;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Message;
import org.matsim.contrib.ev.charging.ChargingPower;
import org.matsim.contrib.ev.discharging.AuxEnergyConsumption;
import org.matsim.contrib.ev.discharging.DriveEnergyConsumption;
import org.matsim.core.mobsim.dsim.DSimComponentsMessageProcessor;
import org.matsim.core.mobsim.dsim.DistributedMobsimVehicle;
import org.matsim.core.mobsim.dsim.NotifyVehiclePartitionTransfer;
import org.matsim.core.mobsim.qsim.components.QSimComponent;
import org.matsim.dsim.simulation.PartitionTransfer;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link ElectricFleet} for the DSim, which does not assume global-state. This implementation only grants access to its internal state
 * by {@link #getVehicle(Id)} and {@link #hasVehicle(Id)}. The ElectricFleet instantiates EVs lazily when it is queried for an EV. Callers are responsible to make sure
 * that the EV they want to use is in fact present on this partition.
 * <p>
 * This is not tested for analysis purposes yet.
 */
public class DistributedElectricFleet implements ElectricFleet, NotifyVehiclePartitionTransfer,
	DSimComponentsMessageProcessor, QSimComponent {

	// We assume that the scenario contains all the vehicles we could ever need. If we ever implement distributed scenarios, where each compute
	// node only has data for the vehicles located on that node, this has to be revised.
	private final Vehicles scenarioVehicles;
	private final DriveEnergyConsumption.Factory driveEnergyConsumptionFactory;
	private final AuxEnergyConsumption.Factory auxEnergyConsumptionFactory;
	private final ChargingPower.Factory chargingPowerFactory;

	private final PartitionTransfer partitionTransfer;

	private final Map<Id<Vehicle>, ElectricVehicle> electricVehicles = new HashMap<>();

	@Inject
	public DistributedElectricFleet(Vehicles scenarioVehicles, ChargingPower.Factory chargingPowerFactory, DriveEnergyConsumption.Factory driveEnergyConsumptionFactory, AuxEnergyConsumption.Factory auxEnergyConsumptionFactory, PartitionTransfer partitionTransfer) {
		this.scenarioVehicles = scenarioVehicles;
		this.driveEnergyConsumptionFactory = driveEnergyConsumptionFactory;
		this.auxEnergyConsumptionFactory = auxEnergyConsumptionFactory;
		this.chargingPowerFactory = chargingPowerFactory;
		this.partitionTransfer = partitionTransfer;
	}

	// we do this lazy initialization hoop in combination with hasVehicle, as some modules call this fleet from within a VehicleEntersTrafficEventHandler.
	// Since we don't know the order in which handlers are called, we cannot use that hook to lazily create EVs. Hence, we need to do it here.
	@Override
	public ElectricVehicle getVehicle(Id<Vehicle> vehicleId) {
		var result = electricVehicles.get(vehicleId);

		if (result == null) {
			if (isElectricVehicle(vehicleId)) {
				var specification = new ElectricVehicleSpecificationDefaultImpl(scenarioVehicles.getVehicles().get(vehicleId));
				result = ElectricFleetUtils.create(specification, driveEnergyConsumptionFactory, auxEnergyConsumptionFactory, chargingPowerFactory);
				electricVehicles.put(vehicleId, result);
			} else {
				throw new IllegalArgumentException("Vehicle " + vehicleId + " is not an EV. Call hasVehicle() first.");
			}
		}

		return result;
	}

	@Override
	public boolean hasVehicle(Id<Vehicle> vehicleId) {
		// yes, we already have that vehicle
		if (electricVehicles.containsKey(vehicleId)) return true;

		// does the scenario have an electric vehicle with that id?
		return isElectricVehicle(vehicleId);
	}

	@Override
	public void onVehicleLeavesPartition(DistributedMobsimVehicle vehicle, int toPartition) {
		if (electricVehicles.containsKey(vehicle.getId())) {
			var ev = electricVehicles.remove(vehicle.getId());
			partitionTransfer.collect(new ElectricVehicleMessage(ev.getId(), ev.getBattery().getCharge()), toPartition);
		}
	}

	@Override
	public void onVehicleEntersPartition(DistributedMobsimVehicle vehicle) {
		// nothing to do.
	}

	@Override
	public Map<Class<? extends Message>, MessageHandler> getMessageHandlers() {
		return Map.of(ElectricVehicleMessage.class, this::handleMessages);
	}

	private void handleMessages(List<Message> messages, double now) {
		for (var m : messages) {
			// SAFETY: we have only registered ElectricVehicleMessage.class as the message type to receive.
			ElectricVehicleMessage evMessage = (ElectricVehicleMessage) m;
			var id = evMessage.vehicleId;

			if (electricVehicles.containsKey(id)) {
				throw new IllegalStateException("Received ElectricVehicleMessage for vehicle " + id + ". We already have" +
					" this vehicle in the fleet. This indicates, that some component has queried the fleet for this vehicle, even though no corresponding" +
					" MobsimVehicle is present on this partition. This is most probably a programming bug.");
			}

			var vehicle = scenarioVehicles.getVehicles().get(id);
			var spec = new ElectricVehicleSpecificationDefaultImpl(vehicle);
			var ev = ElectricFleetUtils.create(spec, driveEnergyConsumptionFactory, auxEnergyConsumptionFactory, chargingPowerFactory);
			ev.getBattery().setCharge(evMessage.charge);

			electricVehicles.put(ev.getId(), ev);
		}
	}

	private boolean isElectricVehicle(Id<Vehicle> vehicleId) {
		var vehicle = scenarioVehicles.getVehicles().get(vehicleId);
		return ElectricFleetUtils.isElectricVehicleType(vehicle.getType());
	}

	/**
	 * The state of the ev actually is the specification and the battery, which contains the charging state.
	 */
	public record ElectricVehicleMessage(Id<Vehicle> vehicleId, double charge) implements Message {}
}
