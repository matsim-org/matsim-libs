package org.matsim.contrib.dvrp.fleet;

import org.apache.commons.lang3.ArrayUtils;

import java.util.*;
import java.util.stream.Collectors;

public class DefaultDvrpLoadSerializer implements DvrpLoadSerializer {

	private final Map<String, DvrpLoadType> loadTypeByName;
	public DefaultDvrpLoadSerializer(Collection<DvrpLoadType> loadTypes) {
		this(loadTypes.stream().collect(Collectors.toMap(DvrpLoadType::getName, loadType -> loadType)));
	}

	public DefaultDvrpLoadSerializer(DvrpLoadType... loadTypes) {
		this(Arrays.stream(loadTypes).collect(Collectors.toMap(DvrpLoadType::getName, loadType -> loadType)));
	}

	public DefaultDvrpLoadSerializer(Map<String, DvrpLoadType> loadTypeByName) {
		//TODO should we do some quality checks here ? like checking that type and slot names fit in [a-zA-Z] ?
		this.loadTypeByName = new HashMap<>(loadTypeByName);

		if(loadTypeByName.size() == 0) {
			throw new IllegalStateException("At least one DvrpLoadType must be provided");
		}

		Map<Integer, List<DvrpLoadType>> loadTypesPerNumberOfSlots = new HashMap<>();
		Map<String, List<DvrpLoadType>> loadTypesPerSlotName = new HashMap<>();
		for(Map.Entry<String, DvrpLoadType> dvrpLoadTypeEntry: loadTypeByName.entrySet()) {
			String loadTypeName = dvrpLoadTypeEntry.getKey();
			DvrpLoadType dvrpLoadType = dvrpLoadTypeEntry.getValue();
			if(!loadTypeName.equals(dvrpLoadType.getName())) {
				throw new IllegalStateException(String.format("Attempting to register DvrpLoadType with name %s with a different name %s", dvrpLoadType.getName(), loadTypeName));
			}
			String[] slotNames = dvrpLoadType.getSlotNames();
			loadTypesPerNumberOfSlots.computeIfAbsent(slotNames.length, i -> new ArrayList<>()).add(dvrpLoadType);
			for(String slotName: slotNames) {
				loadTypesPerSlotName.computeIfAbsent(slotName, s -> new ArrayList<>()).add(dvrpLoadType);
			}
		}
	}

	@Override
	public DvrpLoad deSerialize(String loadRepr, String loadTypeName) {
		DvrpLoadType loadType = this.loadTypeByName.get(loadTypeName);
		if(loadType == null) {
			throw new IllegalStateException(String.format("Unkown load type %s", loadTypeName));
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
				throw new IllegalStateException(String.format("Load type %s does not contain slot %s", loadType.getName(), slotAndValue[0]));
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
		String loadTypeName = dvrpLoad.getType().getName();
		if(!this.loadTypeByName.containsKey(loadTypeName)) {
			throw new IllegalStateException(String.format("Unknown DvrpLoadType: %s ", loadTypeName));
		}
		if(!this.loadTypeByName.get(loadTypeName).equals(dvrpLoad.getType())) {
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
