package org.matsim.contrib.ev.strategic.infrastructure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecification;
import org.matsim.contrib.ev.strategic.access.ChargerAccess;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacility;

/**
 * This charger provider examines the activity at which an activity-based
 * charging slot is potentially supposed to happen. It keeps a list of
 * facilities to which chargers are assigned and returns the chargers that are
 * assigned to the facility at which the charging activity is taking place. The
 * standard selection criteria (person charger access; search radius) are
 * evaluated.
 * 
 * Chargers can be assigned to facilities by setting the sevc:facilities
 * attribute of the charger, which should contain a list of facility IDs.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class FacilityChargerProvider implements ChargerProvider {
	static public final String FACILITIES_CHARGER_ATTRIBUTE = "sevc:facilities";

	private final IdMap<ActivityFacility, List<ChargerSpecification>> chargers;

	private final double searchRadius;
	private final Scenario scenario;

	private final ChargerAccess access;

	public FacilityChargerProvider(IdMap<ActivityFacility, List<ChargerSpecification>> chargers, double searchRadius,
			Scenario scenario, ChargerAccess access) {
		this.chargers = chargers;
		this.searchRadius = searchRadius;
		this.scenario = scenario;
		this.access = access;
	}

	@Override
	public Collection<ChargerSpecification> findChargers(Person person, Plan plan, ChargerRequest request) {
		if (!request.isLegBased()) {
			List<ChargerSpecification> candidates = new ArrayList<>(
					chargers.getOrDefault(request.startActivity().getFacilityId(), Collections.emptyList()));

			Coord activityLocation = PopulationUtils.decideOnCoordForActivity(request.startActivity(),
					scenario);

			candidates.removeIf(charger -> {
				return !access.hasAccess(person, charger);
			});

			candidates.removeIf(charger -> {
				Coord chargerLocation = scenario.getNetwork().getLinks().get(charger.getLinkId()).getCoord();
				double distance = CoordUtils.calcEuclideanDistance(activityLocation, chargerLocation);
				return distance > searchRadius;
			});

			return candidates;
		}

		return Collections.emptySet();
	}

	static public FacilityChargerProvider build(ChargingInfrastructureSpecification infrastructure, double searchRadius,
			Scenario scenario, ChargerAccess access) {
		IdMap<ActivityFacility, List<ChargerSpecification>> chargers = new IdMap<>(ActivityFacility.class);

		for (ChargerSpecification charger : infrastructure.getChargerSpecifications().values()) {
			String raw = (String) charger.getAttributes().getAttribute(FACILITIES_CHARGER_ATTRIBUTE);

			if (raw != null) {
				for (String rawFacilityId : raw.split(",")) {
					Id<ActivityFacility> facilityId = Id.create(rawFacilityId.trim(), ActivityFacility.class);
					chargers.computeIfAbsent(facilityId, id -> new LinkedList<>()).add(charger);
				}
			}
		}

		return new FacilityChargerProvider(chargers, searchRadius, scenario, access);
	}

	/**
	 * Returns whether a charger is assigned to at least one facility.
	 */
	static public boolean isFacilityCharger(ChargerSpecification charger) {
		return getFacilityIds(charger).size() > 0;
	}

	/**
	 * Sets the facilities to which a charger is assigned.
	 */
	static public void setFacilityIds(ChargerSpecification charger, Collection<Id<ActivityFacility>> facilityIds) {
		charger.getAttributes().putAttribute(FACILITIES_CHARGER_ATTRIBUTE,
				facilityIds.stream().map(Id::toString).collect(Collectors.joining(",")));
	}

	/**
	 * Returns the facilities to which a charger is assigned.
	 */
	static public Set<Id<ActivityFacility>> getFacilityIds(ChargerSpecification charger) {
		String raw = (String) charger.getAttributes().getAttribute(FACILITIES_CHARGER_ATTRIBUTE);
		Set<Id<ActivityFacility>> facilityIds = new HashSet<>();

		if (raw != null) {
			for (String rawFacilityIds : raw.split(",")) {
				Id<ActivityFacility> facilityId = Id.create(rawFacilityIds.trim(), ActivityFacility.class);
				facilityIds.add(facilityId);
			}
		}

		return facilityIds;
	}
}
