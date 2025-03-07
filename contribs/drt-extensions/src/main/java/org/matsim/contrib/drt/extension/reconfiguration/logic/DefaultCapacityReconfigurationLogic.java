package org.matsim.contrib.drt.extension.reconfiguration.logic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEventHandler;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.load.DvrpLoad;
import org.matsim.contrib.dvrp.load.DvrpLoadType;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEventHandler;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.collections.QuadTree;

/**
 * This class proposes a first implementation of the
 * {@link CapacityReconfigurationLogic}
 * The logic behind this implementation is as follows:
 * - The capacities compatible with requests are tracked in the previous
 * iteration. Building A list containing the number of requests per capacity per
 * time slot.
 * - The Capacity changes of the fleet are planned such as the distribution of
 * vehicles with each capacity during the day matches the distribution of
 * capacities required by requests.
 * - If configured to do so, the logic starts by overriding the default vehicle
 * capacities at the beginning of the day before inserting capacity changes
 * - Moreover, to select the capacity change locations, we use the
 * {@link DefaultCapacityReconfigurationLogic.CapacityChangeLinkSelection}
 * parameter to choose either a random selection or the location closest to the
 * vehicle's initial position
 *
 * @author Tarek Chouaki (tkchouaki), IRT SystemX
 */
public class DefaultCapacityReconfigurationLogic
		implements CapacityReconfigurationLogic, DrtRequestSubmittedEventHandler, PassengerRequestRejectedEventHandler {

	public enum CapacityChangeLinkSelection {
		RANDOM, ClosestToVehicleStartLocation
	}

	private final FleetSpecification fleetSpecification;
	private final List<DvrpLoad> possibleCapacities;
	private final int reconfigurationInterval;
	private final Map<DvrpLoad, Integer>[] requestsPerTimeSlotPerDvrpLoad;
	private final int[] totalRequestsPerTimeSlot;
	private int totalRejections = 0;
	private final Map<Integer, List<Id<DvrpVehicle>>> vehiclesActivePerTimeSlot = new HashMap<>();
	private final IdMap<Request, Double> requestsSubmissionTimes = new IdMap<>(Request.class);
	private final IdMap<DvrpVehicle, DvrpLoad> defaultStartingCapacities = new IdMap<>(DvrpVehicle.class);
	private final IdMap<DvrpVehicle, DvrpLoad> overriddenStartingCapacities = new IdMap<>(DvrpVehicle.class);
	private final IdMap<DvrpVehicle, DvrpLoad>[] capacityReconfigurations;
	private final boolean allowCapacityChangeBeforeDayStarts;
	private final List<Link> links;
	private final Random random;
	private final CapacityChangeLinkSelection capacityChangeLinkSelection;
	private final QuadTree<Link> linkQuadTree;
	private final IdMap<DvrpVehicle, Link> vehicleCapacityChangeLocations;

	public DefaultCapacityReconfigurationLogic(FleetSpecification fleetSpecification, Set<DvrpLoad> possibleCapacities,
			Network network, Collection<? extends Link> links,
			int reconfigurationInterval) {
		this(fleetSpecification, possibleCapacities, network, links, reconfigurationInterval,
				true, CapacityChangeLinkSelection.RANDOM);
	}

	@SuppressWarnings("unchecked")
	public DefaultCapacityReconfigurationLogic(FleetSpecification fleetSpecification, Set<DvrpLoad> possibleCapacities,
			Network network, Collection<? extends Link> links,
			int reconfigurationInterval, boolean allowCapacityChangeBeforeDayStarts,
			CapacityChangeLinkSelection capacityChangeLinkSelection) {
		this.allowCapacityChangeBeforeDayStarts = allowCapacityChangeBeforeDayStarts;
		this.fleetSpecification = fleetSpecification;
		this.reconfigurationInterval = reconfigurationInterval;
		this.links = new ArrayList<>(links);
		this.random = MatsimRandom.getRandom();
		this.capacityChangeLinkSelection = capacityChangeLinkSelection;
		if (this.capacityChangeLinkSelection.equals(CapacityChangeLinkSelection.ClosestToVehicleStartLocation)) {
			Collection<Node> nodes = new HashSet<>();
			this.links.stream().flatMap(link -> Stream.of(link.getFromNode(), link.getToNode())).forEach(nodes::add);
			double[] bounds = NetworkUtils.getBoundingBox(nodes);
			this.linkQuadTree = new QuadTree<>(bounds[0], bounds[1], bounds[2], bounds[3]);
			this.links.forEach(link -> this.linkQuadTree.put(link.getCoord().getX(), link.getCoord().getY(), link));

			this.vehicleCapacityChangeLocations = new IdMap<>(DvrpVehicle.class);
			this.fleetSpecification.getVehicleSpecifications()
					.forEach((id, specification) -> {
						Link startingLink = Objects
								.requireNonNull(network.getLinks().get(specification.getStartLinkId()));
						Coord coord = startingLink.getCoord();
						Link link = linkQuadTree.getClosest(coord.getX(), coord.getY());
						vehicleCapacityChangeLocations.put(id, link);
					});

		} else {
			this.linkQuadTree = null;
			this.vehicleCapacityChangeLocations = null;
		}

		this.possibleCapacities = new ArrayList<>();
		// We start by the vehicle capacities coming from the fleet specification
		double maxServiceEndTime = -1;
		for (DvrpVehicleSpecification vehicleSpecification : fleetSpecification.getVehicleSpecifications().values()) {
			if (!this.possibleCapacities.contains(vehicleSpecification.getCapacity())) {
				this.possibleCapacities.add(vehicleSpecification.getCapacity());
			}
			if (vehicleSpecification.getServiceEndTime() > maxServiceEndTime) {
				maxServiceEndTime = vehicleSpecification.getServiceEndTime();
			}
			for (int slotIndex = timeToSlotIndex(
					vehicleSpecification.getServiceBeginTime()); slotIndex < timeToSlotIndex(
							vehicleSpecification.getServiceEndTime()); slotIndex++) {
				this.vehiclesActivePerTimeSlot.computeIfAbsent(slotIndex, key -> new ArrayList<>())
						.add(vehicleSpecification.getId());
			}
			defaultStartingCapacities.put(vehicleSpecification.getId(), vehicleSpecification.getCapacity());
		}
		for (DvrpLoad capacity : possibleCapacities) {
			if (!this.possibleCapacities.contains(capacity)) {
				this.possibleCapacities.add(capacity);
			}
		}

		int maxSlot = timeToSlotIndex(maxServiceEndTime);
		requestsPerTimeSlotPerDvrpLoad = new Map[maxSlot + 1];
		totalRequestsPerTimeSlot = new int[maxSlot + 1];
		this.capacityReconfigurations = new IdMap[maxSlot + 1];
		for (int i = 0; i <= maxSlot; i++) {
			this.capacityReconfigurations[i] = new IdMap<>(DvrpVehicle.class);
		}
		reset(-1);
	}

	private int timeToSlotIndex(double time) {
		return (int) (time / this.reconfigurationInterval);
	}

	private int timeSlotIndexToTime(int index) {
		return this.reconfigurationInterval * index;
	}

	private DvrpLoad getFittingCapacity(DvrpLoad load) {
		for (DvrpLoad capacity : this.possibleCapacities) {
			if (load.fitsIn(capacity)) {
				return capacity;
			}
		}
		throw new IllegalArgumentException(
				"Found a request that cannot be handled by one of the registered capacities");
	}

	@Override
	public void handleEvent(DrtRequestSubmittedEvent event) {
		this.requestsSubmissionTimes.put(event.getRequestId(), event.getTime());
		DvrpLoad load = this.getFittingCapacity(event.getLoad());
		int timeSlot = this.timeToSlotIndex(event.getTime());
		if (timeSlot < this.totalRequestsPerTimeSlot.length) {
			// Only requests that happened during vehicle service are considered
			requestsPerTimeSlotPerDvrpLoad[timeSlot].put(load,
					requestsPerTimeSlotPerDvrpLoad[timeSlot].getOrDefault(load, 0) + 1);
			totalRequestsPerTimeSlot[timeSlot]++;
		}
	}

	@Override
	public void handleEvent(PassengerRequestRejectedEvent event) {
		if (event.getCause().equals("no_insertion_found")) {
			int timeSlot = this.timeToSlotIndex(this.requestsSubmissionTimes.get(event.getRequestId()));
			if (timeSlot < this.totalRequestsPerTimeSlot.length) {
				totalRejections++;
			}
		}
	}

	@Override
	public void reset(int iteration) {
		updateCapacityPlanning();
		this.totalRejections = 0;
		this.requestsSubmissionTimes.clear();
		for (int i = 0; i < requestsPerTimeSlotPerDvrpLoad.length; i++) {
			totalRequestsPerTimeSlot[i] = 0;
			requestsPerTimeSlotPerDvrpLoad[i] = new HashMap<>();
		}
	}

	private void updateCapacityPlanning() {
		if (totalRejections == 0) {
			// Nothing to do
			return;
		}

		IdMap<DvrpVehicle, DvrpLoad> currentCapacities = new IdMap<>(DvrpVehicle.class);
		IdMap<DvrpVehicle, Integer> capacityChangesPerVehicle = new IdMap<>(DvrpVehicle.class);

		this.defaultStartingCapacities.forEach(currentCapacities::put);
		this.fleetSpecification.getVehicleSpecifications().keySet()
				.forEach(vehicleId -> capacityChangesPerVehicle.put(vehicleId, 0));
		Map<DvrpLoad, Integer> currentNumberOfVehiclesPerCapacity = new HashMap<>();

		for (DvrpLoad capacity : this.possibleCapacities) {
			for (Map.Entry<Id<DvrpVehicle>, DvrpLoad> entry : defaultStartingCapacities.entrySet()) {
				if (entry.getValue().equals(capacity)) {
					currentNumberOfVehiclesPerCapacity.put(capacity,
							currentNumberOfVehiclesPerCapacity.getOrDefault(capacity, 0) + 1);
				}
			}
		}

		this.overriddenStartingCapacities.clear();
		for (int i = 0; i < requestsPerTimeSlotPerDvrpLoad.length; i++) {
			capacityReconfigurations[i] = new IdMap<>(DvrpVehicle.class);
		}

		for (int timeSlotIndex = 0; timeSlotIndex < this.totalRequestsPerTimeSlot.length; timeSlotIndex++) {
			if (totalRequestsPerTimeSlot[timeSlotIndex] > 0) {
				Map<DvrpLoad, Integer> target = new HashMap<>();
				Map<DvrpLoad, Integer> delta = new HashMap<>();
				for (Map.Entry<DvrpLoad, Integer> entry : this.requestsPerTimeSlotPerDvrpLoad[timeSlotIndex]
						.entrySet()) {
					DvrpLoad load = entry.getKey();
					double requests = entry.getValue();
					target.put(load, (int) (requests / totalRequestsPerTimeSlot[timeSlotIndex]
							* fleetSpecification.getVehicleSpecifications().size()));
					delta.put(load, target.get(load) - currentNumberOfVehiclesPerCapacity.getOrDefault(load, 0));
				}
				for (DvrpLoad capacity : this.possibleCapacities) {
					boolean noMoreTransferableVehicles = false;
					while (delta.getOrDefault(capacity, 0) > 0) {
						Id<DvrpVehicle> vehicleToTransfer = null;
						int minTransferableVehicleCapacityChanges = -1;
						DvrpLoad currentVehicleToTransferCapacity = null;
						for (Id<DvrpVehicle> vehicleId : this.fleetSpecification.getVehicleSpecifications().keySet()) {
							if (!this.vehiclesActivePerTimeSlot.get(timeSlotIndex).contains(vehicleId)) {
								continue;
							}
							DvrpLoad currentVehicleCapacity = currentCapacities.get(vehicleId);
							if (delta.getOrDefault(currentVehicleCapacity, 0) < 0) {
								if (minTransferableVehicleCapacityChanges < 0 || capacityChangesPerVehicle
										.get(vehicleId) < minTransferableVehicleCapacityChanges) {
									minTransferableVehicleCapacityChanges = capacityChangesPerVehicle.get(vehicleId);
									vehicleToTransfer = vehicleId;
									currentVehicleToTransferCapacity = currentVehicleCapacity;
								}
							}
						}
						if (vehicleToTransfer != null) {
							if (minTransferableVehicleCapacityChanges == 0 && this.allowCapacityChangeBeforeDayStarts) {
								overriddenStartingCapacities.put(vehicleToTransfer, capacity);
							} else {
								capacityReconfigurations[timeSlotIndex].put(vehicleToTransfer, capacity);
							}
							capacityChangesPerVehicle.put(vehicleToTransfer,
									capacityChangesPerVehicle.getOrDefault(vehicleToTransfer, 0) + 1);
							delta.put(capacity, delta.get(capacity) - 1);
							delta.put(currentVehicleToTransferCapacity,
									delta.get(currentVehicleToTransferCapacity) + 1);
							currentNumberOfVehiclesPerCapacity.put(capacity,
									currentNumberOfVehiclesPerCapacity.getOrDefault(capacity, 0) + 1);
							currentNumberOfVehiclesPerCapacity.put(currentVehicleToTransferCapacity,
									currentNumberOfVehiclesPerCapacity.get(currentVehicleToTransferCapacity) - 1);
							currentCapacities.put(vehicleToTransfer, capacity);
						} else {
							noMoreTransferableVehicles = true;
							break;
						}
					}
					if (noMoreTransferableVehicles) {
						break;
					}
				}
			}
		}
	}

	@Override
	public Optional<DvrpLoad> getUpdatedStartCapacity(DvrpVehicle vehicle) {
		return Optional.ofNullable(this.overriddenStartingCapacities.get(vehicle.getId()));
	}

	@Override
	public List<ReconfigurationItem> getCapacityUpdates(DvrpVehicle dvrpVehicle) {
		List<ReconfigurationItem> capacityChangeItems = new ArrayList<>();
		for (int timeSlotIndex = 0; timeSlotIndex < this.capacityReconfigurations.length; timeSlotIndex++) {
			IdMap<DvrpVehicle, DvrpLoad> currentReconfigurations = this.capacityReconfigurations[timeSlotIndex];
			if (currentReconfigurations.containsKey(dvrpVehicle.getId())) {
				capacityChangeItems.add(new ReconfigurationItem(timeSlotIndexToTime(timeSlotIndex),
						getCapacityChangeLinkId(dvrpVehicle), currentReconfigurations.get(dvrpVehicle.getId())));
			}
		}
		return capacityChangeItems;
	}

	protected Id<Link> getCapacityChangeLinkId(DvrpVehicle dvrpVehicle) {
		if (this.capacityChangeLinkSelection.equals(CapacityChangeLinkSelection.RANDOM)) {
			return this.links.get(this.random.nextInt(this.links.size())).getId();
		} else {
			return Objects.requireNonNull(vehicleCapacityChangeLocations.get(dvrpVehicle.getId()).getId());
		}
	}

	static public interface CapacitySupplier {
		Set<DvrpLoad> getAvailableCapacities(DvrpLoadType loadType);
	}

	static public void install(Controler controller, String mode, CapacitySupplier capacitySupplier) {
		install(controller, mode, capacitySupplier, 7200, CapacityChangeLinkSelection.RANDOM);
	}

	static public void install(Controler controller, String mode, CapacitySupplier capacitySupplier, int reconfigurationInterval, DefaultCapacityReconfigurationLogic.CapacityChangeLinkSelection capacityChangeLinkSelection) {
		controller.addOverridingModule(new AbstractDvrpModeModule(mode) {
			@Override
			public void install() {
				bindModal(DefaultCapacityReconfigurationLogic.class).toProvider(modalProvider(getter -> {
					FleetSpecification fleetSpecification = getter.getModal(FleetSpecification.class);

					DvrpLoadType loadType = getter.getModal(DvrpLoadType.class);
					Set<DvrpLoad> possibleCapacities = capacitySupplier.getAvailableCapacities(loadType);

					Network network = getter.getModal(Network.class);

					return new DefaultCapacityReconfigurationLogic(fleetSpecification,
							possibleCapacities, network, network.getLinks().values(), reconfigurationInterval,
							false,
							capacityChangeLinkSelection);
				})).asEagerSingleton();

				bindModal(CapacityReconfigurationLogic.class).to(modalKey(DefaultCapacityReconfigurationLogic.class));
				addEventHandlerBinding().to(modalKey(DefaultCapacityReconfigurationLogic.class));
			}
		});
	}
}
