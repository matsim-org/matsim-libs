package org.matsim.contrib.ev.reservation;

import com.google.inject.Inject;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkPartition;
import org.matsim.contrib.common.util.reservation.ReservationManager;
import org.matsim.contrib.ev.fleet.ElectricFleet;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.core.mobsim.dsim.DSimComponentsMessageProcessor;
import org.matsim.core.mobsim.qsim.components.QSimComponent;
import org.matsim.dsim.simulation.PartitionTransfer;
import org.matsim.vehicles.Vehicle;

import java.util.*;

public class DistributedChargerReservationManager implements ReservationManager<Charger, ElectricVehicle>, DSimComponentsMessageProcessor, QSimComponent {

	private final PartitionTransfer partitionTransfer;
	private final NetworkPartition networkPartition;
	private final ChargingInfrastructure chargingInfrastructure;
	private final ElectricFleet electricFleet;

	private final Map<Id<Charger>, Collection<Id<Reservation>>> chargerToReservation = new HashMap<>();
	private final Map<Id<Reservation>, ChargerReservation> reservations = new HashMap<>();
	//private final Map<Id<ElectricVehicle>, ReservationInfo<ElectricVehicle, Charger>> vehicleToReservation = new HashMap<>();
	private final Int2ObjectMap<ReservationInfo<Charger, ElectricVehicle>> pendingReservationRequests = new Int2ObjectArrayMap<>();
	private final Map<Id<Reservation>, ReservationInfo<Charger, ElectricVehicle>> receivedReservationRequests = new HashMap<>();

	private int reservationIdCounter = 0;
	private int requestCounter = 0;

	@Inject
	public DistributedChargerReservationManager(PartitionTransfer partitionTransfer, NetworkPartition networkPartition, ChargingInfrastructure chargingInfrastructure, ElectricFleet electricFleet) {
		this.partitionTransfer = partitionTransfer;
		this.networkPartition = networkPartition;
		this.chargingInfrastructure = chargingInfrastructure;
		this.electricFleet = electricFleet;
	}

	@Override
	public boolean isAvailable(Charger resource, ElectricVehicle consumer, double startTime, double endTime) {
		return isAvailable(resource.getId(), resource.getLink().getId(), consumer.getId(), startTime, endTime);
	}

	private boolean isAvailable(Id<Charger> chargerId, Id<Link> linkId, Id<Vehicle> vehicleId, double startTime, double endTime) {

		if (!partitionTransfer.isLocal(linkId))
			throw new IllegalStateException("We don't know whether remove chargers are available or not. Just call addReservation and check the result.");

		var charger = getLiveCharger(chargerId);
		var overlappingReservations = new HashSet<>();
		var chargerReservations = chargerToReservation.computeIfAbsent(chargerId, _ -> new ArrayList<>());
		for (var reservationId : chargerReservations) {
			var reservation = reservations.get(reservationId);
			if (!reservation.consumer().equals(vehicleId) && isOverlapping(reservation, startTime, endTime)) {
				overlappingReservations.add(reservationId);
			}
		}

		return overlappingReservations.size() < charger.getPlugCount();
	}

	private boolean isOverlapping(ChargerReservation info, double startTime, double endTime) {
		if (startTime >= info.startTime() && startTime < info.endTime()) {
			return true; // start time within existing range
		} else if (endTime > info.startTime() && endTime < info.endTime()) {
			return true; // end time within existing range
		} else return startTime <= info.startTime() && endTime > info.endTime(); // new range covers existing range
	}

	@Override
	public Optional<ReservationInfo<Charger, ElectricVehicle>> addReservation(Charger resource, ElectricVehicle consumer, double startTime, double endTime) {

		if (partitionTransfer.isLocal(resource.getLink().getId())) {
			return addLocalReservation(resource, consumer, startTime, endTime);
		} else {
			return requestRemoteReservation(resource, consumer, startTime, endTime);
		}
	}

	private Optional<ReservationInfo<Charger, ElectricVehicle>> addLocalReservation(Charger resource, ElectricVehicle consumer, double startTime, double endTime) {

		if (isAvailable(resource.getId(), resource.getLink().getId(), consumer.getId(), startTime, endTime)) {
			reservationIdCounter++;
			var partitionIndex = partitionTransfer.getPartitionIndex(resource.getLink().getId());
			var id = Id.create(resource.getId() + "_" + partitionIndex + "_" + reservationIdCounter, Reservation.class);
			var externalReservation = new ReservationInfo<>(id, resource, consumer, startTime, endTime, ReservationStatus.CONFIRMED);
			// internally, we only store ids, because we cannot hold references of moving objects, as they might leave the partition.
			var internalReservation = ChargerReservation.fromExternalReservation(externalReservation);
			reservations.put(id, internalReservation);
			chargerToReservation.computeIfAbsent(resource.getId(), k -> new ArrayList<>()).add(id);
			return Optional.of(externalReservation);
		}

		return Optional.empty();
	}

