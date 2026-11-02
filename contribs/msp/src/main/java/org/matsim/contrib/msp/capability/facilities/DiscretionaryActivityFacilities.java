package org.matsim.contrib.msp.capability.facilities;

import org.matsim.api.core.v01.Id;
import org.matsim.facilities.ActivityFacility;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * The places at which discretionary activities can be performed, indexed the way the capability analysis
 * asks for them: by {@link DiscretionaryActivityFacility#getType() type} — "where could this agent go
 * shopping?" — and by the id of the underlying MATSim facility.
 * <p>
 * Immutable. Build one with {@link DiscretionaryActivityFacilityExtractor} from a MATSim
 * {@link org.matsim.facilities.ActivityFacilities}.
 */
public final class DiscretionaryActivityFacilities {

	private final Map<String, List<DiscretionaryActivityFacility>> byType;
	private final Map<Id<ActivityFacility>, List<DiscretionaryActivityFacility>> byFacility;
	private final List<DiscretionaryActivityFacility> all;

	public DiscretionaryActivityFacilities(Collection<DiscretionaryActivityFacility> facilities) {

		// types sorted so that output is reproducible, everything else in insertion order
		Map<String, List<DiscretionaryActivityFacility>> byType = new TreeMap<>();
		Map<Id<ActivityFacility>, List<DiscretionaryActivityFacility>> byFacility = new LinkedHashMap<>();

		for (DiscretionaryActivityFacility facility : facilities) {
			byType.computeIfAbsent(facility.getType(), type -> new ArrayList<>()).add(facility);
			byFacility.computeIfAbsent(facility.getId(), id -> new ArrayList<>()).add(facility);
		}

		this.byType = unmodifiable(byType);
		this.byFacility = unmodifiable(byFacility);
		this.all = List.copyOf(facilities);
	}

	/**
	 * @return the types that can be performed somewhere, sorted alphabetically, never null
	 */
	public Set<String> getTypes() {
		return this.byType.keySet();
	}

	/**
	 * @return all places at which the given type can be performed, empty if there is none
	 */
	public List<DiscretionaryActivityFacility> getByType(String type) {
		return this.byType.getOrDefault(type, List.of());
	}

	/**
	 * @return the types offered at the given MATSim facility, empty if it offers none. A facility can
	 * offer several, which is why this is a list rather than a single entry.
	 */
	public List<DiscretionaryActivityFacility> get(Id<ActivityFacility> facilityId) {
		return this.byFacility.getOrDefault(facilityId, List.of());
	}

	/**
	 * The sub-types a type comes in, i.e. how much variety there is to choose from. The basis of the
	 * variety part of the capability analysis.
	 *
	 * @return the sub-types of the given type, in the order they were encountered. Facilities without
	 * a sub-type do not contribute.
	 */
	public Set<String> getSubTypes(String type) {
		Set<String> subTypes = new LinkedHashSet<>();
		for (DiscretionaryActivityFacility facility : getByType(type)) {
			facility.getSubType().ifPresent(subTypes::add);
		}
		return Collections.unmodifiableSet(subTypes);
	}

	/**
	 * @return every place, in the order it was given
	 */
	public List<DiscretionaryActivityFacility> getAll() {
		return this.all;
	}

	/**
	 * @return the number of places, counting a facility that offers several types once per type
	 */
	public int size() {
		return this.all.size();
	}

	public boolean isEmpty() {
		return this.all.isEmpty();
	}

	@Override
	public String toString() {
		StringBuilder text = new StringBuilder("[size=" + size() + "]");
		for (Map.Entry<String, List<DiscretionaryActivityFacility>> entry : this.byType.entrySet()) {
			text.append("[").append(entry.getKey()).append("=").append(entry.getValue().size()).append("]");
		}
		return text.toString();
	}

	private static <K> Map<K, List<DiscretionaryActivityFacility>> unmodifiable(Map<K, List<DiscretionaryActivityFacility>> map) {
		map.replaceAll((key, facilities) -> Collections.unmodifiableList(facilities));
		return Collections.unmodifiableMap(map);
	}
}
