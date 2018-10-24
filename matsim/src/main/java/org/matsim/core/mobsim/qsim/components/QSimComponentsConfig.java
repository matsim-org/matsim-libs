package org.matsim.core.mobsim.qsim.components;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.google.inject.Key;
import com.google.inject.name.Names;

/**
 * Contains information about which QSim components should be used in the
 * simulation and in which order they are registered with the QSim.
 */
final public class QSimComponentsConfig {
	private final List<Object> components = new LinkedList<>();
	private final Set<Key<?>> keys = new HashSet<>();

	public void addComponent(Class<? extends Annotation> annotation) {
		addComponent(Key.get(Object.class, annotation));
		components.add(annotation);
	}

	public void addComponent(Annotation annotation) {
		addComponent(Key.get(Object.class, annotation));
		components.add(annotation);
	}

	private void addComponent(Key<?> componentKey) {
		if (keys.contains(componentKey)) {
			throw new IllegalStateException(keyToString(componentKey) + " is already registered.");
		}
		keys.add(componentKey);
	}

	public void addNamedComponent(String name) {
		addComponent(Names.named(name));
	}

	public void removeComponent(Class<? extends Annotation> annotation) {
		addComponent(Key.get(Object.class, annotation));
		components.remove(annotation);
	}

	public void removeComponent(Annotation annotation) {
		removeComponent(Key.get(Object.class, annotation));
		components.remove(annotation);
	}

	private void removeComponent(Key<?> componentKey) {
		if (!keys.remove(componentKey)) {
			throw new IllegalStateException(keyToString(componentKey) + " is not registered.");
		}
	}

	public void removeNamedComponent(String name) {
		removeComponent(Names.named(name));
	}

	private String keyToString(Key<?> componentKey) {
		return "Annotation" + componentKey.getAnnotation() != null ?
				" " + componentKey.getAnnotation() :
				"Type " + componentKey.getAnnotationType();
	}

	public void clear() {
		components.clear();
	}

	List<Object> getActiveComponents() {
		return Collections.unmodifiableList(components);
	}
}
