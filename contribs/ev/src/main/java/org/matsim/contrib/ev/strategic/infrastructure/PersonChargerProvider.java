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

/**
 * This charger provider keeps a registry of chargers that are assigned to a
 * specific person. Whenever the person examines charging locations and the
 * standard conditions (subscriptions, search radius) are fulfilled, the
 * respective chargers are returned for that person.
 * 
 * Chargers can be assigned to persons by setting the sevc:persons
 * attribute of the charger, which should contain a list of person IDs.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class PersonChargerProvider implements ChargerProvider {
	static public final String PERSONS_CHARGER_ATTRIBUTE = "sevc:persons";

	private final IdMap<Person, List<ChargerSpecification>> chargers;

	private final Scenario scenario;
	private final double searchRadius;

	private final ChargerAccess access;

	public PersonChargerProvider(IdMap<Person, List<ChargerSpecification>> chargers, double searchRadius,
			Scenario scenario,
			ChargerAccess access) {
		this.chargers = chargers;
		this.searchRadius = searchRadius;
		this.scenario = scenario;
		this.access = access;
	}

	@Override
	public Collection<ChargerSpecification> findChargers(Person person, Plan plan, ChargerRequest request) {
		if (!request.isLegBased()) {
			List<ChargerSpecification> candidates = new ArrayList<>(
					chargers.getOrDefault(person.getId(), Collections.emptyList()));

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

	static public PersonChargerProvider build(ChargingInfrastructureSpecification infrastructure, double searchRadius,
			Scenario scenario, ChargerAccess access) {
		IdMap<Person, List<ChargerSpecification>> chargers = new IdMap<>(Person.class);

		for (ChargerSpecification charger : infrastructure.getChargerSpecifications().values()) {
			for (Id<Person> personId : getPersonIds(charger)) {
				chargers.computeIfAbsent(personId, id -> new LinkedList<>()).add(charger);
			}
		}

		return new PersonChargerProvider(chargers, searchRadius, scenario, access);
	}

	/**
	 * Returns whether a charger is assigned to at least one person.
	 */
	static public boolean isPersonCharger(ChargerSpecification charger) {
		return getPersonIds(charger).size() > 0;
	}

	/**
	 * Sets the persons to which a charger is assigned.
	 */
	static public void setPersonIds(ChargerSpecification charger, Collection<Id<Person>> personIds) {
		charger.getAttributes().putAttribute(PERSONS_CHARGER_ATTRIBUTE,
				personIds.stream().map(Id::toString).collect(Collectors.joining(",")));
	}

	/**
	 * Returns the persons to which a charger is assigned.
	 */
	static public Set<Id<Person>> getPersonIds(ChargerSpecification charger) {
		String raw = (String) charger.getAttributes().getAttribute(PERSONS_CHARGER_ATTRIBUTE);
		Set<Id<Person>> personIds = new HashSet<>();

		if (raw != null) {
			for (String rawPersonId : raw.split(",")) {
				Id<Person> personId = Id.createPersonId(rawPersonId.trim());
				personIds.add(personId);
			}
		}

		return personIds;
	}
}
