package org.matsim.contrib.dvrp.passenger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.load.DvrpLoad;
import org.matsim.contrib.dvrp.load.DvrpLoadType;
import org.matsim.utils.objectattributes.attributable.Attributes;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

/**
 * This class offers a default implementation of the {@link DvrpLoadFromTrip}
 * interface.
 * 
 * @author Tarek Chouaki (tkchouaki), IRT SystemX
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class DefaultDvrpLoadFromTrip implements DvrpLoadFromTrip {
	public static final String LOAD_ATTRIBUTE = "dvrp:load";
	public static final String LOAD_ATTRIBUTE_PREFIX = "dvrp:load:";

	private final DvrpLoadType loadType;
	private final String defaultSlot;

	@Inject
	public DefaultDvrpLoadFromTrip(DvrpLoadType loadType, String defaultSlot) {
		this.loadType = loadType;
		this.defaultSlot = defaultSlot;
	}

	@Override
	public DvrpLoad getLoad(Person person, Attributes tripAttributes) {
		DvrpLoad tripLoad = processAttributes(person.getAttributes(), person);
		DvrpLoad personLoad = processAttributes(tripAttributes, person);

		Preconditions.checkState(!(tripLoad != null && personLoad != null),
				String.format("Cannot define load on person and trip level for person %s",
						person.getId()));

		if (tripLoad != null) {
			return tripLoad;
		} else if (personLoad != null) {
			return personLoad;
		} else {
			return loadType.fromMap(Collections.singletonMap(defaultSlot, 1));
		}
	}

	private DvrpLoad processAttributes(Attributes attributes, Person person) {
		String loadRepresentation = (String) attributes.getAttribute(LOAD_ATTRIBUTE);

		Map<String, Number> load = new HashMap<>();
		for (var entry : attributes.getAsMap().entrySet()) {
			if (entry.getKey().startsWith(LOAD_ATTRIBUTE_PREFIX)) {
				Preconditions.checkState(loadRepresentation == null, String.format(
						"Cannot mix string-based and slot-based load configuration for person %s", person.getId()));
				load.put(entry.getKey().substring(LOAD_ATTRIBUTE_PREFIX.length()), (Number) entry.getValue());
			}
		}

		if (loadRepresentation != null) {
			return loadType.deserialize(loadRepresentation);
		} else if (!load.isEmpty()) {
			return loadType.fromMap(load);
		} else {
			return null;
		}
	}
}
