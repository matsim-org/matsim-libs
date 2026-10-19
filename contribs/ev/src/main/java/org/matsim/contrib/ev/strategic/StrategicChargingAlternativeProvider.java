package org.matsim.contrib.ev.strategic;

import com.google.common.base.Verify;
import jakarta.annotation.Nullable;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.contrib.ev.reservation.DistributedChargerReservationManager;
import org.matsim.contrib.ev.strategic.StrategicChargingConfigGroup.AlternativeSearchStrategy;
import org.matsim.contrib.ev.strategic.access.ChargerAccess;
import org.matsim.contrib.ev.strategic.infrastructure.ChargerProvider;
import org.matsim.contrib.ev.strategic.infrastructure.ChargerProvider.ChargerRequest;
import org.matsim.contrib.ev.withinday.ChargingAlternative;
import org.matsim.contrib.ev.withinday.ChargingAlternativeProvider;
import org.matsim.contrib.ev.withinday.ChargingSlot;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeTracker;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * This is the ChargingAlternativeProvider of the strategic charging package.
 * Whenever an agent tries to find a new charger, the ChargingProvider logic is
 * used to find and select viable locations.
 *
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class StrategicChargingAlternativeProvider implements ChargingAlternativeProvider {
	private final Scenario scenario;
	private final ChargerProvider chargerProvider;
	private final ChargerAccess access;
	private final ChargingInfrastructure infrastruture;

	private final DistributedChargerReservationManager reservationManager;
	private final TimeInterpretation timeInterpretation;

	private final AlternativeSearchStrategy onlineSearchStrategy;
	private final boolean useProactiveOnlineSearch;

	private final CriticalAlternativeProvider criticalProvider;
	private final int maximumAlternatives;

	public StrategicChargingAlternativeProvider(Scenario scenario, ChargerProvider chargerProvider,
	                                            ChargingInfrastructure infrastructure,
	                                            ChargerAccess access,
	                                            AlternativeSearchStrategy onlineSearchStrategy, boolean useProactiveOnlineSearch,
	                                            TimeInterpretation timeInterpretation,
	                                            @Nullable DistributedChargerReservationManager reservationManager, CriticalAlternativeProvider criticalProvider,
	                                            int maximumAlternatives) {
		this.maximumAlternatives = maximumAlternatives;
		this.chargerProvider = chargerProvider;
		this.infrastruture = infrastructure;
		this.access = access;
		this.scenario = scenario;
		this.reservationManager = reservationManager;
		this.timeInterpretation = timeInterpretation;
		this.onlineSearchStrategy = onlineSearchStrategy;
		this.useProactiveOnlineSearch = useProactiveOnlineSearch;
		this.criticalProvider = criticalProvider;
	}

	@Override
	public void findAlternativeAsync(double now, PlanAgent agent, ElectricVehicle vehicle, ChargingSlot slot, List<ChargingAlternative> trace,
	                                 Consumer<Optional<ChargingAlternative>> callback) {
		if (trace.size() >= maximumAlternatives) {
			callback.accept(Optional.empty());
			return;
		}

		var plan = agent.getCurrentPlan();
		var person = plan.getPerson();

		Coord initialLocation = slot.isLegBased() ? slot.charger().getCoord()
			: PopulationUtils.decideOnCoordForActivity(slot.startActivity(), scenario);

		Collection<Charger> candidates = chargerProvider.findChargers(person, plan,
				new ChargerRequest(slot.startActivity(), slot.endActivity(), slot.leg(), slot.duration())).stream()
			.map(ChargerSpecification::getId).map(infrastruture.getChargers()::get).collect(Collectors.toList());

		candidates.remove(slot.charger());
		trace.forEach(s -> candidates.removeIf(t -> t.getId().equals(s.charger())));
		candidates.removeIf(candidate -> !access.hasAccess(person, candidate));

		if (onlineSearchStrategy.equals(AlternativeSearchStrategy.OccupancyBased)) {
			// Is this needed from a modelling perspective? If we have a booking system, why does a user care about the current occupancy of a
			// charger?
			throw new UnsupportedOperationException("AlternativeSearchStrategy.OccupancyBased search strategy is not yet implemented for distributed" +
				"simulation. We do not have access to the occupancy information of chargers. This would have to be implemented if it is important");
		}

		if (candidates.isEmpty()) {
			callback.accept(Optional.empty());
			return;
		}

		List<Charger> sortedCandidates = candidates.stream().sorted((a, b) -> {
			double distanceA = CoordUtils.calcEuclideanDistance(initialLocation, a.getCoord());
			double distanceB = CoordUtils.calcEuclideanDistance(initialLocation, b.getCoord());
			return Double.compare(distanceA, distanceB);
		}).collect(Collectors.toList());

		double duration = slot.isLegBased() ? slot.duration() : 0.0;

		if (onlineSearchStrategy.equals(AlternativeSearchStrategy.ReservationBased)) {
			double reservationEndTime = estimateReservationEndTime(now, plan, slot);
			tryReservation(sortedCandidates, 0, vehicle, now, reservationEndTime, duration, callback);
		} else {
			// Naive or OccupancyBased: no reservation needed, pick the closest
			callback.accept(Optional.of(new ChargingAlternative(sortedCandidates.getFirst().getId(), duration)));
		}
	}

	private void tryReservation(List<Charger> candidates, int index, ElectricVehicle vehicle, double now,
	                            double reservationEndTime, double duration,
	                            Consumer<Optional<ChargingAlternative>> callback) {
		if (index >= candidates.size()) {
			callback.accept(Optional.empty());
			return;
		}

		Charger candidate = candidates.get(index);
		reservationManager.addReservation(candidate.getSpecification().getId(), vehicle.getId(), now, reservationEndTime, reservation -> {
			if (reservation.isPresent()) {
				callback.accept(Optional.of(new ChargingAlternative(candidate.getId(), duration)));
			} else {
				tryReservation(candidates, index + 1, vehicle, now, reservationEndTime, duration, callback);
			}
		});
	}

	@Override
	@Nullable
	public ChargingAlternative findAlternative(double now, Person person, Plan plan, ElectricVehicle vehicle,
	                                           ChargingSlot slot, List<ChargingAlternative> trace) {


		if (trace.size() >= maximumAlternatives) {
			return null; // search limit has been reached
		}

		// obtain possible other chargers within the search radius
		Coord initialLocation = slot.isLegBased() ? slot.charger().getCoord()
			: PopulationUtils.decideOnCoordForActivity(slot.startActivity(), scenario);

		Collection<Charger> candidates = chargerProvider.findChargers(person, plan,
				new ChargerRequest(slot.startActivity(), slot.endActivity(), slot.leg(), slot.duration())).stream()
			.map(ChargerSpecification::getId).map(infrastruture.getChargers()::get).collect(Collectors.toList());

		// remove chargers that have already been visited
		candidates.remove(slot.charger());
		trace.forEach(s -> candidates.removeIf(t -> t.getId().equals(s.charger())));

		// remove chargers to which the person has no access
		candidates.removeIf(candidate -> !access.hasAccess(person, candidate));

		// remove chargers that have no free spots
		if (onlineSearchStrategy.equals(AlternativeSearchStrategy.OccupancyBased)) {
			candidates.removeIf(candidate -> candidate.getLogic().getPluggedVehicles().size() == candidate.getPlugCount());
		}

		// remove chargers for which no reservation can be made
		double reservationEndTime = estimateReservationEndTime(now, plan, slot);

		if (onlineSearchStrategy.equals(AlternativeSearchStrategy.ReservationBased)) {
			candidates.removeIf(candidate -> !reservationManager.isAvailable(candidate.getSpecification().getId(), vehicle.getId(), now,
				reservationEndTime));
		}

		// perform a random selection
		if (!candidates.isEmpty()) {
			Charger selected = candidates.stream().min((a, b) -> {
				double distanceA = CoordUtils.calcEuclideanDistance(initialLocation, a.getCoord());
				double distanceB = CoordUtils.calcEuclideanDistance(initialLocation, b.getCoord());
				return Double.compare(distanceA, distanceB);
			}).get();

			// send the reservation if requested
			if (onlineSearchStrategy.equals(AlternativeSearchStrategy.ReservationBased)) {
				Verify.verify(
					reservationManager.addLocalReservation(selected.getSpecification().getId(), vehicle.getId(), now,
						reservationEndTime).isPresent());
			}

			double duration = slot.isLegBased() ? slot.duration() : 0.0;
			return new ChargingAlternative(selected.getId(), duration);
		}

		// no new candidate found
		return null;
	}

	@Override
	public void findEnrouteAlternativeAsync(double now, PlanAgent agent, ElectricVehicle vehicle, ChargingSlot slot, Consumer<Optional<ChargingAlternative>> callback) {

		var plan = agent.getCurrentPlan();

		if (slot == null) {
			// no activity-based or leg-based charging planned, but we may add a critical
			// charge

			if (criticalProvider != null) {
				criticalProvider.findEnrouteAlternativeAsync(now, agent, vehicle, slot, callback);
			} else {
				callback.accept(Optional.empty());
			}
			return;
		}

		// only if proactive search is enabled
		if (!useProactiveOnlineSearch) {
			callback.accept(Optional.empty());
			return;
		}

		// reserve upon approaching
		if (onlineSearchStrategy.equals(AlternativeSearchStrategy.ReservationBased)) {
			double reservationEndTime = estimateReservationEndTime(now, plan, slot);

			// try to make a reservation
			reservationManager.addReservation(slot.charger().getId(), vehicle.getId(), now, reservationEndTime, r -> {
				if (r.isPresent()) {
					callback.accept(Optional.empty());
				} else {
					findAlternativeAsync(now, agent, vehicle, slot, Collections.emptyList(), callback);
				}
			});
			return; // make sure the code below is not executed
		}

		var updateRequired = false;
		// proactively react if planned charger is occupied
		if (onlineSearchStrategy.equals(AlternativeSearchStrategy.OccupancyBased)) {
			updateRequired = slot.charger().getPlugCount() == slot.charger().getLogic().getPluggedVehicles()
				.size();
		}

		if (updateRequired) {
			findAlternativeAsync(now, agent, vehicle, slot, Collections.emptyList(), callback);
		} else {
			// keep initial slot
			callback.accept(Optional.empty());
		}

	}

	@Override
	@Nullable
	public ChargingAlternative findEnrouteAlternative(double now, Person person, Plan plan,
	                                                  ElectricVehicle vehicle,
	                                                  @Nullable ChargingSlot slot) {
		if (slot == null) {
			// no activity-based or leg-based charging planned, but we may add a critical
			// charge

			if (criticalProvider != null) {
				return criticalProvider.findEnrouteAlternative(now, person, plan, vehicle, slot);
			} else {
				return null; // no change
			}
		}

		// only if proactive search is enabled
		if (!useProactiveOnlineSearch) {
			return null;
		}

		boolean updateRequired = false;

		// reserve upon approaching
		if (onlineSearchStrategy.equals(AlternativeSearchStrategy.ReservationBased)) {
			double reservationEndTime = estimateReservationEndTime(now, plan, slot);

			// if a reservation can be made now, keep the initial slot
			if (reservationManager.isAvailable(slot.charger().getSpecification().getId(), vehicle.getId(), now,
				reservationEndTime)) {
				Verify.verify(reservationManager.addLocalReservation(slot.charger().getSpecification().getId(),
					vehicle.getId(), now, reservationEndTime).isPresent());
				return null;
			} else {
				updateRequired = true;
			}
		}

		// proactively react if planned charger is occupied
		if (onlineSearchStrategy.equals(AlternativeSearchStrategy.OccupancyBased)) {
			updateRequired = slot.charger().getPlugCount() == slot.charger().getLogic().getPluggedVehicles()
				.size();
		}

		if (updateRequired) {
			// use logic from above to find a new charger, excluding the planned attempt
			return findAlternative(now, person, plan, vehicle,
				slot, Collections.emptyList());
		} else {
			// keep initial slot
			return null;
		}
	}

	private double estimateReservationEndTime(double startTime, Plan plan, ChargingSlot slot) {
		TimeTracker timeTracker = new TimeTracker(timeInterpretation);
		timeTracker.setTime(startTime);

		if (!slot.isLegBased()) {
			int startIndex = plan.getPlanElements().indexOf(slot.startActivity());
			int endIndex = plan.getPlanElements().indexOf(slot.endActivity());

			for (int i = startIndex; i <= endIndex; i++) {
				timeTracker.addElement(plan.getPlanElements().get(i));
			}
		} else {
			// TODO: estimate drive-to time
			timeTracker.addDuration(slot.duration());
		}

		return timeTracker.getTime().orElse(Double.POSITIVE_INFINITY);
	}
}
