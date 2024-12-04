package org.matsim.contrib.dvrp.fleet.dvrp_load;

import org.apache.commons.collections4.SetUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.IdSet;

import java.util.Map;

/**
 * This implementation of {@link DvrpLoad} defines generic loads with N-slots of {@link ScalarLoad}.
 * Since we rely on maps everytime we perform an operation, this is typically not as efficient as implementing your custom multidimensional loads with an attribute for each dimension
 * @author Tarek Chouaki (tkchouaki)
 */
public class MultipleIndependentSlotsLoad implements DvrpLoad {

	private final MultipleIndependentSlotsLoadType loadType;

	private final IdMap<DvrpLoadType, ScalarLoad> loadPerSlot;

	@SuppressWarnings("unused")
	public MultipleIndependentSlotsLoad(IdMap<DvrpLoadType, ScalarLoad> loadPerSlot, MultipleIndependentSlotsLoadType loadType) {
		this(loadPerSlot, loadType, true);
	}

	MultipleIndependentSlotsLoad(IdMap<DvrpLoadType, ScalarLoad> loadPerSlot, MultipleIndependentSlotsLoadType loadType, boolean checkConsistency) {
		this.loadPerSlot = new IdMap<>(DvrpLoadType.class);
		loadPerSlot.forEach(this.loadPerSlot::put);
		this.loadType = loadType;
		if(checkConsistency) {
			IdSet<DvrpLoadType> slotTypesIds = loadType.getSlotTypesIdSet();
			if(SetUtils.disjunction(slotTypesIds, loadPerSlot.keySet()).size() > 0) {
				throw new IllegalStateException("Provided slots do not match the ones required by the given DvrpLoadType");
			}
		}
	}

	@Override
	public MultipleIndependentSlotsLoadType getType() {
		return this.loadType;
	}

	@Override
	public MultipleIndependentSlotsLoad add(DvrpLoad other) {
		if(other == null) {
			return this;
		}
		if(other instanceof MultipleIndependentSlotsLoad multipleIndependentSlotsLoad) {
			IdMap<DvrpLoadType, ScalarLoad> resultLoadPerSlot = new IdMap<>(DvrpLoadType.class);
			for(Map.Entry<Id<DvrpLoadType>, ScalarLoad> loadEntry: this.loadPerSlot.entrySet()) {
				resultLoadPerSlot.put(loadEntry.getKey(), loadEntry.getValue().add(multipleIndependentSlotsLoad.loadPerSlot.get(loadEntry.getKey())));
			}
			return new MultipleIndependentSlotsLoad(resultLoadPerSlot, this.loadType, false);
		}
		throw new IncompatibleLoadsException(this, other);
	}

	@Override
	public DvrpLoad subtract(DvrpLoad other) {
		if(other == null) {
			return this;
		}
		if(other instanceof MultipleIndependentSlotsLoad multipleIndependentSlotsLoad) {
			IdMap<DvrpLoadType, ScalarLoad> resultLoadPerSlot = new IdMap<>(DvrpLoadType.class);
			for(Map.Entry<Id<DvrpLoadType>, ScalarLoad> loadEntry: this.loadPerSlot.entrySet()) {
				resultLoadPerSlot.put(loadEntry.getKey(), loadEntry.getValue().subtract(multipleIndependentSlotsLoad.loadPerSlot.get(loadEntry.getKey())));
			}
			return new MultipleIndependentSlotsLoad(resultLoadPerSlot, this.loadType, false);
		}
		throw new IncompatibleLoadsException(this, other);
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
		for(Map.Entry<Id<DvrpLoadType>, ScalarLoad> loadEntry: this.loadPerSlot.entrySet()) {
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
		return this.loadPerSlot.get(Id.create(this.loadType.getSlotNames()[i], DvrpLoadType.class)).asArray()[0];
	}

	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public Number[] asArray() {
		Id[] slotIds = this.getType().getOrderedSlotTypesIds();
		Number[] numbers = new Number[slotIds.length];
		for(int i=0; i<numbers.length; i++) {
			numbers[i] = this.loadPerSlot.get(slotIds[i]).asArray()[0];
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
		for(Map.Entry<Id<DvrpLoadType>, ScalarLoad> loadEntry: this.loadPerSlot.entrySet()) {
			if(!loadEntry.getValue().equals(multipleIndependentSlotsLoad.loadPerSlot.get(loadEntry.getKey()))) {
				return false;
			}
		}
		return true;
	}
}
