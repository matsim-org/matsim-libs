package org.matsim.contrib.ev.discharging;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.ev.fleet.ElectricFleet;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.MobsimScopeEventHandler;
import org.matsim.core.mobsim.dsim.DSimComponentsMessageProcessor;
import org.matsim.core.mobsim.dsim.DistributedMobsimVehicle;
import org.matsim.core.mobsim.dsim.NotifyVehiclePartitionTransfer;
import org.matsim.core.mobsim.qsim.components.QSimComponent;
import org.matsim.dsim.simulation.PartitionTransfer;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reimplementation of {@link DriveDischargingHandler} until i have figured out what is going on.
 * <p>
 * The original implementation has some logic to apply the discharge in the next time step due to reace conditions
 * between the simulatin and the events processing. DSim does not have this. Therefore we handle the discharging in
 * the event handlers directly.
 */
// I guess this could also be processing mode task?
@DistributedEventHandler(value = DistributedMode.PARTITION, processing = ProcessingMode.DIRECT)
public class DistributedDriveDischargingHandler implements LinkLeaveEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler,
	MobsimScopeEventHandler, NotifyVehiclePartitionTransfer, DSimComponentsMessageProcessor, QSimComponent {

	private final Network network;
	private final EventsManager em;
	private final Map<Id<Vehicle>, ? extends ElectricVehicle> eVehicles;
	private final PartitionTransfer partitionTransfer;

	private final Map<Id<Vehicle>, EvDrive> evDrives = new HashMap<>();

	@Inject
	public DistributedDriveDischargingHandler(Network network, EventsManager em, ElectricFleet fleet, PartitionTransfer partitionTransfer) {
		this.network = network;
		this.em = em;
		this.eVehicles = fleet.getElectricVehicles();
		this.partitionTransfer = partitionTransfer;
	}

	@Override
	public void handleEvent(LinkLeaveEvent e) {
		var evDrive = dischargeVehicle(e.getVehicleId(), e.getLinkId(), e.getTime());
		evDrive.moveOverNodeTime = e.getTime();
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent e) {
		var ev = eVehicles.get(e.getVehicleId());
		var drive = new EvDrive(e.getVehicleId(), ev);
		evDrives.put(e.getVehicleId(), drive);
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent e) {
		var evDrive = dischargeVehicle(e.getVehicleId(), e.getLinkId(), e.getTime());
		evDrives.remove(evDrive.vehicleId);
	}

	@Override
	public void onVehicleLeavesPartition(DistributedMobsimVehicle vehicle, int toPartition) {
		if (evDrives.containsKey(vehicle.getId())) {
			var evDrive = evDrives.remove(vehicle.getId());
			partitionTransfer.collect(evDrive, toPartition);
		}
	}

	@Override
	public void onVehicleEntersPartition(DistributedMobsimVehicle vehicle) {
		// nothing to do here.
	}

	@Override
	public Map<Class<? extends Message>, MessageHandler> getMessageHandlers() {
		return Map.of(EvDrive.class, this::processEvDriveMessage);
	}

	private void processEvDriveMessage(List<Message> messages, double now) {
		for (var m : messages) {
			var msg = (EvDrive) m;
			evDrives.put(msg.vehicleId, msg);
		}
	}

	private EvDrive dischargeVehicle(Id<Vehicle> vehicleId, Id<Link> linkId, double now) {
		EvDrive evDrive = evDrives.get(vehicleId);
		if (!evDrive.isOnFirstLink()) {// skip the first link
			Link link = network.getLinks().get(linkId);
			double tt = now - evDrive.moveOverNodeTime;
			ElectricVehicle ev = evDrive.ev;
			double energy = ev.getDriveEnergyConsumption().calcEnergyConsumption(link, tt, now - tt) + ev.getAuxEnergyConsumption()
				.calcEnergyConsumption(now - tt, tt, linkId);
			//Energy consumption may be negative on links with negative slope
			//===============================================================
			//Added to handle cases when the negative energy discharged would surpass the battery capacity
			//The new resulting energy should mean that the battery stays at the battery capacity when the energy is negative
			if (ev.getBattery().getCharge() - energy > ev.getBattery().getCapacity()) {
				energy = ev.getBattery().getCharge() - ev.getBattery().getCapacity();
			}
			//===================================================================
			ev.getBattery()
				.dischargeEnergy(energy,
					missingEnergy -> em.processEvent(new MissingEnergyEvent(now, ev.getId(), link.getId(), missingEnergy)));
			em.processEvent(new DrivingEnergyConsumptionEvent(now, vehicleId, linkId, energy, ev.getBattery().getCharge()));
		}
		return evDrive;
	}

	/**
	 * Not sure yet, whether it is ok to send ElecticVehicle as message. Let's try.
	 */
	public static class EvDrive implements Message {

		private final Id<Vehicle> vehicleId;
		private final ElectricVehicle ev;

		private double moveOverNodeTime = Double.NaN;

		EvDrive(Id<Vehicle> vehicleId, ElectricVehicle ev) {
			this.vehicleId = vehicleId;
			this.ev = ev;
		}

		boolean isOnFirstLink() {
			return Double.isNaN(moveOverNodeTime);
		}
	}
}