	private Optional<ReservationInfo<Charger, ElectricVehicle>> requestRemoteReservation(Charger resource, ElectricVehicle consumer, double startTime, double endTime) {

		requestCounter++;
		var request = new ReservationRequest(networkPartition.getIndex(), requestCounter, resource.getId(), consumer.getId(), startTime, endTime);
		var info = new ReservationInfo<>(
			Id.create(resource.getId() + "_pending_" + requestCounter, Reservation.class),
			resource,
			consumer,
			startTime,
			endTime,
			ReservationStatus.PENDING
		);
		pendingReservationRequests.put(requestCounter, info);
		partitionTransfer.collect(request, resource.getLink().getId());
		return Optional.of(info);
	}

	@Override
	public boolean removeReservation(Id<?> resourceId, Id<Reservation> reservationId) {
		var reservation = reservations.remove(reservationId);
		if (reservation != null) {
			chargerToReservation.get(resourceId).remove(reservationId);
			return true;
		}
		return false;
	}

	@Override
	public boolean updateReservation(Id<?> resourceId, Id<Reservation> reservationId, double newStartTime, double newEndTime) {
		throw new UnsupportedOperationException("Updating reservations is not supported in this implementation.");
	}

	@Override
	public Optional<ReservationInfo<Charger, ElectricVehicle>> findReservation(Charger resource, ElectricVehicle consumer, double now) {
		var chargerReservations = chargerToReservation.get(resource.getId());
		if (chargerReservations == null) return Optional.empty();
		for (var reservationId : chargerReservations) {
			var reservation = reservations.get(reservationId);
			if (reservation.consumer().equals(consumer.getId()) && now >= reservation.startTime() && now < reservation.endTime()) {
				return reservation.toExternalReservation(chargingInfrastructure, electricFleet);
			}
		}
		return Optional.empty();
	}

	@Override
	public Optional<ReservationInfo<Charger, ElectricVehicle>> findReservation(Id<?> resourceId, Id<Reservation> reservationId) {
		var reservation = reservations.get(reservationId);
		if (reservation == null) return Optional.empty();
		return reservation.toExternalReservation(chargingInfrastructure, electricFleet);
	}


	@Override
	public Map<Class<? extends Message>, MessageHandler> getMessageHandlers() {
		return Map.of(ReservationRequest.class, this::handleReservationRequest,
			ReservationResponse.class, this::handleReservationResponse);
	}

	public Optional<ReservationInfo<Charger, ElectricVehicle>> queryPendingReservation(Id<Reservation> id) {
		var reservation = receivedReservationRequests.get(id);
		return Optional.ofNullable(reservation);
	}

	private void handleReservationRequest(List<Message> messages, double now) {

		for (var m : messages) {
			var message = (ReservationRequest) m;

			var localReservation = addLocalReservation(
				getLiveCharger(message.chargerId()),
				electricFleet.getVehicle(message.vehicleId),
				message.startTime,
				message.endTime
			);
			// send back the reservation response
			if (localReservation.isPresent()) {
				var reservationId = localReservation.get().reservationId();
				partitionTransfer.collect(new ReservationResponse(message.requestId(), ReservationStatus.CONFIRMED, reservationId), message.requestPartition());
			} else {
				partitionTransfer.collect(new ReservationResponse(message.requestId(), ReservationStatus.REJECTED, null), message.requestPartition());
			}
		}
	}

	private void handleReservationResponse(List<Message> messages, double now) {
		for (var m : messages) {
			ReservationResponse response = (ReservationResponse) m;
			var pendingReservation = pendingReservationRequests.remove(response.requestId);
			var reservation = new ReservationInfo<>(
				response.reservationId,
				pendingReservation.resource(),
				pendingReservation.consumer(),
				pendingReservation.startTime(),
				pendingReservation.endTime(),
				response.status()
			);
			receivedReservationRequests.put(pendingReservation.reservationId(), reservation);
		}
	}

	private Charger getLiveCharger(Id<Charger> chargerId) {
		return chargingInfrastructure.getChargers().get(chargerId);
	}

	public record ReservationRequest(
		int requestPartition,
		int requestId,
		Id<Charger> chargerId,
		Id<Vehicle> vehicleId,
		double startTime,
		double endTime) implements Message {
	}

	public record ReservationResponse(
		int requestId,
		ReservationStatus status,
		Id<Reservation> reservationId
	) implements Message {
	}

	/**
	 * Just like the orignal, but without actual references.
	 */
	public record ChargerReservation(
		Id<Reservation> reservationId,
		Id<Charger> resource,
		Id<Vehicle> consumer,
		double startTime,
		double endTime,
		ReservationStatus status
	) {

		static ChargerReservation fromExternalReservation(ReservationInfo<Charger, ElectricVehicle> info) {
			return new ChargerReservation(
				info.reservationId(),
				info.resource().getId(),
				info.consumer().getId(),
				info.startTime(),
				info.endTime(),
				info.status()
			);
		}

		Optional<ReservationInfo<Charger, ElectricVehicle>> toExternalReservation(ChargingInfrastructure chargingInfrastructure, ElectricFleet electricFleet) {
			var charger = chargingInfrastructure.getChargers().get(resource);
			var vehicle = electricFleet.getVehicle(consumer);
			if (charger == null || vehicle == null) return Optional.empty();
			return Optional.of(new ReservationInfo<>(reservationId, charger, vehicle, startTime, endTime, status));
		}
	}

}
