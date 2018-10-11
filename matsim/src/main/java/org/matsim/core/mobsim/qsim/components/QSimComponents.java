package org.matsim.core.mobsim.qsim.components;

import java.lang.annotation.Annotation;
import java.util.LinkedList;
import java.util.List;

import com.google.inject.name.Names;

/**
 * Contains information about which QSim components should be used in the
 * simulation and in which order they are registered with the QSim.
 */
final public class QSimComponents {
	private final List<Object> components = new LinkedList<>();

	public void addComponent(Class<? extends Annotation> annotation) {
		if (components.contains(annotation)) {
			throw new IllegalStateException("Annotation type " + annotation + " is already registered.");
		}

		components.add(annotation);
	}

	public void addComponent(Annotation annotation) {
		if (components.contains(annotation)) {
			throw new IllegalStateException("Annotation " + annotation + " is already registered.");
		}

		if (components.contains(annotation.getClass())) {
			throw new IllegalStateException("Cannot register " + annotation + " because annotation type "
					+ annotation.getClass() + " is registered already.");
		}

		components.add(annotation);
	}

	public void addNamedComponent(String name) {
		addComponent(Names.named(name));
	}

	public void removeComponent(Class<? extends Annotation> annotation) {
		components.remove(annotation);
	}

	public void removeComponent(Annotation annotation) {
		components.remove(annotation);
	}

	public void removeNamedComponent(String name) {
		removeComponent(Names.named(name));
	}

	public void clear() {
		components.clear();
	}

	public List<Object> getActiveComponents() {
		return components;
	}
}
