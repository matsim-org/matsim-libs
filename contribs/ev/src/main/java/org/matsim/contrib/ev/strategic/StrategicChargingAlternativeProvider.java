package org.matsim.contrib.ev.strategic;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.contrib.ev.reservation.ChargerReservationManager;
import org.matsim.contrib.ev.strategic.StrategicChargingConfigGroup.AlternativeSearchStrategy;
import org.matsim.contrib.ev.strategic.access.ChargerAccess;
import org.matsim.contrib.ev.strategic.infrastructure.ChargerProvider;
import org.matsim.contrib.ev.strategic.infrastructure.ChargerProvider.ChargerRequest;
import org.matsim.contrib.ev.withinday.ChargingAlternative;
import org.matsim.contrib.ev.withinday.ChargingAlternativeProvider;
import org.matsim.contrib.ev.withinday.ChargingSlot;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeTracker;

import com.google.common.base.Verify;

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

	private final ChargerReservationManager reservationManager;
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
			@Nullable ChargerReservationManager reservationManager, CriticalAlternativeProvider criticalProvider,
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
		trace.forEach(s -> {
			candidates.remove(s.charger());
		});

		// remove chargers to which the person has no access
		candidates.removeIf(candidate -> !access.hasAccess(person, candidate));

		// remove chargers that have no free spots
		if (onlineSearchStrategy.equals(AlternativeSearchStrategy.OccupancyBased)) {
			candidates.removeIf(candidate -> {
				return candidate.getLogic().getPluggedVehicles().size() == candidate.getPlugCount();
			});
		}

		// remove chargers for which no reservation can be made
		double reservationStartTime = now;
		double reservationEndTime = estimateReservationEndTime(reservationStartTime, plan, slot);

		if (onlineSearchStrategy.equals(AlternativeSearchStrategy.ReservationBased)) {
			candidates.removeIf(candidate -> {
				return !reservationManager.isAvailable(candidate.getSpecification(), vehicle, reservationStartTime,
						reservationEndTime);
			});
		}

		// perform a random selection
		if (candidates.size() > 0) {
			Charger selected = candidates.stream().sorted((a, b) -> {
				double distanceA = CoordUtils.calcEuclideanDistance(initialLocation, a.getCoord());
				double distanceB = CoordUtils.calcEuclideanDistance(initialLocation, b.getCoord());
				return Double.compare(distanceA, distanceB);
			}).findFirst().get();

			// send the reservation if requested
			if (onlineSearchStrategy.equals(AlternativeSearchStrategy.ReservationBased)) {
				Verify.verify(
						reservationManager.addReservation(selected.getSpecification(), vehicle, reservationStartTime,
								reservationEndTime) != null);
			}

			double duration = slot.isLegBased() ? slot.duration() : 0.0;
			return new ChargingAlternative(selected, duration);
		}

		// no new candidate found
		return null;
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
			double reservationStartTime = now;
			double reservationEndTime = estimateReservationEndTime(reservationStartTime, plan, slot);

			// if a reservation can be made now, keep the initial slot
			if (reservationManager.isAvailable(slot.charger().getSpecification(), vehicle, reservationStartTime,
					reservationEndTime)) {
				Verify.verifyNotNull(reservationManager.addReservation(slot.charger().getSpecification(),
						vehicle, reservationStartTime, reservationEndTime));
				return null;
			} else {
				updateRequired = true;
			}
		}

		// proactively react if planned charger is occupied
		if (onlineSearchStrategy.equals(AlternativeSearchStrategy.OccupancyBased)) {
			updateRequired |= slot.charger().getPlugCount() == slot.charger().getLogic().getPluggedVehicles()
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
