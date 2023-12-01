package org.matsim.contrib.drt.extension.insertion.distances;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.Waypoint;
import org.matsim.contrib.drt.optimizer.Waypoint.End;
import org.matsim.contrib.drt.optimizer.insertion.InsertionDetourTimeCalculator.DetourTimeInfo;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.passenger.AcceptedDrtRequest;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.dvrp.schedule.DriveTask;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;

public class InsertionDistanceCalculator {
	public VehicleDistance calculateScheduledDistance(VehicleEntry vehicleEntry) {
		Schedule schedule = vehicleEntry.vehicle.getSchedule();

		double occupiedDistance = 0.0;
		double emptyDistance = 0.0;
		double passengerDistance = 0.0;

		if (!schedule.getStatus().equals(ScheduleStatus.UNPLANNED)) {
			int occupancy = 0;

			for (Task task : schedule.getTasks()) {
				if (task instanceof DrtStopTask) {
					occupancy += getPassengers(((DrtStopTask) task).getPickupRequests().values());
					occupancy -= getPassengers(((DrtStopTask) task).getDropoffRequests().values());
				}

				if (task instanceof DriveTask) {
					double taskDistance = calculateTaskDistance((DriveTask) task);

					if (occupancy == 0) {
						emptyDistance += taskDistance;
					} else {
						occupiedDistance += taskDistance;
						passengerDistance += taskDistance * occupancy;
					}
				}
			}
		}

		return new VehicleDistance(occupiedDistance, emptyDistance, passengerDistance);
	}

	private int getPassengers(Collection<AcceptedDrtRequest> requests) {
		return requests.stream().mapToInt(r -> r.getPassengerIds().size()).sum();
	}

