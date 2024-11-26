package org.matsim.contrib.dvrp.fleet.dvrp_load;

import org.apache.commons.collections4.SetUtils;
import org.matsim.contrib.dvrp.fleet.DvrpLoad;
import org.matsim.contrib.dvrp.fleet.DvrpLoadType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MultipleIndependentSlotsLoadType implements DvrpLoadType {

	private final Map<String, ScalarLoadType> slotTypeByName;
	private final String[] slotNames;
	private final String name;

	public MultipleIndependentSlotsLoadType(List<ScalarLoadType> scalarLoadTypes, String name) {
		this.name = name;
		slotTypeByName = scalarLoadTypes.stream().collect(Collectors.toMap(ScalarLoadType::getName, loadType -> loadType));
		// It is important that slotNames follows the same order as the scalarLoadTypes list
		slotNames = scalarLoadTypes.stream().map(ScalarLoadType::getName).toArray(String[]::new);
	}

	@Override
	public DvrpLoad fromArray(Number[] array) {
		Map<String, ScalarLoad> loadPerSlot = new HashMap<>();
		if(array.length != slotNames.length) {
			throw new IllegalStateException();
		}
		for(int i=0; i<slotNames.length; i++) {
			loadPerSlot.put(slotNames[i], slotTypeByName.get(slotNames[i]).fromNumber(array[i]));
		}
		return new MultipleIndependentSlotsLoad(loadPerSlot, this, false);
	}

	@Override
	public MultipleIndependentSlotsLoad getEmptyLoad() {
		Map<String, ScalarLoad> loadPerSlot = new HashMap<>();
		for(Map.Entry<String, ScalarLoadType> loadTypeEntry: slotTypeByName.entrySet()) {
			loadPerSlot.put(loadTypeEntry.getKey(), loadTypeEntry.getValue().getEmptyLoad());
		}
		return new MultipleIndependentSlotsLoad(loadPerSlot, this, false);
	}

	@Override
	public int numberOfDimensions() {
		return this.slotNames.length;
	}

	@Override
	public String[] getSlotNames() {
		return this.slotNames;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == this) {
			return true;
		}
		if(!obj.getClass().equals(this.getClass())) {
			return false;
		}
		MultipleIndependentSlotsLoadType other = (MultipleIndependentSlotsLoadType) obj;
		if(!this.name.equals(other.name)) {
			return false;
		}
		if(SetUtils.disjunction(this.slotTypeByName.keySet(), other.slotTypeByName.keySet()).size() > 0) {
			return false;
		}
		for(String slot: this.slotNames) {
			if(!this.slotTypeByName.get(slot).equals(other.slotTypeByName.get(slot))) {
				return false;
			}
		}
		return true;
	}
}
