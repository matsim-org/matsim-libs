package org.matsim.contrib.dvrp.fleet.dvrp_load;

import org.apache.commons.collections4.SetUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.IdSet;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Defines types of {@link MultipleIndependentSlotsLoad}
 * @author Tarek Chouaki (tkchouaki)
 */
public class MultipleIndependentSlotsLoadType implements DvrpLoadType {

	private final IdMap<DvrpLoadType, ScalarLoadType> slotTypeById;
	private final IdSet<DvrpLoadType> slotTypesIdSet;
	@SuppressWarnings("rawtypes")
	private final Id[] orderedSlotTypesIds;
	private final Id<DvrpLoadType> id;

	public MultipleIndependentSlotsLoadType(List<ScalarLoadType> scalarLoadTypes, Id<DvrpLoadType> id) {
		this.id = id;
		slotTypeById = scalarLoadTypes.stream().collect(Collectors.toMap(ScalarLoadType::getId, loadType -> loadType, (a, b) -> {
			throw new IllegalStateException();
		}, () -> new IdMap<>(DvrpLoadType.class)));
		// It is important that slotNames follows the same order as the scalarLoadTypes list
		orderedSlotTypesIds = scalarLoadTypes.stream().map(ScalarLoadType::getId).toArray(Id[]::new);
		slotTypesIdSet = new IdSet<>(DvrpLoadType.class);
		slotTypesIdSet.addAll(slotTypeById.keySet());
	}

	@Override
	@SuppressWarnings("unchecked")
	public DvrpLoad fromArray(Number[] array) {
		IdMap<DvrpLoadType, ScalarLoad> loadPerSlot = new IdMap<>(DvrpLoadType.class);
		if(array.length != orderedSlotTypesIds.length) {
			throw new IllegalStateException();
		}
		for(int i = 0; i< orderedSlotTypesIds.length; i++) {
			loadPerSlot.put(orderedSlotTypesIds[i], slotTypeById.get(orderedSlotTypesIds[i]).fromNumber(array[i]));
		}
		return new MultipleIndependentSlotsLoad(loadPerSlot, this, false);
	}

	@Override
	public MultipleIndependentSlotsLoad getEmptyLoad() {
		IdMap<DvrpLoadType, ScalarLoad> loadPerSlot = new IdMap<>(DvrpLoadType.class);
		for(Map.Entry<Id<DvrpLoadType>, ScalarLoadType> loadTypeEntry: slotTypeById.entrySet()) {
			loadPerSlot.put(loadTypeEntry.getKey(), loadTypeEntry.getValue().getEmptyLoad());
		}
		return new MultipleIndependentSlotsLoad(loadPerSlot, this, false);
	}

	public IdSet<DvrpLoadType> getSlotTypesIdSet() {
		return this.slotTypesIdSet;
	}

	@Override
	public String[] getSlotNames() {
		return Arrays.stream(this.orderedSlotTypesIds).map(Object::toString).toArray(String[]::new);
	}

	@SuppressWarnings("rawtypes")
	public Id[] getOrderedSlotTypesIds() {
		return this.orderedSlotTypesIds;
	}

	@Override
	public Id<DvrpLoadType> getId() {
		return this.id;
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
		if(!this.id.equals(other.id)) {
			return false;
		}
		if(SetUtils.disjunction(this.slotTypeById.keySet(), other.slotTypeById.keySet()).size() > 0) {
			return false;
		}
		for(Id<DvrpLoadType> slot: this.slotTypeById.keySet()) {
			if(!this.slotTypeById.get(slot).equals(other.slotTypeById.get(slot))) {
				return false;
			}
		}
		return true;
	}
}
