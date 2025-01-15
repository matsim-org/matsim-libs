package org.matsim.contrib.dvrp.fleet.dvrp_load;

import org.apache.commons.lang3.ArrayUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;


import java.util.Collection;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This default implementation of the {@link DvrpLoadSerializer} follows a slotName=value representation (except if the load type only has one slot, in this case we only print a number).
 * @author Tarek Chouaki (tkchouaki)
 */
public class DefaultDvrpLoadSerializer implements DvrpLoadSerializer {

	private final IdMap<DvrpLoadType, DvrpLoadType> loadTypeById;

	private DefaultDvrpLoadSerializer(Stream<DvrpLoadType> loadTypeStream) {
		this(loadTypeStream.collect(Collectors.toMap(DvrpLoadType::getId, dvrpLoadType -> dvrpLoadType, (a, b) -> {
			throw new IllegalStateException();
		}, () -> new IdMap<>(DvrpLoadType.class))));
	}

	@SuppressWarnings("unused")
	public DefaultDvrpLoadSerializer(Collection<DvrpLoadType> loadTypes) {
		this(loadTypes.stream());
	}

	public DefaultDvrpLoadSerializer(DvrpLoadType... loadTypes) {
		this(Arrays.stream(loadTypes));
	}

	private void checkNameFormat(String name) {
		for(int i=0; i<name.length(); i++) {
			char c = name.charAt(i);
			if(!Character.isAlphabetic(c) && !Character.isDigit(c)) {
				throw new IllegalStateException("Load type ids and slot names must be alphanumeric");
			}
		}
	}

	public DefaultDvrpLoadSerializer(IdMap<DvrpLoadType, DvrpLoadType> loadTypeById) {
		this.loadTypeById = new IdMap<>(DvrpLoadType.class);
		loadTypeById.forEach(this.loadTypeById::put);

		if(loadTypeById.size() == 0) {
			throw new IllegalStateException("At least one DvrpLoadType must be provided");
		}

		Map<Integer, List<DvrpLoadType>> loadTypesPerNumberOfSlots = new HashMap<>();
		Map<String, List<DvrpLoadType>> loadTypesPerSlotName = new HashMap<>();
		for(Map.Entry<Id<DvrpLoadType>, DvrpLoadType> dvrpLoadTypeEntry: loadTypeById.entrySet()) {
			Id<DvrpLoadType> loadTypeId = dvrpLoadTypeEntry.getKey();
			checkNameFormat(loadTypeId.toString());
			DvrpLoadType dvrpLoadType = dvrpLoadTypeEntry.getValue();
			if(!loadTypeId.equals(dvrpLoadType.getId())) {
				throw new IllegalStateException(String.format("Attempting to register DvrpLoadType with name %s with a different name %s", dvrpLoadType.getId(), loadTypeId));
			}
			String[] slotNames = dvrpLoadType.getSlotNames();
			loadTypesPerNumberOfSlots.computeIfAbsent(slotNames.length, i -> new ArrayList<>()).add(dvrpLoadType);
			for(String slotName: slotNames) {
				checkNameFormat(slotName);
				loadTypesPerSlotName.computeIfAbsent(slotName, s -> new ArrayList<>()).add(dvrpLoadType);
			}
		}
	}

	@Override
	public DvrpLoad deSerialize(String loadRepr, Id<DvrpLoadType> dvrpLoadTypeId) {
		DvrpLoadType loadType = this.loadTypeById.get(dvrpLoadTypeId);
		if(loadType == null) {
			throw new IllegalStateException(String.format("Unkown load type %s", dvrpLoadTypeId));
		}
		return deSerialize(loadRepr, loadType);
	}

	private DvrpLoad deSerialize(String loadRepr, DvrpLoadType loadType) {
		if(loadRepr.isEmpty()) {
			return loadType.getEmptyLoad();
		}
		if(!loadRepr.contains("=")) {
			// We expect only one number
			return loadType.fromArray(new Number[]{stringToNumber(loadRepr)});
		}
		String[] slotNames = loadType.getSlotNames();
		String[] stringComponents = loadRepr.split(",");
		Map<String, Number> valuePerSlot = new HashMap<>();
		for (String stringComponent : stringComponents) {
			String[] slotAndValue = stringComponent.split("=");
			if (slotAndValue.length != 2) {
				throw new IllegalStateException();
			}
			if (!ArrayUtils.contains(slotNames, slotAndValue[0])) {
				throw new IllegalStateException(String.format("Load type %s does not contain slot %s", loadType.getId(), slotAndValue[0]));
			}
			valuePerSlot.put(slotAndValue[0], stringToNumber(slotAndValue[1]));
		}
		Number[] numberComponents = new Number[slotNames.length];
		for(int i=0; i<numberComponents.length; i++) {
			numberComponents[i] = valuePerSlot.getOrDefault(slotNames[i], 0);
		}
		return loadType.fromArray(numberComponents);
	}

	private Number stringToNumber(String s) {
		if(s.contains(".")) {
			return Double.parseDouble(s);
		}
		return Integer.parseInt(s);
	}

	@Override
	public String serialize(DvrpLoad dvrpLoad) {
		Id<DvrpLoadType> loadTypeName = dvrpLoad.getType().getId();
		if(!this.loadTypeById.containsKey(loadTypeName)) {
			throw new IllegalStateException(String.format("Unknown DvrpLoadType: %s ", loadTypeName));
		}
		if(!this.loadTypeById.get(loadTypeName).equals(dvrpLoad.getType())) {
			throw new IllegalStateException(String.format("Different DvrpLoadType registered with the same name %s", loadTypeName));
		}
		List<String> components = new ArrayList<>();
		Number[] loadComponents = dvrpLoad.asArray();
		String[] slotNames = dvrpLoad.getType().getSlotNames();
		if(loadComponents.length == 1) {
			return loadComponents[0].toString();
		}
		for(int i=0; i<loadComponents.length; i++) {
			if(loadComponents[i].doubleValue() == 0.0) {
				continue;
			}
			String componentString = slotNames[i]+"="+loadComponents[i].toString();
			components.add(componentString);
		}
		return String.join(",", components);
	}
}
