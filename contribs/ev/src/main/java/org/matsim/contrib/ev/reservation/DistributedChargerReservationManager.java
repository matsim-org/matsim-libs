package org.matsim.contrib.ev.reservation;

import com.google.inject.Inject;
import com.google.inject.Provider;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.MobsimMessageCollector;
import org.matsim.api.core.v01.network.NetworkPartition;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.core.mobsim.dsim.DSimComponentsMessageProcessor;
import org.matsim.core.mobsim.qsim.components.QSimComponent;
import org.matsim.vehicles.Vehicle;

import java.util.*;
import java.util.function.Consumer;

public class DistributedChargerReservationManager implements DSimComponentsMessageProcessor, QSimComponent {

	private final MobsimMessageCollector partitionTransfer;
	private final NetworkPartition networkPartition;
	private final Provider<ChargingInfrastructure> chargingInfrastructure;

	private final Map<Id<Charger>, Collection<Id<ChargerReservation>>> chargerToReservation = new HashMap<>();
	private final Map<Id<ChargerReservation>, ChargerReservation> reservations = new HashMap<>();
	private final Int2ObjectMap<Consumer<Optional<ChargerReservation>>> pendingReservationRequests = new Int2ObjectArrayMap<>();

	private int reservationIdCounter = 0;
	private int requestCounter = 0;

	@Inject
	public DistributedChargerReservationManager(MobsimMessageCollector partitionTransfer, NetworkPartition networkPartition, Provider<ChargingInfrastructure>
		chargingInfrastructure) {
		this.partitionTransfer = partitionTransfer;
		this.networkPartition = networkPartition;
		this.chargingInfrastructure = chargingInfrastructure;
	}

	public Optional<ChargerReservation> findReservation(Id<Charger> resource, Id<Vehicle> consumer, double now) {

		var charger = getLiveCharger(resource);
		if (!partitionTransfer.isLocal(charger.getLink().getId())) {
			throw new IllegalArgumentException("Resource: " + resource + " is not present on this partition. Reservations may only be queried for local reservations.");
		}

		var chargerReservations = chargerToReservation.get(resource);
		if (chargerReservations == null) return Optional.empty();

		for (var reservationId : chargerReservations) {
			var reservation = reservations.get(reservationId);
			if (reservation.consumer().equals(consumer) && now >= reservation.startTime() && now < reservation.endTime()) {
				return Optional.of(reservation);
			}
		}
		return Optional.empty();
	}

	public void removeReservation(Id<ChargerReservation> reservationId, Id<Charger> chargerId) {
		var reservation = reservations.remove(reservationId);
		if (reservation != null) {
			chargerToReservation.get(chargerId).remove(reservationId);
			// we don't care about empty maps.
		}
	}

	public boolean isAvailable(Id<Charger> chargerId, Id<Vehicle> vehicleId, double startTime, double endTime) {

		var charger = getLiveCharger(chargerId);

		if (!partitionTransfer.isLocal(charger.getLink().getId()))
			throw new IllegalStateException("Charger " + chargerId + " is not present on this partition. addLocalReservation may only be called for local chargers.");

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
		} else if (startTime <= info.startTime() && endTime > info.endTime()) {
			return true; // new range covers existing range
		} else {
			return false;
		}
	}

	public void addReservation(Id<Charger> resource, Id<Vehicle> consumer, double startTime, double endTime, Consumer<Optional<ChargerReservation>> callback) {

		var charger = getLiveCharger(resource);

		if (partitionTransfer.isLocal(charger.getLink().getId())) {
			callback.accept(addLocalReservation(resource, consumer, startTime, endTime));
		} else {
			requestRemoteReservation(resource, consumer, startTime, endTime, callback);
		}
	}

	/**
	 * Keep this synchronous version here, so that we don't have to rewrite the entire PlugginPriority code ase well.
	 */
	public Optional<ChargerReservation> addLocalReservation(Id<Charger> resource, Id<Vehicle> consumer, double startTime, double endTime) {

		if (isAvailable(resource, consumer, startTime, endTime)) {
			reservationIdCounter++;
			var charger = getLiveCharger(resource);
			var partitionIndex = partitionTransfer.getPartitionIndex(charger.getLink().getId());
			var id = Id.create(resource + "_" + partitionIndex + "_" + reservationIdCounter, ChargerReservation.class);
			var reservation = new ChargerReservation(id, resource, consumer, startTime, endTime);
			reservations.put(id, reservation);
			chargerToReservation.computeIfAbsent(resource, _ -> new ArrayList<>()).add(id);
			return Optional.of(reservation);
		}

		return Optional.empty();
	}

	private void requestRemoteReservation(Id<Charger> resource, Id<Vehicle> consumer, double startTime, double endTime, Consumer<Optional<ChargerReservation>> callback) {
		requestCounter++;
		var charger = getLiveCharger(resource);
		var request = new ReservationRequest(networkPartition.getIndex(), requestCounter, resource, consumer, startTime, endTime);
		pendingReservationRequests.put(requestCounter, callback);
		partitionTransfer.collect(request, charger.getLink().getId());
	}

	@Override
	public Map<Class<? extends Message>, MessageHandler> getMessageHandlers() {
		return Map.of(ReservationRequest.class, this::handleReservationRequest,
			ReservationResponse.class, this::handleReservationResponse);
	}

	private void handleReservationRequest(List<Message> messages, double now) {

		for (var m : messages) {
			var message = (ReservationRequest) m;

			var localReservation = addLocalReservation(
				message.chargerId(),
				message.vehicleId,
				message.startTime,
				message.endTime
			);
			// send back the reservation response
			partitionTransfer.collect(new ReservationResponse(message.requestId(), localReservation), message.requestPartition());
		}
	}

	private void handleReservationResponse(List<Message> messages, double now) {
		for (var m : messages) {
			ReservationResponse response = (ReservationResponse) m;
			var pendingReservation = pendingReservationRequests.remove(response.requestId);
			pendingReservation.accept(response.reservation());
		}
	}

	private Charger getLiveCharger(Id<Charger> chargerId) {
		return chargingInfrastructure.get().getChargers().get(chargerId);
	}

	public record ReservationRequest(
		int requestPartition,
		int requestId,
		Id<Charger> chargerId,
		Id<Vehicle> vehicleId,
		double startTime,
		double endTime) implements Message {}

	public record ReservationResponse(
		int requestId,
		Optional<ChargerReservation> reservation
	) implements Message {}

	/**
	 * Just like the orignal, but without actual references.
	 */
	public record ChargerReservation(
		Id<ChargerReservation> reservationId,
		Id<Charger> resource,
		Id<Vehicle> consumer,
		double startTime,
		double endTime
	) {}
}