	public VehicleDistance calculateInsertionDistance(Insertion insertion, DetourTimeInfo detourTimeInfo,
			DistanceCalculator distanceEstimator) {
		// pairs containing driven distance and occupany
		List<DistanceEntry> addedDistances = new LinkedList<>();
		List<DistanceEntry> removedDistances = new LinkedList<>();

		final Link pickupFromLink = insertion.pickup.previousWaypoint.getLink();
		final Link pickupNewLink = insertion.pickup.newWaypoint.getLink();
		final Link pickupToLink = insertion.pickup.nextWaypoint.getLink();

		int beforePickupOccupancy = insertion.pickup.previousWaypoint.getOutgoingOccupancy();

		double beforePickupDistance = distanceEstimator
				.calculateDistance(insertion.pickup.previousWaypoint.getDepartureTime(), pickupFromLink, pickupNewLink);

		double afterPickupDistance = distanceEstimator.calculateDistance(detourTimeInfo.pickupDetourInfo.departureTime,
				pickupNewLink, pickupToLink);

		addedDistances.add(new DistanceEntry(beforePickupDistance, beforePickupOccupancy));
		addedDistances.add(new DistanceEntry(afterPickupDistance, beforePickupOccupancy + 1));

		if (insertion.pickup.previousWaypoint instanceof Waypoint.Start) {
			Task currentTask = insertion.vehicleEntry.vehicle.getSchedule().getCurrentTask();

			if (currentTask instanceof DriveTask) {
				OnlineDriveTaskTracker tracker = (OnlineDriveTaskTracker) ((DriveTask) currentTask).getTaskTracker();

				double removedStartDistance = 0.0;

				for (int k = tracker.getCurrentLinkIdx(); k < tracker.getPath().getLinkCount(); k++) {
					removedStartDistance += tracker.getPath().getLink(k).getLength();
				}

				int startOccupancy = insertion.pickup.previousWaypoint.getOutgoingOccupancy();
				removedDistances.add(new DistanceEntry(removedStartDistance, startOccupancy));
			}
		} else {
			int startIndex = ((Waypoint.Stop) insertion.pickup.previousWaypoint).task.getTaskIdx();
			int occupancy = insertion.pickup.previousWaypoint.getOutgoingOccupancy();

			final int endIndex;
			if (insertion.pickup.nextWaypoint instanceof Waypoint.End) {
				endIndex = insertion.vehicleEntry.vehicle.getSchedule().getTaskCount() - 1;
			} else if (!(insertion.pickup.nextWaypoint instanceof Waypoint.Dropoff)) {
				endIndex = ((Waypoint.Stop) insertion.pickup.nextWaypoint).task.getTaskIdx();
			} else {
				endIndex = startIndex;
			}

			for (int index = startIndex; index < endIndex; index++) {
				Task task = insertion.vehicleEntry.vehicle.getSchedule().getTasks().get(index);

				if (task instanceof DrtStopTask) {
					occupancy += getPassengers(((DrtStopTask) task).getPickupRequests().values());
					occupancy -= getPassengers(((DrtStopTask) task).getDropoffRequests().values());
				}

				if (task instanceof DriveTask) {
					double taskDistance = calculateTaskDistance((DriveTask) task);
					removedDistances.add(new DistanceEntry(taskDistance, occupancy));
				}
			}
		}

		final Link dropoffFromLink = insertion.dropoff.previousWaypoint.getLink();
		final Link dropoffNewLink = insertion.dropoff.newWaypoint.getLink();
		final Link dropoffToLink = (insertion.dropoff.nextWaypoint instanceof End)
				? insertion.dropoff.newWaypoint.getLink()
				: insertion.dropoff.nextWaypoint.getLink();

		final int beforeDropoffOccupancy;
		if (insertion.dropoff.index > insertion.pickup.index) {
			beforeDropoffOccupancy = insertion.dropoff.previousWaypoint.getOutgoingOccupancy() + 1;
			double beforeDropoffDistance = distanceEstimator.calculateDistance(
					insertion.dropoff.previousWaypoint.getDepartureTime(), dropoffFromLink, dropoffNewLink);

			addedDistances.add(new DistanceEntry(beforeDropoffDistance, beforeDropoffOccupancy));
		} else {
			beforeDropoffOccupancy = beforePickupOccupancy + 1;
		}

		double afterDropoffDistance = distanceEstimator.calculateDistance(detourTimeInfo.dropoffDetourInfo.arrivalTime,
				dropoffNewLink, dropoffToLink);
		addedDistances.add(new DistanceEntry(afterDropoffDistance, beforeDropoffOccupancy - 1));

		if (insertion.dropoff.index > insertion.pickup.index) {
			int startIndex = ((Waypoint.Stop) insertion.dropoff.previousWaypoint).task.getTaskIdx();
			int occupancy = insertion.dropoff.previousWaypoint.getOutgoingOccupancy();

			final int endIndex;
			if (insertion.dropoff.nextWaypoint instanceof Waypoint.End) {
				endIndex = insertion.vehicleEntry.vehicle.getSchedule().getTaskCount() - 1;
			} else {
				endIndex = ((Waypoint.Stop) insertion.dropoff.nextWaypoint).task.getTaskIdx();
			}

			for (int index = startIndex; index < endIndex; index++) {
				Task task = insertion.vehicleEntry.vehicle.getSchedule().getTasks().get(index);

				if (task instanceof DrtStopTask) {
					occupancy += getPassengers(((DrtStopTask) task).getPickupRequests().values());
					occupancy -= getPassengers(((DrtStopTask) task).getDropoffRequests().values());
				}

				if (task instanceof DriveTask) {
					double taskDistance = calculateTaskDistance((DriveTask) task);
					removedDistances.add(new DistanceEntry(taskDistance, occupancy));
				}
			}
		}

		double occupiedDriveDistance = 0.0;
		double emptyDriveDistance = 0.0;
		double passengerDistance = 0.0;

		for (var entry : addedDistances) {
			if (entry.occupancy == 0) {
				emptyDriveDistance += entry.distance;
			} else {
				occupiedDriveDistance += entry.distance;
				passengerDistance += entry.distance * entry.occupancy;
			}
		}

		for (var entry : removedDistances) {
			if (entry.occupancy == 0) {
				emptyDriveDistance -= entry.distance;
			} else {
				occupiedDriveDistance -= entry.distance;
				passengerDistance -= entry.hashCode() * entry.occupancy;
			}
		}

		return new VehicleDistance(occupiedDriveDistance, emptyDriveDistance, passengerDistance);
	}

	private double calculateTaskDistance(DriveTask task) {
		double distance = 0.0;

		for (int k = 0; k < task.getPath().getLinkCount(); k++) {
			distance += task.getPath().getLink(k).getLength();
		}

		return distance;
	}

	static public record VehicleDistance(double occupiedDriveDistance, double emptyDriveDistance,
			double passengerDistance) {
		public double totalDriveDistance() {
			return occupiedDriveDistance + emptyDriveDistance;
		}
	}

	private record DistanceEntry(double distance, int occupancy) {
	}
}
