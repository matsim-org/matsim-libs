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
 * Reimplementation of {@link DriveDischargingHandler} which is aware of simulation partitions. For the time being we need both implementations, as this
 * handler assumes it is run without race conditions between event handling and the mobsim, because it is processed directly (ProcessingMode.DIRECT).
 */
@DistributedEventHandler(value = DistributedMode.PARTITION, processing = ProcessingMode.DIRECT)
public class DistributedDriveDischargingHandler implements LinkLeaveEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler,
	MobsimScopeEventHandler, NotifyVehiclePartitionTransfer, DSimComponentsMessageProcessor, QSimComponent {

	// Implementation notes:
	// The original implementation has some logic to apply the discharge in the next time step due to race conditions
	// between the simulatin and the events processing. DSim does not have this. Therefore, we handle the discharging in
	// the event handlers directly.
	//
	// Conceptually, this handler gets a reference to the ElectricFleet. We assume that it contains the electric vehicles present on this partition.
	// When a VehicleEntersTraffic event is received, the handler starts tracking that vehicle in its evDrives list. When the tracked vehicle leaves the partition,
	// the corresponding evDrive is transferred to that partition. When a tracked vehicle arrives on this partition, the corresponding
	// evDrive is implicitly received as a message.
	//
	// Also, this handler mutates the state of EVs in the ElectricFleet. In terms of ownership it would make more sense to put the discharging logic
	// into MobsimVehicles, as those are part of the simulated physics and capable of tracking their own discharging rate and so on. After all that
	// would adhere to the agent based design of matsim... However, I found things as they were, so we keep the tracking approach and concern ourself
	// with passing information around the distributed simulation.

	private final Network network;
	private final EventsManager em;
	private final ElectricFleet fleet;
	private final PartitionTransfer partitionTransfer;

	private final Map<Id<Vehicle>, EvDrive> evDrives = new HashMap<>();

	@Inject
	public DistributedDriveDischargingHandler(Network network, EventsManager em, ElectricFleet fleet, PartitionTransfer partitionTransfer) {
		this.network = network;
		this.em = em;
		this.fleet = fleet;
		this.partitionTransfer = partitionTransfer;
	}

	@Override
	public void handleEvent(LinkLeaveEvent e) {
		var evDrive = dischargeVehicle(e.getVehicleId(), e.getLinkId(), e.getTime());
		evDrive.moveOverNodeTime = e.getTime();
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent e) {

		if (fleet.hasVehicle(e.getVehicleId())) {
			evDrives.put(e.getVehicleId(), new EvDrive(e.getVehicleId()));
		}
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
			// obtain a reference to the vehicle in the fleet. This should be fine, as we are executed from within the SimProcess...
			ElectricVehicle ev = fleet.getVehicle(vehicleId);
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
		//private final ElectricVehicle ev;

		private double moveOverNodeTime = Double.NaN;

		EvDrive(Id<Vehicle> vehicleId) {
			this.vehicleId = vehicleId;
		}

		boolean isOnFirstLink() {
			return Double.isNaN(moveOverNodeTime);
		}
	}
}
