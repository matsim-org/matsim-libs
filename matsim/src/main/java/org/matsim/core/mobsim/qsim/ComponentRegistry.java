package org.matsim.core.mobsim.qsim;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.google.inject.Key;

public class ComponentRegistry<T> {
	private final static Logger log = Logger.getLogger(ComponentRegistry.class);

	private final Map<String, Key<? extends T>> components = new HashMap<>();
	private final String componentTypeDescription;

	public ComponentRegistry(String componentTypeDescription) {
		this.componentTypeDescription = componentTypeDescription;
	}

	public void register(String name, Class<? extends T> component) {
		register(name, Key.get(component));
	}

	public void register(String name, Key<? extends T> component) {
		if (components.containsKey(name)) {
			throw new IllegalArgumentException(
					String.format("A %s with name '%s' is already registered", componentTypeDescription, name));
		}

		this.components.put(name, component);

		log.info(String.format("Registered %s with name '%s' to %s", componentTypeDescription, name, component));
	}

	public Key<? extends T> getComponent(String name) {
		if (components.containsKey(name)) {
			return components.get(name);
		} else {
			throw new IllegalArgumentException(
					String.format("A %s with name '%s' does not exist", componentTypeDescription, name));
		}
	}

	public List<Key<? extends T>> getOrderedComponents(List<String> ordering) {
		return ordering.stream().map(this::getComponent).collect(Collectors.toList());
	}
}
