package org.matsim.contrib.dvrp.fleet.dvrp_load;

import org.apache.commons.collections4.SetUtils;
import org.matsim.contrib.dvrp.fleet.DvrpLoad;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MultipleIndependentSlotsLoad implements DvrpLoad {

	private final MultipleIndependentSlotsLoadType loadType;

	private final Map<String, ScalarLoad> loadPerSlot;

	public MultipleIndependentSlotsLoad(Map<String, ScalarLoad> loadPerSlot, MultipleIndependentSlotsLoadType loadType) {
		this(loadPerSlot, loadType, true);
	}

	MultipleIndependentSlotsLoad(Map<String, ScalarLoad> loadPerSlot, MultipleIndependentSlotsLoadType loadType, boolean checkConsistency) {
		this.loadPerSlot = new HashMap<>(loadPerSlot);
		this.loadType = loadType;
		if(checkConsistency) {
			Set<String> typeSlots = Arrays.stream(loadType.getSlotNames()).collect(Collectors.toSet());
			if(SetUtils.disjunction(typeSlots, loadPerSlot.keySet()).size() > 0) {
				throw new IllegalStateException("Provided slots do not match the ones required by the given DvrpLoadType");
			}
		}
	}

	@Override
	public MultipleIndependentSlotsLoadType getType() {
		return this.loadType;
	}

	@Override
	public MultipleIndependentSlotsLoad addTo(DvrpLoad other) {
		if(other == null) {
			return this;
		}
		if(other instanceof MultipleIndependentSlotsLoad multipleIndependentSlotsLoad) {
			Map<String, ScalarLoad> resultLoadPerSlot = new HashMap<>();
			for(Map.Entry<String, ScalarLoad> loadEntry: this.loadPerSlot.entrySet()) {
				resultLoadPerSlot.put(loadEntry.getKey(), loadEntry.getValue().addTo(multipleIndependentSlotsLoad.loadPerSlot.get(loadEntry.getKey())));
			}
			return new MultipleIndependentSlotsLoad(resultLoadPerSlot, this.loadType, false);
		}
		throw new UnsupportedVehicleLoadException(this, other.getClass());
	}

	@Override
	public DvrpLoad subtract(DvrpLoad other) {
		if(other == null) {
			return this;
		}
		if(other instanceof MultipleIndependentSlotsLoad multipleIndependentSlotsLoad) {
			Map<String, ScalarLoad> resultLoadPerSlot = new HashMap<>();
			for(Map.Entry<String, ScalarLoad> loadEntry: this.loadPerSlot.entrySet()) {
				resultLoadPerSlot.put(loadEntry.getKey(), loadEntry.getValue().subtract(multipleIndependentSlotsLoad.loadPerSlot.get(loadEntry.getKey())));
			}
			return new MultipleIndependentSlotsLoad(resultLoadPerSlot, this.loadType, false);
		}
		throw new UnsupportedVehicleLoadException(this, other.getClass());
	}

	@Override
	public boolean fitsIn(DvrpLoad other) {
		if(!this.getClass().equals(other.getClass())) {
			return false;
		}
		if(this.loadType.equals(other.getType())) {
			return false;
		}
		MultipleIndependentSlotsLoad multipleIndependentSlotsLoad = (MultipleIndependentSlotsLoad) other;
		for(Map.Entry<String, ScalarLoad> loadEntry: this.loadPerSlot.entrySet()) {
			if(!loadEntry.getValue().fitsIn(multipleIndependentSlotsLoad.loadPerSlot.get(loadEntry.getKey()))) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean isEmpty() {
		for(ScalarLoad scalarLoad: this.loadPerSlot.values()) {
			if(!scalarLoad.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public Number getElement(int i) {
		return this.loadPerSlot.get(this.loadType.getSlotNames()[i]).asArray()[0];
	}

	@Override
	public Number[] asArray() {
		String[] slotNames = this.loadType.getSlotNames();
		Number[] numbers = new Number[slotNames.length];
		for(int i=0; i<numbers.length; i++) {
			numbers[i] = this.loadPerSlot.get(slotNames[i]).asArray()[0];
		}
		return numbers;
	}

	@Override
	public boolean equals(Object obj) {
		if(!obj.getClass().equals(this.getClass())) {
			return false;
		}
		MultipleIndependentSlotsLoad multipleIndependentSlotsLoad = (MultipleIndependentSlotsLoad) obj;
		if(!this.loadType.equals(multipleIndependentSlotsLoad.getType())) {
			return false;
		}
		for(Map.Entry<String, ScalarLoad> loadEntry: this.loadPerSlot.entrySet()) {
			if(!loadEntry.getValue().equals(multipleIndependentSlotsLoad.loadPerSlot.get(loadEntry.getKey()))) {
				return false;
			}
		}
		return true;
	}
}
